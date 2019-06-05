package ro.ctalau.xmlpaper.publish;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class PublisherIT {
  
  /**
   * <p><b>Description:</b> Test that the conversion works.</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testPDFGeneration() throws Exception {
    FileInputStream xmlInputStream = new FileInputStream(new File("test/paper.xml"));
    byte[] pdfBytes = new Publisher("target/frameworks/").generatePdf(xmlInputStream);
    
    try (FileOutputStream pdfFile = new FileOutputStream("target/paper.pdf")) {
      IOUtils.write(pdfBytes, pdfFile);
    }
  }
}
