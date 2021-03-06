package com.chimbori.crux.articles;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.net.URI;

/**
 * Contains common utils for Preprocessor and Postprocessor
 */
public abstract class ProcessorBase {


  protected boolean shouldKeepShortParagraph(Node node) {
    if(node instanceof Element) {
      Element childElement = (Element) node;
      String classNameAndId = childElement.className() + " " + childElement.id();

      if(containsImage(childElement) ||
          ExtractionHelpers.POSITIVE_CSS_CLASSES_AND_IDS.matcher(classNameAndId).find() &&
          ExtractionHelpers.UNLIKELY_CSS_CLASSES_AND_IDS.matcher(classNameAndId).find() == false &&
          ExtractionHelpers.NEGATIVE_CSS_CLASSES_AND_IDS.matcher(classNameAndId).find() == false) {
        return true;
      }
    }

    return false;
  }

  protected boolean shouldKeepElement(Element element) {
    return "td".equals(element.tagName()) || containsHeading(element) || containsImage(element) ||
            ExtractionHelpers.POSITIVE_CSS_CLASSES_AND_IDS.matcher(element.className() + " " + element.id()).find();
  }

  protected boolean containsHeading(Element element) {
    return element.select("h1, h2, h3, h4, h5, h6").size() > 0;
  }

  protected boolean containsImage(Element element) {
    Element imageElement = element.select("img").first();
    if(imageElement != null) {
      return isSmallImage(imageElement) == false;
    }

    return false;
  }

  protected boolean isSmallImage(Element image) {
    try {
      if(image.attr("width").length() > 0) {
        int width = Integer.parseInt(image.attr("width"));
        if(width < 33) {
          return true;
        }
      }

      if(image.attr("height").length() > 0) {
        int height = Integer.parseInt(image.attr("height"));
        if(height < 33) {
          return true;
        }
      }
    } catch(Exception ignored) { }

    return false;
  }



  protected void makeUrlsAbsolute(Element element) {
    String baseUri = element.baseUri();

    for(Element hrefElement : element.select("[href]")) {
        hrefElement.attr("href", makeUrlAbsolute(hrefElement.attr("href"), baseUri));
    }

    for(Element srcElement : element.select("[src]")) {
        srcElement.attr("src", makeUrlAbsolute(srcElement.attr("src"), baseUri));

      makeSourceSetUrlsAbsolute(srcElement, baseUri);
    }
  }

  protected void makeSourceSetUrlsAbsolute(Element element, String baseUri) {
    String sourceSet = element.attr("srcset");
    if(sourceSet.isEmpty() == false) {
      element.attr("srcset", makeSourceSetUrlsAbsolute(sourceSet, baseUri));
    }
  }

  protected String makeSourceSetUrlsAbsolute(String sourceSetValue, String baseUri) {
    String[] urls = sourceSetValue.split(",");

    for(int i = 0; i < urls.length; i++) {
      urls[i] = makeUrlAbsolute(urls[i].trim(), baseUri);
    }

    String newSourceSetValue = "";
    for(int i = 0; i < urls.length; i++) {
      newSourceSetValue += urls[i];

      if(i < urls.length - 1) {
        newSourceSetValue += ", ";
      }
    }

    return newSourceSetValue;
  }

  protected String makeUrlAbsolute(String url, String siteUrl) {
    String absoluteUrl = url;

    if(url.startsWith("//")) {
      if(siteUrl.startsWith("https:")) {
        absoluteUrl = "https:" + url;
      }
      else {
        absoluteUrl = "http:" + url;
      }
    }
    else if(url.startsWith("/") || url.startsWith("http") == false) {
      String result = tryToMakeUrlAbsolute(url, siteUrl);
      if(result != null) {
        absoluteUrl = result;
      }
    }

    return absoluteUrl;
  }

  protected String tryToMakeUrlAbsolute(String relativeUrl, String siteUrl) {
    try {
      URI relativeUri = new URI(relativeUrl);
      if(relativeUri.isAbsolute() && relativeUri.getScheme().startsWith("http") == false) {
        return relativeUrl; // it's an absolute uri but just doesn't start with http, e.g. mailto: for file:
      }
    } catch(Exception ignored) { }

    try {
      URI uri = new URI(siteUrl);
      return uri.resolve(relativeUrl).toString();
    } catch(Exception ignored) { }

    try {
      URI uri = new URI(siteUrl);

      String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
      String separator = relativeUrl.startsWith("/") ? "" : "/";

      String manuallyCreatedUriString = uri.getScheme() + "://" + uri.getHost() + port + separator + relativeUrl;
      URI manuallyCreatedUri = new URI(manuallyCreatedUriString);
      return manuallyCreatedUri.toString();
    } catch(Exception ignored) { }

    return null;
  }

}
