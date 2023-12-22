package com.pdemuinck;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.dhatim.fastexcel.HyperLink;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

public class Main {
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
        double valuation = DCF.calculateDcf(ticker.getFreeCashFlow() - ticker.getStockBasedCompensation(), ticker.getGrowthNext5Years(),
            ticker.getGrowthNext5Years() * 0.8, ticker.getNetDebt(), ticker.getNumberOfShares(),
            discountRate,
            terminalValue);
        Worksheet tickerSheet = wb.newWorksheet(ticker.getTicker());
        tickerSheet.value(0, 0, ticker.getTicker());
        tickerSheet.value(2, 0, "current");
        tickerSheet.value(3, 0, "H52");
        tickerSheet.value(4, 0, "L52");
        tickerSheet.value(5, 0, "growth rate (1-5 yrs)");
        tickerSheet.value(6, 0, "growth rate (6-10 yrs)");
        tickerSheet.value(7, 0, "discount rate");
        tickerSheet.value(8, 0, "terminal value (multiple of FCF)");
        tickerSheet.value(9, 0, "Free Cash Flow year 0");
        tickerSheet.value(10, 0, "Stock Based Compensation");
        tickerSheet.value(11, 0, "Net Debt");
        tickerSheet.value(12, 0, "Shares outstanding");

        tickerSheet.value(1, 1, "Inputs");
        tickerSheet.formula(2, 1, "GOOGLEFINANCE(\"" + ticker.getTicker() + "\")");
        tickerSheet.formula(3, 1, "GOOGLEFINANCE(\"" + ticker.getTicker() + "\";\"high52\")");
        tickerSheet.formula(4, 1, "GOOGLEFINANCE(\"" + ticker.getTicker() + "\";\"low52\")");
        tickerSheet.value(5, 1, ticker.getGrowthNext5Years());
        tickerSheet.value(6, 1, ticker.getGrowthNext5Years() * 0.8);
        tickerSheet.value(7, 1, discountRate);
        tickerSheet.value(8, 1, terminalValue);
        tickerSheet.value(9, 1, ticker.getFreeCashFlow());
        tickerSheet.value(10, 1, ticker.getStockBasedCompensation());
        tickerSheet.value(11, 1, ticker.getNetDebt());
        tickerSheet.value(12, 1, ticker.getNumberOfShares());

        tickerSheet.value(1, 3, "Year");
        tickerSheet.value(2, 3, 1);
        tickerSheet.value(3, 3, 2);
        tickerSheet.value(4, 3, 3);
        tickerSheet.value(5, 3, 4);
        tickerSheet.value(6, 3, 5);
        tickerSheet.value(7, 3, 6);
        tickerSheet.value(8, 3, 7);
        tickerSheet.value(9, 3, 8);
        tickerSheet.value(10, 3, 9);
        tickerSheet.value(11, 3, 10);
        tickerSheet.value(12, 3, 10);
        tickerSheet.value(13, 3, "Present Value of Future Cash Flows");
        tickerSheet.value(14, 3, "Intrinsic Value");
        tickerSheet.value(15, 3, "Intrinsic Value Per Share");
        tickerSheet.value(16, 3, "Current Margin of Safety %");
        tickerSheet.value(17, 3, "Margin of Safety Target Prices");
        tickerSheet.style(13, 3).bold();
        tickerSheet.style(14, 3).bold();
        tickerSheet.style(15, 3).bold();
        tickerSheet.style(16, 3).bold();
        tickerSheet.style(17, 3).bold().fontSize(23);

        tickerSheet.value(1, 4, "Free Cash Flow");
        tickerSheet.formula(2, 4, "(B10 - B11) * (1+$B$6)");
        tickerSheet.formula(3, 4, "E3*(1+$B$6)");
        tickerSheet.formula(4, 4, "E4*(1+$B$6)");
        tickerSheet.formula(5, 4, "E5*(1+$B$6)");
        tickerSheet.formula(6, 4, "E6*(1+$B$6)");
        tickerSheet.formula(7, 4, "E7*(1+$B$7)");
        tickerSheet.formula(8, 4, "E8*(1+$B$7)");
        tickerSheet.formula(9, 4, "E9*(1+$B$7)");
        tickerSheet.formula(10, 4, "E10*(1+$B$7)");
        tickerSheet.formula(11, 4, "E11*(1+$B$7)");
        tickerSheet.formula(12, 4, "B9 * E12");
        tickerSheet.value(17, 4, "10%");
        tickerSheet.value(18, 4, "20%");
        tickerSheet.value(19, 4, "30%");
        tickerSheet.value(20, 4, "40%");
        tickerSheet.value(21, 4, "50%");

        tickerSheet.value(1, 5, "Present Value");
        tickerSheet.formula(2, 5, "E3/POW(1+$B$8;D3)");
        tickerSheet.formula(3, 5, "E4/POW(1+$B$8;D4)");
        tickerSheet.formula(4, 5, "E5/POW(1+$B$8;D5)");
        tickerSheet.formula(5, 5, "E6/POW(1+$B$8;D6)");
        tickerSheet.formula(6, 5, "E7/POW(1+$B$8;D7)");
        tickerSheet.formula(7, 5, "E8/POW(1+$B$8;D8)");
        tickerSheet.formula(8, 5, "E9/POW(1+$B$8;D9)");
        tickerSheet.formula(9, 5, "E10/POW(1+$B$8;D10)");
        tickerSheet.formula(10, 5, "E11/POW(1+$B$8;D11)");
        tickerSheet.formula(11, 5, "E12/POW(1+$B$8;D12)");
        tickerSheet.formula(12, 5, "E13/POW(1+$B$8;D13)");
        tickerSheet.formula(13, 5, "SUM(F3:F13)");
        tickerSheet.formula(14, 5, "F14+B12");
        tickerSheet.formula(15, 5, "F15/B13");
        tickerSheet.formula(16, 5, "1-(B3/F16)");
        tickerSheet.formula(17, 5, "$F$16*(1-E18)");
        tickerSheet.formula(18, 5, "$F$16*(1-E19)");
        tickerSheet.formula(19, 5, "$F$16*(1-E20)");
        tickerSheet.formula(20, 5, "$F$16*(1-E21)");
        tickerSheet.formula(21, 5, "$F$16*(1-E22)");


        tickerSheet.range(13, 3, 13, 4).merge();
        tickerSheet.range(14, 3, 14, 4).merge();
        tickerSheet.range(15, 3, 15, 4).merge();
        tickerSheet.range(16, 3, 16, 4).merge();
        tickerSheet.range(5, 0, 8, 1).style().borderColor("red");

        summary.value(i + 1, 0, ticker.getTicker());
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
