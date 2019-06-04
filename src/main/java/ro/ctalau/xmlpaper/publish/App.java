package ro.ctalau.xmlpaper.publish;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class App implements RequestStreamHandler {

  /**
   * Transformer factory.
   */
  private final com.icl.saxon.TransformerFactoryImpl factoryImpl = 
      new com.icl.saxon.TransformerFactoryImpl();
  private final String resourcesFolder = "/var/task/resources/";

  /**
   * Request handler function.
   */
  public void handleRequest(InputStream input, OutputStream output, Context arg2) throws IOException {
    String payload = getPayload(input);
    System.out.println("Event received.");

    ByteArrayOutputStream pdfBuffer = new ByteArrayOutputStream();
    Future<Fop> fop = createFop(pdfBuffer);

    try {
      Source xmlInput = new StreamSource(new StringReader(payload));
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      Transformer transformer = factoryImpl.newTransformer(
          new StreamSource(new File(resourcesFolder + "addPlaceholders.xsl")));
      transformer.transform(xmlInput, new StreamResult(buffer));
      
      System.out.println("Placeholders added.");
      
      ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
      Transformer transformer2 = createDocbookXSLTransformer();
      transformer2.transform(
          new StreamSource(new ByteArrayInputStream(buffer.toByteArray())), 
          new StreamResult(buffer2));
      
      System.out.println("FOP generated.");

      Transformer idTransformer = factoryImpl.newTransformer();
      Source src = new StreamSource(new ByteArrayInputStream(buffer2.toByteArray()));
      Result res = new SAXResult(fop.get().getDefaultHandler());
      idTransformer.transform(src, res);
      
      System.out.println("PDF generated.");

      sendBytes(output, pdfBuffer.toByteArray());
    } catch (TransformerException | SAXException | InterruptedException | ExecutionException e) {
      throw new IOException("Cannot transform", e);
    }
  }

  /**
   * Start the FOP initialization.
   * 
   * @param buffer3 The buffer where the output will be stored.
   * 
   * @return A future of the fop instance.
   */
  private Future<Fop> createFop(ByteArrayOutputStream buffer3) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    return executorService.submit(() -> {
      FopFactory fopFactory = FopFactory.newInstance(new File(resourcesFolder + "fop.xconf"));
      File fontsCache = new File(System.getProperty("java.io.tmpdir"), "fop-fonts.cache");
      fopFactory.getFontManager().setCacheFile(fontsCache.toURI());
      Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, buffer3);
      return fop;
    });
  }

  /**
   * Parse the input as a JSON and return the body field.
   * @param input The input.
   * @return The payload.
   * @throws IOException
   */
  private String getPayload(InputStream input) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode event = mapper.readTree(input);
    String payload = event.get("body").asText();
    return payload;
  }

  /**
   * Send the bytes as a response Base64 encoded.
   * 
   * @param output The output stream..
   * @param bytes The bytes to send.
   * @throws IOException
   * @throws JsonGenerationException
   * @throws JsonMappingException
   */
  private void sendBytes(OutputStream output, byte[] bytes)
      throws IOException, JsonGenerationException, JsonMappingException {
    ObjectMapper mapper = new ObjectMapper();
    
    Map<String, Object> response = new HashMap<>();
    response.put("headers", Collections.emptyMap());
    response.put("statusCode", 200);
    String transformed = Base64.getEncoder().encodeToString(bytes);
    response.put("body", transformed);
    
    mapper.writeValue(output, response);
  }

  /**
   * Creates the docbook transformer.
   * 
   * @param resourcesFolder The folder with resources.
   * @param factoryImpl The transformer factory.
   * @return The transformer.
   * 
   * @throws TransformerConfigurationException
   */
  private Transformer createDocbookXSLTransformer()
      throws TransformerConfigurationException {
    File xslFile2 = new File(resourcesFolder + "docbook/xsl/fo/docbook_custom.xsl");
      Transformer transformer2 = factoryImpl.newTransformer(new StreamSource(xslFile2));
    transformer2.setParameter("admon.graphics", 1);
    transformer2.setParameter("admon.graphics.extension", ".png");
    transformer2.setParameter("admon.graphics.path", 
        resourcesFolder + "docbook/css/img/");
    transformer2.setParameter("body.font.family", "Times New Roman, Arial Unicode MS, Tahoma, Batang, serif");
    transformer2.setParameter("callout.graphics.path", 
        resourcesFolder + "docbook/xsl/images/callouts/");
    transformer2.setParameter("draft.mode", "no");
    transformer2.setParameter("fop.extensions", 0);
    transformer2.setParameter("fop1.extensions", 0);
    transformer2.setParameter("highlight.source", 1);
    transformer2.setParameter("highlight.xslthl.config", 
        "file://" + resourcesFolder + "docbook/xsl/highlighting/xslthl-config.xml");
    transformer2.setParameter("admon.graphics", 1);
    transformer2.setParameter("monospace.font.family", 
        "monospace, Courier New, Courier, Consolas, Liberation Mono, Arial Unicode MS");
    transformer2.setParameter("paper.type", "A4");
    transformer2.setParameter("title.font.family", 
        "Arial, Arial Unicode MS, Tahoma, Batang, sans-serif");
    return transformer2;
  }
}

