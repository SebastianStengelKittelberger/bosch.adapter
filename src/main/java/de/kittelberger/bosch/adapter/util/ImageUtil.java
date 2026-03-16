package de.kittelberger.bosch.adapter.util;

import de.kittelberger.bosch.adapter.model.Image;
import de.kittelberger.bosch.adapter.model.ImageDimension;
import de.kittelberger.bosch.adapter.model.MediaObject;
import de.kittelberger.webexport602w.solr.api.dto.MediaobjectDTO;
import de.kittelberger.webexport602w.solr.api.generated.Attrval;
import de.kittelberger.webexport602w.solr.api.generated.Ltattrval;
import de.kittelberger.webexport602w.solr.api.generated.Mediaobject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ImageUtil {

  private static final Long MEDIAOBJECT_OFFSET = 200000000L;
  private static final String SYMBOLIMG_HEADLINE = "SYMBOLIMG_HEADLINE";
  private static final String DESCRIPTION = "DESCRIPTION";
  private static final String ICON_DESCRIPTION = "ICON_DESCRIPTION";


  /**
   * Generates an image item based on a media object
   * @param lobValueTyp the required lob val type
   * @param mediaDomain domain to get the correct domain
   * @param mediaobjectDTO the media object dto
   * @param urlBuilderUtil url builder Util to build url
   * @return imageitem
   */
  public static Image getImageItemFromMediaObject(
    final String lobValueTyp,
    final String mediaDomain,
    final MediaobjectDTO mediaobjectDTO,
    final UrlBuilderUtil urlBuilderUtil
  ) {

    if (mediaobjectDTO == null) {
      return null;
    }
    final Image imageItem = Image.builder()
      .mediaObjectId(mediaobjectDTO.getId())
      .build();

    String textVal = "";
    imageItem.setMediaItemId(getOriginalMediaID(mediaobjectDTO.getId()));
    if (null != mediaobjectDTO.getAttrvals() && null != mediaobjectDTO.getAttrvals().getAttrval()) {
      final List<Attrval> mediaObjectAttrValList = mediaobjectDTO.getAttrvals().getAttrval();
      for (final Attrval mediaObjectAttrVal : mediaObjectAttrValList) {
        if (DESCRIPTION.equals(mediaObjectAttrVal.getUkey())) {
          textVal = mediaObjectAttrVal.getTextval();
        }
      }
    }

    final Mediaobject.Lobvalues.Lobvalue lobvalue = getLobval(mediaobjectDTO, lobValueTyp);

    enrichImageItem(urlBuilderUtil,mediaDomain,imageItem, lobvalue, textVal);

    return StringUtils.isBlank(imageItem.getFileName())
      ? null
      : imageItem;
  }

  /**
   * Set the values for the given {@link Image}
   *
   * @param urlBuilderUtil the {@link UrlBuilderUtil}
   * @param mediaDomain    the media domain
   * @param imageItem      the {@link Image}
   * @param lobvalue       the {@link Mediaobject.Lobvalues.Lobvalue}
   */
  private static void enrichImageItem(
    UrlBuilderUtil urlBuilderUtil,
    String mediaDomain,
    Image imageItem,
    Mediaobject.Lobvalues.Lobvalue lobvalue,
    String textVal
  ) {


    if (null != lobvalue && null != lobvalue.getLobval()) {
      final String lobValFilename = lobvalue.getLobval().getFname();
      if (StringUtils.isNotBlank(lobValFilename)) {
        imageItem.setUrl(
          urlBuilderUtil.getMediaObjectUrl(
            lobValFilename,
            mediaDomain
          )
        );
        imageItem.setFileName(lobValFilename);
      }
      final Mediaobject.Lobvalues.Lobvalue.Ltattrvals ltattrvals = lobvalue.getLtattrvals();
      ImageDimension dimension = getDimensionFromLobvalAttrval(ltattrvals);
      if(null != dimension){
        imageItem.setAspectRatio(parseAspectRatio(dimension).orElse(1.0));
        imageItem.setImageDimension(dimension);
      }
    }
    if(StringUtils.isBlank(imageItem.getTitle())) {
      imageItem.setTitle(textVal);
    }
  }

  private static Optional<Double> parseAspectRatio(final ImageDimension dimension) {
    try {
      int width = Integer.parseInt(dimension.getWidth());
      int height = Integer.parseInt(dimension.getHeight());

      if (height > 0) {
        return Optional.of(((double) width) / ((double) height));
      }
    } catch (Exception e) {
      log.error("could not parse dimensions from '{}'", dimension, e);
    }
    return Optional.empty();
  }

  private static ImageDimension getDimensionFromLobvalAttrval(Mediaobject.Lobvalues.Lobvalue.Ltattrvals ltattrvals) {
    if(ltattrvals != null && CollectionUtils.isNotEmpty(ltattrvals.getLtattrval())){
      List<Ltattrval> ltattrvalList = ltattrvals.getLtattrval();

      Ltattrval dimension = ltattrvalList.stream()
        .filter(ltattrval -> ltattrval.getCode().equals("dimension"))
        .findFirst()
        .orElse(null);

      if(dimension != null){
        // raw text format is: "901/900 px (76,3/76,2 mm)"
        String dimensionTextVal = dimension.getTextval();

        return Stream.of(dimensionTextVal)
          .map(s -> StringUtils.substringBefore(dimensionTextVal, " "))
          .map(s -> Arrays.asList(s.split("/")))
          .map(strings -> new ImageDimension(strings.getFirst(), strings.get(1)))
          .findFirst()
          .orElse(null);
      }
    }
    return null;
  }

  private static Mediaobject.Lobvalues.Lobvalue getLobval(
    final MediaobjectDTO mediaobjectDTO,
    final String lobValueTyp
  ) {
    if (mediaobjectDTO != null) {
      final Mediaobject.Lobvalues lobvalues = mediaobjectDTO.getLobvalues();
      if (lobvalues != null) {
        final List<Mediaobject.Lobvalues.Lobvalue> lobvalueList = lobvalues.getLobvalue();
        if (lobvalueList != null) {
          for (final Mediaobject.Lobvalues.Lobvalue lobValue : lobvalueList) {
            if (lobValueTyp.equals(lobValue.getLobtypeUkey())) {
              return lobValue;
            }
          }
        }
      }
    }
    return null;
  }

  private static Long getOriginalMediaID(final Long mediaSolrID) {
    if (mediaSolrID > MEDIAOBJECT_OFFSET) {
      return mediaSolrID - MEDIAOBJECT_OFFSET;
    } else {
      return mediaSolrID;
    }
  }

}
