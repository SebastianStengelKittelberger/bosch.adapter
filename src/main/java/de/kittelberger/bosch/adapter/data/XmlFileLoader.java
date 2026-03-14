package de.kittelberger.bosch.adapter.data;

import de.kittelberger.webexport602w.solr.api.generated.Webexport;
import de.kittelberger.webexport602w.solr.api.utils.UnmarshallingUtil;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
public class XmlFileLoader {

  private static final Logger log = LoggerFactory.getLogger(XmlFileLoader.class);
  private static final String ADVASTA_NAMESPACE = "http://www.advasta.com/XMLSchema/6.02w";
  private static final Map<Class<?>, JAXBContext> CONTEXT_CACHE = new ConcurrentHashMap<>();
  private static final XMLInputFactory XML_INPUT_FACTORY = createXmlInputFactory();

  @Value("${xml.directory}")
  private String xmlDirectory;

  @PostConstruct
  void logDirectoryInfo() {
    File dir = new File(xmlDirectory);
    log.info("XML directory configured: {}", dir.getAbsolutePath());
    log.info("  exists={}, isDirectory={}, canRead={}, files={}",
      dir.exists(), dir.isDirectory(), dir.canRead(),
      dir.exists() ? dir.listFiles() != null ? dir.listFiles().length : "listFiles()=null" : "n/a");
  }

  public void forEachFileOfType(String type, Consumer<Webexport> consumer) {
    File dir = new File(xmlDirectory);
    if (!dir.exists() || !dir.isDirectory()) {
      log.warn("XML directory not found or not a directory: {}", dir.getAbsolutePath());
      return;
    }

    File[] files = dir.listFiles((d, name) -> name.endsWith("_" + type + ".xml"));
    if (files == null) {
      return;
    }

    Arrays.sort(files);
    log.debug("Loading {} '{}' files from {}", files.length, type, dir.getAbsolutePath());
    for (File file : files) {
      try {
        Webexport webexport = unmarshal(file, Webexport.class);
        if (webexport != null) {
          consumer.accept(webexport);
        } else {
          log.warn("Parsing returned null for file: {}", file.getName());
        }
      } catch (Exception e) {
        log.error("Failed to parse file {}: {}", file.getName(), e.getMessage(), e);
      }
    }
  }

  public <T> void forEachElementOfType(
    String fileType,
    String elementName,
    Class<T> elementClass,
    Predicate<T> consumer
  ) {
    File dir = new File(xmlDirectory);
    if (!dir.exists() || !dir.isDirectory()) {
      log.warn("XML directory not found or not a directory: {}", dir.getAbsolutePath());
      return;
    }

    File[] files = dir.listFiles((d, name) -> name.endsWith("_" + fileType + ".xml"));
    if (files == null) {
      return;
    }

    Arrays.sort(files);
    log.debug("Streaming {} '{}' files from {}", files.length, fileType, dir.getAbsolutePath());
    for (File file : files) {
      try {
        if (!streamFileElements(file, elementName, elementClass, consumer)) {
          return;
        }
      } catch (Exception e) {
        log.error("Failed to stream file {}: {}", file.getName(), e.getMessage(), e);
      }
    }
  }

  private static JAXBContext getContext(Class<?> type) throws Exception {
    JAXBContext context = CONTEXT_CACHE.get(type);
    if (context != null) {
      return context;
    }

    JAXBContext created = JAXBContext.newInstance(type);
    JAXBContext existing = CONTEXT_CACHE.putIfAbsent(type, created);
    return existing != null ? existing : created;
  }

  private <T> T unmarshal(File file, Class<T> returnClass) throws Exception {
    JAXBContext jaxbContext = getContext(returnClass);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    try (InputStream inputStream = Files.newInputStream(file.toPath())) {
      XMLReader reader = XMLReaderFactory.createXMLReader();
      UnmarshallingUtil.NameSpaceFilter filter = new UnmarshallingUtil.NameSpaceFilter(ADVASTA_NAMESPACE, true);
      filter.setParent(reader);
      SAXSource source = new SAXSource(filter, new InputSource(inputStream));

      JAXBElement<T> object = unmarshaller.unmarshal(source, returnClass);
      return object.getValue();
    }
  }

  private <T> boolean streamFileElements(
    File file,
    String elementName,
    Class<T> elementClass,
    Predicate<T> consumer
  ) throws Exception {
    JAXBContext jaxbContext = getContext(elementClass);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    try (InputStream inputStream = Files.newInputStream(file.toPath())) {
      XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(inputStream);
      try {
        while (reader.hasNext()) {
          if (reader.next() == XMLStreamConstants.START_ELEMENT && elementName.equals(reader.getLocalName())) {
            JAXBElement<T> element = unmarshaller.unmarshal(reader, elementClass);
            if (!consumer.test(element.getValue())) {
              return false;
            }
          }
        }
      } finally {
        reader.close();
      }
    }

    return true;
  }

  private static XMLInputFactory createXmlInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory;
  }
}
