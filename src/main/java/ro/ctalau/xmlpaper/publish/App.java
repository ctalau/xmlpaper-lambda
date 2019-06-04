package ro.ctalau.xmlpaper.publish;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class App implements RequestStreamHandler {

  public void handleRequest(InputStream input, OutputStream output, Context arg2) throws IOException {
    // Everything is unpacked in "/var/task".
    File xslFile = new File("/var/task/resources/addPlaceholders.xsl");
    Source xsl = new StreamSource(xslFile);
    
    // The input is a JSON object with a body field. 
    ObjectMapper mapper = new ObjectMapper();
    JsonNode event = mapper.readTree(input);
    String payload = event.get("body").asText();
    System.out.println(payload);
    Source xmlInput = new StreamSource(new StringReader(payload));
    
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Result result = new StreamResult(outputStream);

    try {
      Transformer transformer = new com.icl.saxon.TransformerFactoryImpl().newTransformer(xsl);
      transformer.transform(xmlInput, result);
    } catch (TransformerException e) {
      throw new IOException("Cannot transform", e);
    }
    
    // Writing the result.
    Map<String, Object> response = new HashMap<>();
    response.put("headers", Collections.emptyMap());
    response.put("statusCode", 200);
    String transformed = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    System.out.println(transformed);
    response.put("body", transformed);
    
    mapper.writeValue(output, response);
  }
}

