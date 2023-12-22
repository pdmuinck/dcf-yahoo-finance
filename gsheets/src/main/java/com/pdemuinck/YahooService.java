package com.pdemuinck;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class YahooService {

  private static final Logger LOGGER = Logger.getLogger(YahooService.class.getName());

  public static Ticker fetchTicker(String ticker){
    RetryConfig config = RetryConfig.<HttpResponse<String>>custom()
        .maxAttempts(10)
        .waitDuration(Duration.ofMillis(60*1000))
        .retryOnResult(response -> response.statusCode() == 429)
        .failAfterMaxAttempts(true)
        .build();

    RetryRegistry registry = RetryRegistry.of(config);
    registry.getEventPublisher()
        .onEntryAdded(entryAddedEvent -> {
          Retry addedRetry = entryAddedEvent.getAddedEntry();
          LOGGER.info(String.format("Retry for %s added", addedRetry.getName()));
        })
        .onEntryReplaced(event -> {
          LOGGER.info(String.format("Retry for %s replaced", event.getOldEntry().getName()));
        })
    ;

    Retry financeDataRetry = registry.retry(String.format("Finance data for %s", ticker));
    Retry growthRateDataRetry = registry.retry(String.format("Growth rate data for %s", ticker));

    Map<String, Double> financialData = parseFinancialData(financeDataRetry.executeSupplier(() -> fetchFinancialData(ticker)).body().toString());
    double growthRate5NextYears =
        parseGrowthRate(growthRateDataRetry.executeSupplier(() -> growthRate(ticker)).body());
    double netDebt = financialData.getOrDefault("quarterlyNetDebt", 0.0);
    double numberOfShares = financialData.getOrDefault("quarterlyOrdinarySharesNumber", 0.0);
    double freeCashFlow = financialData.getOrDefault("annualFreeCashFlow", 0.0);
    double stockBasedCompensation = financialData.getOrDefault("annualStockBasedCompensation", 0.0);
    return new Ticker(ticker, freeCashFlow, netDebt, growthRate5NextYears, numberOfShares, stockBasedCompensation);
  }

  private static HttpResponse fetchFinancialData(String ticker) {
    try {
      HttpClient client = HttpClient.newBuilder().build();
      String metrics = String.join(",", "annualFreeCashFlow", "annualStockBasedCompensation",
          "quarterlyNetDebt", "quarterlyOrdinarySharesNumber",
          "52WeekChange");

      HttpRequest financialsRequest = HttpRequest.newBuilder()
          .uri(new URI(String.format(
              "https://query2.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/%s?type=%s&period1=493590046&period2=1702655894&corsDomain=finance.yahoo.com",
              ticker, metrics)))
          .version(HttpClient.Version.HTTP_2)
          .GET()
          .build();

      HttpResponse<String> response =
          client.send(financialsRequest, HttpResponse.BodyHandlers.ofString());
      if(response.statusCode() != 200){
        LOGGER.info(String.format("Financial data not yet found for ticker %s, status %d", ticker, response.statusCode()));
      }
      return response;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Double> parseFinancialData(String jsonString){
    JSONObject jsonObject = new JSONObject(jsonString);
    JSONArray results = jsonObject.getJSONObject("timeseries").getJSONArray("result");
    HashMap<String, Double> financialData = new HashMap<>();
    for (int i = 0; i < results.length(); i++) {
      for (String metric : List.of("annualFreeCashFlow", "annualStockBasedCompensation",
          "quarterlyNetDebt",
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
    }
    return financialData;
  }

  private static HttpResponse<String> growthRate(String ticker) {
    try {
      HttpClient client = HttpClient.newBuilder().build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(
              String.format("https://finance.yahoo.com/quote/%s/analysis?p=%s", ticker,
                  ticker)))
          .header("cookie",
              "A1=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; GUC=AQABCAFle1plo0IgmgSP&s=AQAAAEE2hU4j&g=ZXoTlw; A3=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; maex=%7B%22v2%22%3A%7B%7D%7D; A1S=d=AQABBITDcWUCEBcMde-OHUrHrOvEYvJNlboFEgABCAFae2WjZRT1bmUBAiAAAAcIhMNxZfJNlbo&S=AQAAAkDNhyUGCOtcU8g4iH5FBWA; cmp=t=1702655865&j=1&u=1---&v=6; EuConsent=CP2uYgAP2uYgAAOACKNLAeEgAAAAAAAAACiQAAAAAAAA; PRF=t%3DNVDA%252BINTC%26newChartbetateaser%3D1")
          .header("user-agent",
              "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
          .version(HttpClient.Version.HTTP_2)
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if(response.statusCode() != 200){
        LOGGER.info(String.format("Financial data not yet found for ticker %s, status %d", ticker, response.statusCode()));
      }
      return response;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static double parseGrowthRate(String html){
    Document parse = Jsoup.parse(html);
    Elements elementsByClass1 = parse.getElementsByClass("BdT Bdc($seperatorColor)");
    return elementsByClass1.stream()
        .filter(element -> element.childNodes().get(0).toString().contains("Next 5 Years"))
        .filter(
            element -> !element.childNodes().get(1).childNodes().get(0).toString().equals("N/A"))
        .map(element -> Double.parseDouble(
            element.childNodes().get(1).childNodes().get(0).toString().replace("%", "")))
        .map(x -> x / 100)
        .findFirst().orElseGet(() -> 0.10);
  }
}
