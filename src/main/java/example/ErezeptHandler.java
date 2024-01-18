package example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ErezeptHandler {

  public static void main(String[] args) throws IOException {
    ErezeptHandler erezeptHandler = new ErezeptHandler();
    String bundleString = erezeptHandler.readFile(
        "testDaten/OK414_2_Rezepte_TA7_V004_eAbrech_1.3_eVo_1.1.0._eQuitt_1.2_eAbgabe_1.3.xml");
    System.out.println(bundleString);
    FHIRHandler handler = new FHIRHandler();
    handler.handleFhirString(bundleString);
  }

  private String readFile(String pfad) throws IOException {
    Path path = Paths.get(pfad);

    Stream<String> lines = Files.lines(path);
    String data = lines.collect(Collectors.joining("\n"));
    lines.close();
    return data;
  }


}
