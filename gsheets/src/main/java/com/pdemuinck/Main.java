package com.pdemuinck;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.dhatim.fastexcel.BorderSide;
import org.dhatim.fastexcel.Color;
import org.dhatim.fastexcel.ConditionalFormattingExpressionRule;
import org.dhatim.fastexcel.HyperLink;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;


public class Main {

  public static void testSheets(String[] args) {
    if (args.length == 0) {
      String query = "";
      try (InputStreamReader isr = new InputStreamReader(System.in)) {
        int ch;
        while ((ch = isr.read()) != -1) {
          query += (char) ch;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      String[] split = query.split("\n");
      if (split.length > 0) {
        args = split;
      }
    }

    DecimalFormat df = new DecimalFormat("#");
    df.setMaximumFractionDigits(0);

    String tickers =
        Arrays.stream(args)
            .map(YahooService::fetchTicker)
            .map(ticker -> String.join(",", ticker.getTicker(),
                String.valueOf(df.format(ticker.getNumberOfShares())),
                String.valueOf(ticker.getGrowthNext5Years()),
                String.valueOf(df.format(ticker.getFreeCashFlow())),
                String.valueOf(df.format(ticker.getNetDebt())),
                String.valueOf(df.format(ticker.getStockBasedCompensation()))))
            .collect(Collectors.joining("\n"));

    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter("test.csv", true))) {
      writer.append(tickers);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void createWorkBook(){
    try (BufferedReader reader = new BufferedReader(new FileReader("test.csv"))){
      List<Ticker> list = reader.lines().map(line -> {
        String[] split = line.split(",");
        return new Ticker(split[0], Double.parseDouble(split[3]), Double.parseDouble(split[4]),
            Double.parseDouble(split[2]), Double.parseDouble(split[1]), Double.parseDouble(split[5]));
      }).toList();
      toWorkBookWithSharedSheets(list, "bla.xlsx");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      String query = "";
      try (InputStreamReader isr = new InputStreamReader(System.in)) {
        int ch;
        while ((ch = isr.read()) != -1) {
          query += (char) ch;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      String[] split = query.split("\n");
      if (split.length > 0) {
        args = split;
      }
    }

    List<Ticker> tickers =
        Arrays.stream(args).map(YahooService::fetchTicker).collect(Collectors.toList());
    if(tickers.size() < 200){
      toWorkBook(tickers, "S&P500.xlsx");
    } else {
      toWorkBookWithSharedSheets(tickers, "S&P500.xlsx");
    }
  }

  private static void toWorkBookWithSharedSheets(List<Ticker> tickers, String s) {
    try (OutputStream os = new FileOutputStream(s);
         Workbook wb = new Workbook(os, "S&P500", "1.0")) {
      Worksheet summary = wb.newWorksheet("Summary");
      List<String> labels =
          List.of("WorkSheet", "Ticker", "Discount Rate", "Terminal Value", "DCF Valuation",
              "Current Price",
              "Current Margin of Safety");

      for (int c = 0; c < labels.size(); c++) {
        summary.value(0, c, labels.get(c));
      }

      int tickersPerSheet = 3;
      for (int i = 0; i < tickers.size() - tickersPerSheet; i += tickersPerSheet) {
        List<Ticker> tickersForSheet = tickers.subList(i, i + tickersPerSheet);
        String sheetName =
            tickersForSheet.stream().map(Ticker::getTicker).collect(Collectors.joining("__"));
        Worksheet tickerSheet = wb.newWorksheet(sheetName);

        for(int j = 0; j < tickersForSheet.size(); j++){
          int startRow = j * 25;
          Ticker ticker = tickersForSheet.get(j);
          printTickerToSheet(startRow, 0, tickerSheet, ticker);

          summary.hyperlink(i + j + 1, 0,
              HyperLink.internal(String.format("%s!A%d", sheetName, startRow + 1), ticker.getTicker()));
          summary.value(i + j + 1, 1, ticker.getTicker());
          summary.value(i + j + 1, 2, 0.10);
          summary.style(i + j + 1, 2).format("0.00%").set();
          summary.value(i + j + 1, 3, 15);
          summary.formula(i + j + 1, 4, String.format("%s!F%s", sheetName, startRow + 16));
          summary.formula(i + j + 1, 5, String.format("%s!B%s", sheetName, startRow + 3));
          summary.formula(i + j + 1, 6, String.format("%s!F%s", sheetName, startRow + 17));
          summary.width(0, 20);
          summary.width(1, 20);
          summary.width(2, 20);
          summary.width(3, 20);
          summary.width(4, 20);
          summary.width(5, 20);
          summary.width(6, 20);
        }

      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

    private static void toWorkBook(List<Ticker> tickers, String output){

    try (OutputStream os = new FileOutputStream(output);
         Workbook wb = new Workbook(os, "S&P500", "1.0")) {
      Worksheet summary = wb.newWorksheet("Summary");
      List<String> labels =
          List.of("WorkSheet", "Ticker", "Discount Rate", "Terminal Value", "DCF Valuation",
              "Current Price",
              "Current Margin of Safety");

      for (int c = 0; c < labels.size(); c++) {
        summary.value(0, c, labels.get(c));
      }

      for (int i = 0; i < tickers.size(); i++) {
        Ticker ticker = tickers.get(i);
        double discountRate = 0.10;
        double terminalValue = 15;
        Worksheet tickerSheet = wb.newWorksheet(ticker.getTicker());
        printTickerToSheet(0, 0, tickerSheet, ticker);

        summary.hyperlink(i + 1, 0,
            HyperLink.internal(String.format("%s!A1", ticker.getTicker()), ticker.getTicker()));
        summary.value(i + 1, 1, ticker.getTicker());
        summary.value(i + 1, 2, discountRate);
        summary.style(i + 1, 2).format("0.00%").set();
        summary.value(i + 1, 3, terminalValue);
        summary.formula(i + 1, 4, String.format("%s!F16", ticker.getTicker()));
        summary.formula(i + 1, 5, String.format("%s!B3", ticker.getTicker()));
        summary.formula(i + 1, 6, String.format("%s!F17", ticker.getTicker()));
        summary.width(0, 20);
        summary.width(1, 20);
        summary.width(2, 20);
        summary.width(3, 20);
        summary.width(4, 20);
        summary.width(5, 20);
        summary.width(6, 20);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void printTickerToSheet(int startR, int startC, Worksheet tickerSheet,
                                         Ticker ticker){
    double discountRate = 0.10;
    double terminalValue = 15;
    tickerSheet.width(0, 30);
    tickerSheet.width(1, 20);
    tickerSheet.width(3, 30);
    tickerSheet.width(4, 30);
    tickerSheet.width(5, 30);
    tickerSheet.style(startR + 0, startC + 0).bold().set();
    tickerSheet.value(startR + 0, startC + 0, ticker.getTicker());
    tickerSheet.value(startR + 2, startC + 0, "current");
    tickerSheet.value(startR + 3, startC + 0, "H52");
    tickerSheet.value(startR + 4, startC + 0, "L52");
    tickerSheet.value(startR + 5, startC + 0, "growth rate (1-5 yrs)");
    tickerSheet.value(startR + 6, startC + 0, "growth rate (6-10 yrs)");
    tickerSheet.value(startR + 7, startC + 0, "discount rate");
    tickerSheet.value(startR + 8, startC + 0, "terminal value (multiple of FCF)");
    tickerSheet.value(startR + 9, startC + 0, "Free Cash Flow year 0");
    tickerSheet.value(startR + 10, startC + 0, "Stock Based Compensation");
    tickerSheet.value(startR + 11, startC + 0, "Net Debt");
    tickerSheet.value(startR + 12, startC + 0, "Shares outstanding");

    tickerSheet.value(startR + 1, startC + 1, "Inputs");
    tickerSheet.formula(startR + 2, startC + 1, "GOOGLEFINANCE(\"" + ticker.getTicker() + "\")");
    tickerSheet.value(startR + 5, startC + 1, ticker.getGrowthNext5Years());
    tickerSheet.style(startR + 5, startC + 1).format("0.00%").set();
    tickerSheet.value(startR + 6, startC + 1, ticker.getGrowthNext5Years() * 0.8);
    tickerSheet.style(startR + 6, startC + 1).format("0.00%").set();
    tickerSheet.value(startR + 7, startC + 1, discountRate);
    tickerSheet.style(startR + 7, startC + 1).format("0.00%").set();
    tickerSheet.value(startR + 8, startC + 1, terminalValue);
    tickerSheet.value(startR + 9, startC + 1, ticker.getFreeCashFlow());
    tickerSheet.value(startR + 10, startC + 1, ticker.getStockBasedCompensation());
    tickerSheet.value(startR + 11, startC + 1, ticker.getNetDebt());
    tickerSheet.value(startR + 12, startC + 1, ticker.getNumberOfShares());

    tickerSheet.value(startR + 1, startC + 3, "Year");
    tickerSheet.value(startR + 2, startC + 3, 1);
    tickerSheet.value(startR + 3, startC + 3, 2);
    tickerSheet.value(startR + 4, startC + 3, 3);
    tickerSheet.value(startR + 5, startC + 3, 4);
    tickerSheet.value(startR + 6, startC + 3, 5);
    tickerSheet.value(startR + 7, startC + 3, 6);
    tickerSheet.value(startR + 8, startC + 3, 7);
    tickerSheet.value(startR + 9, startC + 3, 8);
    tickerSheet.value(startR + 10, startC + 3, 9);
    tickerSheet.value(startR + 11, startC + 3, 10);
    tickerSheet.value(startR + 12, startC + 3, 10);
    tickerSheet.style(startR + 13, startC + 3).bold().horizontalAlignment("center").set();
    tickerSheet.style(startR + 14, startC + 3).bold().horizontalAlignment("center").set();
    tickerSheet.style(startR + 15, startC + 3).bold().horizontalAlignment("center").set();
    tickerSheet.style(startR + 16, startC + 3).bold().horizontalAlignment("center").set();
    tickerSheet.style(startR + 17, startC + 3).bold().horizontalAlignment("center").verticalAlignment("center")
        .set();
    tickerSheet.value(startR + 13, startC + 3, "Present Value of Future Cash Flows");
    tickerSheet.value(startR + 14, startC + 3, "Intrinsic Value");
    tickerSheet.value(startR + 15, startC + 3, "Intrinsic Value Per Share");
    tickerSheet.value(startR + 16, startC + 3, "Current Margin of Safety %");
    tickerSheet.value(startR + 17, startC + 3, "Margin of Safety Target Prices");

    tickerSheet.value(startR + 1, startC + 4, "Free Cash Flow");
    tickerSheet.formula(startR + 2, startC + 4, String.format("(B%d - B%d) * (1+$B$%d)", startR + 10, startR + 11, startR + 6));
    tickerSheet.formula(startR + 3, startC + 4, String.format("E%d*(1+$B$%d)", startR + 3, startR + 6));
    tickerSheet.formula(startR + 4, startC + 4, String.format("E%d*(1+$B$%d)", startR + 4, startR + 6));
    tickerSheet.formula(startR + 5, startC + 4, String.format("E%d*(1+$B$%d)", startR + 5, startR + 6));
    tickerSheet.formula(startR + 6, startC + 4, String.format("E%d*(1+$B$%d)", startR + 6, startR + 6));
    tickerSheet.formula(startR + 7, startC + 4, String.format("E%d*(1+$B$%d)", startR + 7, startR + 6));
    tickerSheet.formula(startR + 8, startC + 4, String.format("E%d*(1+$B$%d)", startR + 8, startR + 6));
    tickerSheet.formula(startR + 9, startC + 4, String.format("E%d*(1+$B$%d)", startR + 9, startR + 6));
    tickerSheet.formula(startR + 10, startC + 4, String.format("E%d*(1+$B$%d)", startR + 10, startR + 6));
    tickerSheet.formula(startR + 11, startC + 4, String.format("E%d*(1+$B$%d)", startR + 11, startR + 6));
    tickerSheet.formula(startR + 12, startC + 4, String.format("B%d * E%d", startR + 9, startR + 12));

    tickerSheet.value(startR + 17, startC + 4, "10%");
    tickerSheet.value(startR + 18, startC + 4, "20%");
    tickerSheet.value(startR + 19, startC + 4, "30%");
    tickerSheet.value(startR + 20, startC + 4, "40%");
    tickerSheet.value(startR + 21, startC + 4, "50%");
    tickerSheet.style(startR + 17, startC + 4).horizontalAlignment("right").set();
    tickerSheet.style(startR + 18, startC + 4).horizontalAlignment("right").set();
    tickerSheet.style(startR + 19, startC + 4).horizontalAlignment("right").set();
    tickerSheet.style(startR + 20, startC + 4).horizontalAlignment("right").set();
    tickerSheet.style(startR + 21, startC + 4).horizontalAlignment("right").set();

    tickerSheet.value(startR + 1, startC + 5, "Present Value");
    tickerSheet.formula(startR + 2, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 3, startR + 8, startR + 3));
    tickerSheet.formula(startR + 3, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 4, startR + 8, startR + 4));
    tickerSheet.formula(startR + 4, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 5, startR + 8, startR + 5));
    tickerSheet.formula(startR + 5, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 6, startR + 8, startR + 6));
    tickerSheet.formula(startR + 6, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 7, startR + 8, startR + 7));
    tickerSheet.formula(startR + 7, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 8, startR + 8, startR + 8));
    tickerSheet.formula(startR + 8, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 9, startR + 8, startR + 9));
    tickerSheet.formula(startR + 9, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 10, startR + 8, startR + 10));
    tickerSheet.formula(startR + 10, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 11, startR + 8, startR + 11));
    tickerSheet.formula(startR + 11, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 12, startR + 8, startR + 12));
    tickerSheet.formula(startR + 12, startC + 5, String.format("E%d/POW(1+$B$%d;D%d)", startR + 13, startR + 8, startR + 13));
    tickerSheet.formula(startR + 13, startC + 5, String.format("SUM(F%d:F%d)", startR + 3, startR + 13));
    tickerSheet.formula(startR + 14, startC + 5, String.format("F%d+B%d", startR + 14, startR + 12));
    tickerSheet.formula(startR + 15, startC + 5, String.format("F%d/B%d", startR + 15, startR + 13));
    tickerSheet.style(startR + 15, startC + 5).format("0.00").set();
    tickerSheet.formula(startR + 16, startC + 5, String.format("1-(B%d/F%d)", startR + 3, startR + 16));
    tickerSheet.style(startR + 16, startC + 5).format("0.00%").set();

    tickerSheet.formula(startR + 17, startC + 5, String.format("$F$%d*(1-E%d)", startR + 16, startR + 18));
    tickerSheet.formula(startR + 18, startC + 5, String.format("$F$%d*(1-E%d)", startR + 16, startR + 19));
    tickerSheet.formula(startR + 19, startC + 5, String.format("$F$%d*(1-E%d)", startR + 16, startR + 20));
    tickerSheet.formula(startR + 20, startC + 5, String.format("$F$%d*(1-E%d)", startR + 16, startR + 21));
    tickerSheet.formula(startR + 21, startC + 5, String.format("$F$%d*(1-E%d)", startR + 16, startR + 22));
    tickerSheet.style(startR + 17, startC + 5).format("0.00").set();
    tickerSheet.style(startR + 18, startC + 5).format("0.00").set();
    tickerSheet.style(startR + 19, startC + 5).format("0.00").set();
    tickerSheet.style(startR + 20, startC + 5).format("0.00").set();
    tickerSheet.style(startR + 21, startC + 5).format("0.00").set();


    tickerSheet.range(startR + 13, startC + 3, startR + 13, 4).merge();
    tickerSheet.range(startR + 14, startC + 3, startR + 14, 4).merge();
    tickerSheet.range(startR + 15, startC + 3, startR + 15, 4).merge();
    tickerSheet.range(startR + 16, startC + 3, startR + 16, 4).merge();
    tickerSheet.range(startR + 17, startC + 3, startR + 21, 3).merge();
    tickerSheet.range(startR + 5, startC + 0, startR + 8, 1).style().borderStyle("solid").borderColor(BorderSide.RIGHT,
        Color.RED).set();
  }
}

