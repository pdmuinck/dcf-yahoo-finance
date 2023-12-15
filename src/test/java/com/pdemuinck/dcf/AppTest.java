package com.pdemuinck.dcf;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

public class AppTest {

  @Test
  @StdIo
  public void nvdaShouldBeValued(StdOut out)
      throws URISyntaxException, IOException, InterruptedException {
    App.main(new String[] {"NVDA"});
    assertThat(out.capturedLines()).hasSize(1);
    assertThat(Double.parseDouble(out.capturedLines()[0])).isGreaterThan(0.0);
  }
}
