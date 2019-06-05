package ro.ctalau.xmlpaper.publish;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
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

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

/**
 * Class with publishing utility functions.
 * 
 * @author cristi_talau
 */
public class Publisher {
  
  /**
   * The resources folder.
   */
  private final String resourcesFolder;
  
  /**
   * Constructor.
   * 
   * @param resourcesFolder Folder that contains resources used for publishing: a folder with the
   * docbook folder and a "fop.xconf" file. 
   */
  public Publisher(String resourcesFolder) {
    this.resourcesFolder = resourcesFolder;
  }
  
  /**
   * Transformer factory.
   */
  private final com.icl.saxon.TransformerFactoryImpl factoryImpl = 
      new com.icl.saxon.TransformerFactoryImpl();
  

  /**
   * Creates the docbook transformer.
   * 
   * @param resourcesFolder The folder with resources.
   * @param factoryImpl The transformer factory.
   * @return The transformer.
   * 
   * @throws TransformerConfigurationException
   */
  public Transformer createDocbookXSLTransformer()
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
  

  /**
   * Start the FOP initialization.
   * 
   * @param buffer3 The buffer where the output will be stored.
   * 
   * @return A future of the fop instance.
   */
  public Future<Fop> createFop(ByteArrayOutputStream buffer3) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    return executorService.submit(() -> {
      FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI(), this.getClass().getResourceAsStream("/fop.xconf"));
      File fontsCache = new File(System.getProperty("java.io.tmpdir"), "fop-fonts.cache");
      fopFactory.getFontManager().setCacheFile(fontsCache.toURI());
      return fopFactory.newFop(MimeConstants.MIME_PDF, buffer3);
    });
  }
  
  /**
   * @return A new identity transformer.
   * 
   * @throws TransformerConfigurationException 
   */
  public Transformer newIdTransformer() throws TransformerConfigurationException {
    return this.factoryImpl.newTransformer();
  }
  
  /**
   * @return A placeholder expander.
   * @throws TransformerConfigurationException
   */
  public Transformer createPlaceholderExpander() throws TransformerConfigurationException {
    return factoryImpl.newTransformer(
        new StreamSource(this.getClass().getResourceAsStream("/addPlaceholders.xsl")));
  }
  /**
   * Generates a PDF from the given XML.
   * 
   * @param xml The XML file.
   * 
   * @return The PDF bytes.
   * 
   * @throws TransformerException
   * @throws FOPException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public byte[] generatePdf(InputStream xml) throws TransformerException, FOPException, InterruptedException, ExecutionException {
    ByteArrayOutputStream pdfBuffer = new ByteArrayOutputStream();
    Future<Fop> fop = createFop(pdfBuffer);

    Source xmlInput = new StreamSource(xml);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Transformer transformer = createPlaceholderExpander();
    transformer.transform(xmlInput, new StreamResult(buffer));
    
    System.out.println("Placeholders added.");
    
    ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
    Transformer transformer2 = createDocbookXSLTransformer();
    transformer2.transform(
        new StreamSource(new ByteArrayInputStream(buffer.toByteArray())), 
        new StreamResult(buffer2));
    
    System.out.println("FOP generated.");

    Transformer idTransformer = newIdTransformer();
    Source src = new StreamSource(new ByteArrayInputStream(buffer2.toByteArray()));
    Result res = new SAXResult(fop.get().getDefaultHandler());
    idTransformer.transform(src, res);
    
    System.out.println("PDF generated.");
    
    return pdfBuffer.toByteArray();
  }
}
