package com.pdemuinck.dcf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Ticker {


  public static double calculateDcf(String tickerName)
      throws URISyntaxException, IOException, InterruptedException {
    return fetchCurrentStockPrice(tickerName);
  }

  private static double fetchCurrentStockPrice(String tickerName)
      throws URISyntaxException, IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(
            String.format("https://finance.yahoo.com/quote/%s/analysis?p=%s", tickerName, tickerName)))
        .header("cookie", "A1=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; GUC=AQABCAFle1plo0IgmgSP&s=AQAAAEE2hU4j&g=ZXoTlw; A3=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; maex=%7B%22v2%22%3A%7B%7D%7D; A1S=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; cmp=t=1702655865&j=1&u=1---&v=6; EuConsent=CP2uYgAP2uYgAAOACKNLAeEgAAAAAAAAACiQAAAAAAAA; PRF=t%3DNVDA%252BINTC%26newChartbetateaser%3D1")
        .header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36" )
        .version(HttpClient.Version.HTTP_2)
        .GET()
        .build();

    String analysts = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    Document parse = Jsoup.parse(analysts);

    Elements elementsByClass1 = parse.getElementsByClass("BdT Bdc($seperatorColor)");
    double growthRate = elementsByClass1.stream()
        .filter(element -> element.childNodes().get(0).toString().contains("Next 5 Years"))
        .filter(element -> !element.childNodes().get(1).childNodes().get(0).toString().equals("N/A"))
        .map(element -> Double.parseDouble(element.childNodes().get(1).childNodes().get(0).toString().replace("%", "")))
        .findFirst().orElseGet(() -> 10.0);

    double growthRate6To10 = growthRate * 0.8;

    Map<String, Double> financialData = financialData("query2", tickerName);
    if(financialData.isEmpty()){
      financialData = financialData("query1", tickerName);
    }

    Double annualCashFlow = financialData.get("annualFreeCashFlow")
        - financialData.getOrDefault("annualStockBasedCompensation", 0.0);
    double[] futureCashFlows = new double[10];
    double[] discountedCashFlows = new double[10];
    futureCashFlows[0] = annualCashFlow * (1 + (growthRate / 100));
    discountedCashFlows[0] = futureCashFlows[0] / 1.1;
    for(int i = 1; i < futureCashFlows.length; i++){
      double growth = (1 + growthRate / 100);
      if(i > 4){
        growth = (1 + growthRate6To10 / 100);
      }
      futureCashFlows[i] = futureCashFlows[i-1] * growth;
      discountedCashFlows[i] = futureCashFlows[i] / Math.pow(1.1, i+1);
    }
    Double terminalValue = 15.0;
    Double netDebt = financialData.get("quarterlyCashAndCashEquivalents") - (financialData.get("quarterlyTotalDebt"));
    Double presentValueOfAllCashFlows = Arrays.stream(discountedCashFlows).sum() + (discountedCashFlows[9] * terminalValue);
    Double intrinsicValue = presentValueOfAllCashFlows + netDebt;
    Double intrinsicValuePerShare = intrinsicValue / (financialData.get("quarterlyOrdinarySharesNumber"));
    return intrinsicValuePerShare;
  }

  private static HashMap<String, Double> financialData(String query, String tickerName)
      throws IOException, InterruptedException, URISyntaxException {
    HttpClient client = HttpClient.newBuilder().build();
    String type = String.join(",", "annualFreeCashFlow", "annualStockBasedCompensation",
        "quarterlyCashAndCashEquivalents", "quarterlyTotalDebt", "quarterlyOrdinarySharesNumber", "52WeekChange");

    HttpRequest financialsRequest = HttpRequest.newBuilder()
        .uri(new URI(String.format(
            "https://%s.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/%s?type=%s&period1=493590046&period2=1702655894&corsDomain=finance.yahoo.com",
            query, tickerName, type)))
        .version(HttpClient.Version.HTTP_2)
        .GET()
        .build();

    HttpResponse<String> response = client.send(financialsRequest, HttpResponse.BodyHandlers.ofString());
    JSONObject jsonObject = new JSONObject(response.body());
    JSONArray results = jsonObject.getJSONObject("timeseries").getJSONArray("result");
    HashMap<String, Double> financialData = new HashMap<>();
    for(int i = 0 ; i < results.length() ; i++){
      for(String metric : List.of("annualFreeCashFlow", "annualStockBasedCompensation",
          "quarterlyCashAndCashEquivalents", "quarterlyTotalDebt", "quarterlyOrdinarySharesNumber")){
        try{
          JSONArray metricObject = results.getJSONObject(i).getJSONArray(metric);
          if(metricObject != null){
            financialData.put(metric, metricObject.getJSONObject(metricObject.length() - 1).getJSONObject("reportedValue").getDouble("raw"));
          }
        } catch(JSONException e){
        }
      };
    }
    return financialData;
  }
}
