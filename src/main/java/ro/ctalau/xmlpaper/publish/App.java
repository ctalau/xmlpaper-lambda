package ro.ctalau.xmlpaper.publish;

import java.io.ByteArrayInputStream;
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
    String resourcesFolder = "/var/task/resources/";
    File xslFile = new File(resourcesFolder + "addPlaceholders.xsl");
    File xslFile2 = new File(resourcesFolder + "docbook/xsl/fo/docbook_custom.xsl");
    
    
    // The input is a JSON object with a body field. 
    ObjectMapper mapper = new ObjectMapper();
    JsonNode event = mapper.readTree(input);
    String payload = event.get("body").asText();
    Source xmlInput = new StreamSource(new StringReader(payload));
    
    ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();

    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      
      com.icl.saxon.TransformerFactoryImpl factoryImpl = new com.icl.saxon.TransformerFactoryImpl();
      Transformer transformer = factoryImpl.newTransformer(new StreamSource(xslFile));
      transformer.transform(xmlInput, new StreamResult(buffer));
      
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
      transformer2.transform(
          new StreamSource(new ByteArrayInputStream(buffer.toByteArray())), 
          new StreamResult(buffer2));
    } catch (TransformerException e) {
      throw new IOException("Cannot transform", e);
    }
    
    // Writing the result.
    Map<String, Object> response = new HashMap<>();
    response.put("headers", Collections.emptyMap());
    response.put("statusCode", 200);
    String transformed = new String(buffer2.toByteArray(), StandardCharsets.UTF_8);
    System.out.println(transformed);
    response.put("body", transformed);
    
    mapper.writeValue(output, response);
  }
}

