package com.pdemuinck;

public class App {

  public static void main(String... args){
    Ticker ticker = new Ticker(args[0]);
    System.out.println(DCF.calculateDcf(ticker.getFreeCashFlow(), ticker.getGrowthNext5Years(),
        ticker.getGrowthNext5Years() * 0.8, ticker.getNetDebt(), ticker.getNumberOfShares(), 0.10, 15.0));
  }
}
