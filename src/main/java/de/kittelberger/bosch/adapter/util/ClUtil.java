package de.kittelberger.bosch.adapter.util;

import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.ClngTextList2000;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Map;

public class ClUtil {
  private ClUtil() {}

  public static final String ID_LOCALE_JAVA_LANGUAGE = "in";
  public static final String IL_LOCALE_JAVA_LANGUAGE = "iw";

  /**
   * Returns the correct translation of the attr text value based on a locale
   * !!! Caution: falls back to system language if no translation is found !!!
   * @param attrval
   * @param locale
   * @return
   */
  public static String getAttrCltextValByLocale(
    final Attrval attrval,
    final Locale locale
  ) {
    final String textVal = ClUtil.getValue(attrval.getCltextval(), locale);
    if (textVal == null) {
      return attrval.getTextval();
    }
    return textVal;
  }


  public static String getCleanedValue(
    final Map<String, String> localizedValues,
    final Locale locale
  ) {
    final String rawValue = getValue(localizedValues, locale);
    return removeLineBreaksAndTrim(rawValue);
  }

  public static String removeLineBreaksAndTrim(final String text) {
    return StringUtils.trim(
      StringUtils.replace(
        text,
        "\n",
        " "
      )
    );
  }

  public static String getValue(final Map<String, String> map, final Locale locale) {
    if(map !=null){
      return map.get(getClKey(locale));
    }else{
      return "";
    }
  }

  public static String getValue(final ClngTextList2000 clNode, Locale locale) {
    return getValueInternal(clNode, getClKey(locale));
  }

  private static String getValueInternal(final ClngTextList2000 clNode, final String cl) {
    if (null != clNode && CollectionUtils.isNotEmpty(clNode.getClText2000())) {
      for (final ClngTextList2000.ClText2000 clText2000 : clNode.getClText2000()) {
        if (clText2000.getCl().equalsIgnoreCase(cl)) {
          return clText2000.getValue();
        }
      }
    }

    return null;
  }

  public static String getClKey(final Locale locale) {
    return getClKey(locale.getCountry(), getLanguageIsoCode(locale));
  }

  public static String getClKey(final String country, final String language) {
    return language + "-" + country.toUpperCase(Locale.ENGLISH);
  }

  /**
   * @param locale
   * @return two letter Language ISO Code
   */
  public static String getLanguageIsoCode(final Locale locale) {
    String resultIsoLang = "";
    final String receivedLanguage = locale.getLanguage().toLowerCase();
    if (ID_LOCALE_JAVA_LANGUAGE.equals(receivedLanguage) || IL_LOCALE_JAVA_LANGUAGE.equals(receivedLanguage)) {
      String isoLocale = locale.toLanguageTag().toLowerCase();
      final String[] localeSplit = StringUtils.split(isoLocale, "-");
      if (localeSplit.length >= 1) {
        resultIsoLang = localeSplit[0].trim();
      }
    } else {
      resultIsoLang = receivedLanguage;
    }
    return resultIsoLang;
  }

}
