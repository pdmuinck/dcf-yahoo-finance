package com.pdemuinck;

import java.util.logging.Logger;

public class Ticker {

  String ticker;
  double freeCashFlow;


  double stockBasedCompensation;
  double netDebt;
  double growthNext5Years;

  public Ticker(String ticker, double freeCashFlow, double netDebt,
                double growthNext5Years, double numberOfShares, double stockBasedCompensation) {
    this.ticker = ticker;
    this.freeCashFlow = freeCashFlow;
    this.netDebt = netDebt;
    this.growthNext5Years = growthNext5Years;
    this.numberOfShares = numberOfShares;
    this.stockBasedCompensation = stockBasedCompensation;
  }

  double numberOfShares;

  private Ticker() {
  }


  public String getTicker() {
    return ticker;
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

  public double getStockBasedCompensation() {
    return stockBasedCompensation;
  }
}
