package ro.ctalau.xmlpaper.publish;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lambda function that converts an XML file to PDF.
 *
 * @author cristi_talau
 */
public class App implements RequestStreamHandler {

  /**
   * Request handler function.
   */
  public void handleRequest(InputStream input, OutputStream output, Context arg2) throws IOException {
    String payload = getPayload(input);
    System.out.println("Event received.");
   
    Publisher publisher = new Publisher("/var/task/resources/");

    try {
      ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
      byte[] pdfBytes = publisher.generatePdf(xmlInputStream);
      sendBytes(output, pdfBytes);
    } catch (TransformerException | SAXException | InterruptedException | ExecutionException e) {
      throw new IOException("Cannot transform", e);
    }
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
    return event.get("body").asText();
  }

  /**
   * Send the bytes as a response Base64 encoded.
   * 
   * @param output The output stream..
   * @param bytes The bytes to send.
   * 
   * @throws IOException
   */
  private void sendBytes(OutputStream output, byte[] bytes) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    
    Map<String, Object> response = new HashMap<>();
    response.put("headers", Collections.emptyMap());
    response.put("statusCode", 200);
    String transformed = Base64.getEncoder().encodeToString(bytes);
    response.put("body", transformed);
    
    mapper.writeValue(output, response);
  }

}

