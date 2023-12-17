package com.pdemuinck;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

public class ExcelWriter {

  public static void createMarketWorkbook(String collectionName, Map<String, List<Double>> lines) {
    try (OutputStream os = new FileOutputStream(collectionName + ".xlsx");
         Workbook wb = new Workbook(os, collectionName, "1.0")) {
      Worksheet summary = wb.newWorksheet("Summary");


      summary.value(0, 0, "blabla");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
