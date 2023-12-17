package com.pdemuinck;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

public class AppTest {

  @Test
  @StdIo
  public void valuateNVDA(StdOut out){
    App.main("NVDA");
    double value = Double.parseDouble(out.capturedLines()[0]);
    assertThat(value).isBetween(0.1, 10000.0);
  }
}
