package de.kittelberger.bosch.adapter.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class UrlBuilderUtil {

  private static final String DEFAULT_MEDIAOBJECT_OPTIMIZED_SCHEME = "/binary/ocsmedia/full/{mediaObjectFilename}";

  /** Standard-URL: /binary/ocsmedia/optimized/full/{filename} */
  public String getMediaObjectUrl(String mediaObjectFilename, String protocolAndDomain) {
    String returnUrl = protocolAndDomain.concat(DEFAULT_MEDIAOBJECT_OPTIMIZED_SCHEME);
    returnUrl = replaceMediaObjectFilename(returnUrl, mediaObjectFilename);
    return replaceOldDomainOrSetWithNewDomain(returnUrl, "", protocolAndDomain);
  }


  private String replaceMediaObjectFilename(String input, String mediaObjectFilename) {
    return input.replace("{mediaObjectFilename}", mediaObjectFilename);
  }

  public static String replaceOldDomainOrSetWithNewDomain(final String originalString, final String partWhichShouldBeReplaced, final String partWhichWillReplaceWith) {
    String returnString = originalString;
    if (StringUtils.isNotBlank(originalString) && StringUtils.isNotBlank(partWhichShouldBeReplaced) && StringUtils.isNotBlank(partWhichWillReplaceWith)) {
      returnString = originalString.replace(partWhichShouldBeReplaced, partWhichWillReplaceWith);
    }
    if (StringUtils.startsWith(returnString, "/") && StringUtils.isNotBlank(partWhichWillReplaceWith)) {
      returnString = partWhichWillReplaceWith + returnString;
    }
    return returnString;
  }
}
