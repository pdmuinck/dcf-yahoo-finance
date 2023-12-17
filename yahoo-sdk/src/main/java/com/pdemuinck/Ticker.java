package com.pdemuinck;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Ticker {
  public boolean skip = false;
  String ticker;
  double defaultGrowthRate;
  double freeCashFlow;
  double netDebt;
  double growthNext5Years;



  double numberOfShares;

  private Ticker() {

  }

  public Ticker(String ticker) {
    this(ticker, 0.10);
  }

  public Ticker(String ticker, double defaultGrowthRate) {
    System.out.println("started ticker: " + ticker);
    this.ticker = ticker;
    this.defaultGrowthRate = defaultGrowthRate;
    this.growthNext5Years = growthRate();
    HashMap<String, Double> financialData = financialData("query2", ticker);
    if (financialData.isEmpty()) {
      financialData = financialData("query1", ticker);
    }
    if(financialData.isEmpty()){
      this.skip = true;
      return;
    }
    this.netDebt = financialData.getOrDefault("quarterlyCashAndCashEquivalents", 0.0) -
        (financialData.getOrDefault("quarterlyTotalDebt", 0.0));
    this.numberOfShares = financialData.getOrDefault("quarterlyOrdinarySharesNumber", 0.0);
    this.freeCashFlow = financialData.getOrDefault("annualFreeCashFlow", 0.0)
        - financialData.getOrDefault("annualStockBasedCompensation", 0.0);
  }

  private double growthRate() {
    HttpClient client = HttpClient.newBuilder().build();

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(
              String.format("https://finance.yahoo.com/quote/%s/analysis?p=%s", this.ticker,
                  this.ticker)))
          .header("cookie",
              "A1=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; GUC=AQABCAFle1plo0IgmgSP&s=AQAAAEE2hU4j&g=ZXoTlw; A3=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; maex=%7B%22v2%22%3A%7B%7D%7D; A1S=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; cmp=t=1702655865&j=1&u=1---&v=6; EuConsent=CP2uYgAP2uYgAAOACKNLAeEgAAAAAAAAACiQAAAAAAAA; PRF=t%3DNVDA%252BINTC%26newChartbetateaser%3D1")
          .header("user-agent",
              "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
          .version(HttpClient.Version.HTTP_2)
          .GET()
          .build();

      String analysts = null;
      analysts = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
      Document parse = Jsoup.parse(analysts);

      Elements elementsByClass1 = parse.getElementsByClass("BdT Bdc($seperatorColor)");
      return elementsByClass1.stream()
          .filter(element -> element.childNodes().get(0).toString().contains("Next 5 Years"))
          .filter(
              element -> !element.childNodes().get(1).childNodes().get(0).toString().equals("N/A"))
          .map(element -> Double.parseDouble(
              element.childNodes().get(1).childNodes().get(0).toString().replace("%", "")))
          .map(x -> x / 100)
          .findFirst().orElseGet(() -> defaultGrowthRate);

    } catch (Exception e) {
      return defaultGrowthRate;
    }
  }

  private HashMap<String, Double> financialData(String query, String tickerName) {
    try {
      HttpClient client = HttpClient.newBuilder().build();
      String type = String.join(",", "annualFreeCashFlow", "annualStockBasedCompensation",
          "quarterlyCashAndCashEquivalents", "quarterlyTotalDebt", "quarterlyOrdinarySharesNumber",
          "52WeekChange");

      HttpRequest financialsRequest = HttpRequest.newBuilder()
          .uri(new URI(String.format(
              "https://%s.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/%s?type=%s&period1=493590046&period2=1702655894&corsDomain=finance.yahoo.com",
              query, tickerName, type)))
          .version(HttpClient.Version.HTTP_2)
          .GET()
          .build();

      HttpResponse<String> response =
          client.send(financialsRequest, HttpResponse.BodyHandlers.ofString());
      JSONObject jsonObject = new JSONObject(response.body());
      JSONArray results = jsonObject.getJSONObject("timeseries").getJSONArray("result");
      HashMap<String, Double> financialData = new HashMap<>();
      for (int i = 0; i < results.length(); i++) {
        for (String metric : List.of("annualFreeCashFlow", "annualStockBasedCompensation",
            "quarterlyCashAndCashEquivalents", "quarterlyTotalDebt",
            "quarterlyOrdinarySharesNumber")) {
          try {
            JSONArray metricObject = results.getJSONObject(i).getJSONArray(metric);
            if (metricObject != null) {
              financialData.put(metric,
                  metricObject.getJSONObject(metricObject.length() - 1)
                      .getJSONObject("reportedValue")
                      .getDouble("raw"));
            }
          } catch (JSONException e) {
          }
        }
        ;
      }
      return financialData;
    } catch (Exception e) {
      this.skip = true;
      return new HashMap<>();
    }
  }

  public double getFreeCashFlow() {
    return freeCashFlow;
  }

  public double getNetDebt() {
    return netDebt;
  }

  public double getGrowthNext5Years() {
    return growthNext5Years;
  }

  public double getNumberOfShares() {
    return numberOfShares;
  }
}
