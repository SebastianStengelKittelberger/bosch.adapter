package de.kittelberger.bosch.adapter.data;

import de.kittelberger.webexport602w.solr.api.generated.*;
import de.kittelberger.webexport602w.solr.api.utils.UnmarshallingUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Component
public class XmlFileLoader {

  private static final Logger log = LoggerFactory.getLogger(XmlFileLoader.class);

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

  public List<Webexport> parseFilesOfType(String type) {
    List<Webexport> result = new ArrayList<>();
    File dir = new File(xmlDirectory);
    if (!dir.exists() || !dir.isDirectory()) {
      log.warn("XML directory not found or not a directory: {}", dir.getAbsolutePath());
      return result;
    }
    File[] files = dir.listFiles((d, name) -> name.endsWith("_" + type + ".xml"));
    if (files == null) return result;
    Arrays.sort(files);
    log.debug("Loading {} '{}' files from {}", files.length, type, dir.getAbsolutePath());
    for (File file : files) {
      try {
        String content = Files.readString(file.toPath());
        Webexport we = UnmarshallingUtil.unmarshalFromXML(content, Webexport.class);
        if (we != null) {
          result.add(we);
        } else {
          log.warn("Parsing returned null for file: {}", file.getName());
        }
      } catch (Exception e) {
        log.error("Failed to parse file {}: {}", file.getName(), e.getMessage(), e);
      }
    }
    log.debug("Loaded {} '{}' webexport objects", result.size(), type);
    return result;
  }
}
