package com.pdemuinck;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DCFTest {

  @Test
  public void calculateDCF(){
    double output = DCF.calculateDcf(100.0, 0.10, 0.10, 0.0, 100.0, 0.10, 15.0);
    assertThat(Math.round(output)).isEqualTo(25L);
  }
}
