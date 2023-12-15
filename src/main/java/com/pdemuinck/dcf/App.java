package com.pdemuinck.dcf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class App {

  public static void main(String... args) throws URISyntaxException, IOException, InterruptedException {
    System.out.println(Ticker.calculateDcf(args[0]));
  }
}
