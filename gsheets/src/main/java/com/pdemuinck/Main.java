package com.pdemuinck;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

public class Main {
  public static void main(String[] args) {
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

    List<Ticker> tickers = new ArrayList<>();
    try (ExecutorService myExecutor = Executors.newFixedThreadPool(4)) {
      for (String arg : args) {
        Future<Ticker> submit = myExecutor.submit(() -> new Ticker(arg));
        Ticker ticker = submit.get();
        if(!ticker.skip){
          tickers.add(ticker);
        }
      }
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    try (OutputStream os = new FileOutputStream("S&P500.xlsx");
         Workbook wb = new Workbook(os, "S&P500", "1.0")) {
      Worksheet summary = wb.newWorksheet("Summary");
      List<String> labels =
          List.of("Ticker", "Discount Rate", "Terminal Value", "DCF Valuation", "Current Price",
              "Current Margin of Safety");

      for (int c = 0; c < labels.size(); c++) {
        summary.value(0, c, labels.get(c));
      }

      for (int i = 0; i < tickers.size(); i++) {
        Ticker ticker = tickers.get(i);
        double discountRate = 0.10;
        double terminalValue = 15;
        double valuation = DCF.calculateDcf(ticker.getFreeCashFlow(), ticker.getGrowthNext5Years(),
            ticker.getGrowthNext5Years() * 0.8, ticker.getNetDebt(), ticker.getNumberOfShares(),
            discountRate,
            terminalValue);
        summary.value(i + 1, 0, args[i]);
        summary.value(i + 1, 1, discountRate);
        summary.value(i + 1, 2, terminalValue);
        summary.value(i + 1, 3, valuation);
        summary.formula(i + 1, 4, "GOOGLEFINANCE(\"" + args[i] + "\")");
        summary.formula(i + 1, 5, String.format("1 - (E%d / D%d)", i + 2, i + 2));
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
