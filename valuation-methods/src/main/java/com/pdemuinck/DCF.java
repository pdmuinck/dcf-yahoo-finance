package com.pdemuinck;

import java.util.Arrays;

public class DCF {


  public static double calculateDcf(double freeCashFlow, double growthRateNext5Years,
                                    double growthRate6To10, double netDebt, double numberOfShares,
                                    double discountRate, double terminalValue) {


    double[] futureCashFlows = new double[10];
    double[] discountedCashFlows = new double[10];
    futureCashFlows[0] = freeCashFlow * (1 + growthRateNext5Years);
    discountedCashFlows[0] = futureCashFlows[0] / 1.1;
    for (int i = 1; i < futureCashFlows.length; i++) {
      double growth = (1 + growthRateNext5Years);
      if (i > 4) {
        growth = (1 + growthRate6To10);
      }
      futureCashFlows[i] = futureCashFlows[i - 1] * growth;
      discountedCashFlows[i] = futureCashFlows[i] / Math.pow(1.0 + discountRate, i + 1);
    }
    double presentValueOfAllCashFlows =
        Arrays.stream(discountedCashFlows).sum() +
            (discountedCashFlows[discountedCashFlows.length - 1] * terminalValue);
    double intrinsicValue = presentValueOfAllCashFlows + netDebt;
    return intrinsicValue / numberOfShares;
  }
}
