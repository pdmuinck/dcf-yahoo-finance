package com.pdemuinck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TickerTest {

  @Test
  public void testNVDAHasValues(){
    Ticker nvda = new Ticker("NVDA");
    assertThat(nvda.getFreeCashFlow()).isGreaterThan(0.0);
    assertThat(nvda.getGrowthNext5Years()).isGreaterThan(0.0);
    assertThat(nvda.getNetDebt()).isNotEqualTo(0.0);
    assertThat(nvda.getNumberOfShares()).isGreaterThan(0.0);
  }

}
