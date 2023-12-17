package com.pdemuinck;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MainTest {

  @Test
  public void createWorkBookNVDA(){
    Main.main(new String[] {"NVDA", "AMD", });
  }


}
