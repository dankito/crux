package com.chimbori.crux.articles;

import com.chimbori.crux.common.Log;
import com.chimbori.crux.common.StringUtils;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cleans up the best-match Element after one has been picked, in order to provide a sanitized
 * output tree to the caller.
 */
class PostprocessHelpers {
  /**
   * If a string is shorter than this limit, it is not considered a paragraph.
   */
  private static final int MIN_LENGTH_FOR_PARAGRAPHS = 50;

  private static final Pattern UNLIKELY_CSS_STYLES = Pattern.compile("display\\:none|visibility\\:hidden");

  /**
   * Tags that should not be output, but still may contain interesting content.
   */
  private static final Set<String> REMOVE_TAGS_BUT_RETAIN_CONTENT = new HashSet<>(Arrays.asList(
      "font", "table", "tbody", "tr", "td", "div", "ol", "ul", "li", "span"
  ));

  /**
   * Tags that should be retained in the output. This list should be fairly minimal, and equivalent
   * to the list of tags that callers can be expected to be able to handle.
   */
  private static final Set<String> RETAIN_TAGS = new HashSet<>(Arrays.asList(
      "p", "b", "i", "u", "strong", "em", "a", "pre", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "img", "br"
  ));

  /**
   * Tags that can contain really short content, because they are not paragraph-level tags. Content
   * within these tags is not subject to the {@code MIN_LENGTH_FOR_PARAGRAPHS} requirement.
   */
  private static final Set<String> TAGS_EXEMPT_FROM_MIN_LENGTH_CHECK = new HashSet<>(Arrays.asList(
      "b", "i", "u", "strong", "em", "a", "pre", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote"
  ));

  /**
   * The whitelist of attributes that should be retained in the output. No other attributes
   * will be retained.
   */
  private static final Set<String> ATTRIBUTES_TO_RETAIN_IN_HTML = new HashSet<>(Arrays.asList(
      "href", "title", "src", "alt", "width", "height", "cellspacing", "cellpadding"
  ));

  /**
   * After a final set of top-level nodes has been extracted, all tags except these are removed.
   * This ensures that while inline tags containing shorter text, e.g. <a href="…">one word</a>
   * are kept as part of a larger paragraph, those same short tags are not allowed to be
   * top-level children.
   */
  private static final Set<String> RETAIN_TAGS_TOP_LEVEL = new HashSet<>(Arrays.asList(
      "p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "li"
  ));

  static Document postprocess(Element topNode) {
    Log.i("postprocess");
    Document doc = new Document("");
    if (topNode == null) {
      return doc;
    }

    removeNodesWithNegativeScores(topNode);
    replaceLineBreaksWithSpaces(topNode);
    removeUnlikelyChildNodes(topNode);
    removeTagsButRetainContent(topNode);
    removeTagsNotLikelyToBeParagraphs(topNode);
    removeTopLevelTagsNotLikelyToBeParagraphs(topNode);
    removeShortParagraphs(topNode);
    removeDisallowedAttributes(topNode);
    makeUrlsAbsolute(topNode);

    for (Node node : topNode.childNodes()) {
      doc.appendChild(node.clone());  // TODO: Don’t copy each item separately.
    }
    return doc;
  }

  private static void replaceLineBreaksWithSpaces(Element topNode) {
    for (Element brNextToBrElement : topNode.select("br + br")) {
      brNextToBrElement.remove();
    }
    // TODO: what were the • that good for? Introduces ugly • while removing senseful <br>s
    for (Element brElement : topNode.select("br")) {
      if (brElement.previousSibling() != null) {
        brElement.previousSibling().after(" ");
      } else {
        brElement.parent().append(" ");
      }
    }
  }

  private static void removeTopLevelTagsNotLikelyToBeParagraphs(Element element) {
    for (Element childElement : element.children()) {
      if (!RETAIN_TAGS_TOP_LEVEL.contains(childElement.tagName())) {
        Log.printAndRemove(childElement, "removeTopLevelTagsNotLikelyToBeParagraphs");
      }
    }
  }

  private static void removeTagsNotLikelyToBeParagraphs(Element element) {
    for (Element childElement : element.children()) {
      if (!RETAIN_TAGS.contains(childElement.tagName())) {
        Log.printAndRemove(childElement, "removeTagsNotLikelyToBeParagraphs");
      } else if (childElement.children().size() > 0) {
        removeTagsNotLikelyToBeParagraphs(childElement);
      }
    }
  }

  private static void removeTagsButRetainContent(Element element) {
    for (Element childElement : element.children()) {
      removeTagsButRetainContent(childElement);
      if (REMOVE_TAGS_BUT_RETAIN_CONTENT.contains(childElement.tagName())) {
        Log.i("removeTagsButRetainContent: [%s] %s", childElement.tagName(), childElement.outerHtml());
        childElement.tagName("p");  // Set the wrapper tag to <p> instead of unwrapping them.
      }
    }
  }

  private static void removeShortParagraphs(Element topNode) {
    for (int i = topNode.childNodeSize() - 1; i >= 0; i--) {
      Node childNode = topNode.childNode(i);

      String text = null;
      boolean isExemptFromMinTextLengthCheck = false;
      if (childNode instanceof TextNode) {
        text = ((TextNode) childNode).text().trim();

      } else if (childNode instanceof Element) {
        Element childElement = (Element) childNode;
        text = childElement.text().trim();
        isExemptFromMinTextLengthCheck = TAGS_EXEMPT_FROM_MIN_LENGTH_CHECK.contains(childElement.tagName());
      }

      Log.i("removeShortParagraphs: [%s] isExemptFromMinTextLengthCheck : %b", childNode, isExemptFromMinTextLengthCheck);

      if (text == null ||
          text.isEmpty() ||
          (!isExemptFromMinTextLengthCheck && text.length() < MIN_LENGTH_FOR_PARAGRAPHS) ||
          text.length() > StringUtils.countLetters(text) * 2) {
        Log.printAndRemove(childNode, "removeShortParagraphs:");
      }
    }
  }

  private static void removeUnlikelyChildNodes(Element element) {
    for (Element childElement : element.children()) {
      if (isUnlikely(childElement)) {
        Log.printAndRemove(childElement, "removeUnlikelyChildNodes");
      } else if (childElement.children().size() > 0) {
        removeUnlikelyChildNodes(childElement);
      }
    }
  }

  static private void removeNodesWithNegativeScores(Element topNode) {
    Elements elementsWithGravityScore = topNode.select(ExtractionHelpers.GRAVITY_SCORE_SELECTOR);
    for (Element element : elementsWithGravityScore) {
      int score = Integer.parseInt(element.attr(ExtractionHelpers.GRAVITY_SCORE_ATTRIBUTE));
      if (score < 0 || (element.text().length() < MIN_LENGTH_FOR_PARAGRAPHS && isHeading(element) == false && isImage(element) == false)) { // keep headings and images
        Log.printAndRemove(element, "removeNodesWithNegativeScores");
      }
    }
  }

  private static boolean isHeading(Element element) {
    return element.tagName().toLowerCase().startsWith("h") && element.tagName().length() == 2;
  }

  private static boolean isImage(Element element) {
    Element imageElement = element.select("img").first();
    if(imageElement != null) {
      return isSmallImage(imageElement) == false;
    }

    return false;
  }

  private static boolean isSmallImage(Element image) {
    try {
      if(image.attr("width").length() > 0) {
        int width = Integer.parseInt(image.attr("width"));
        if(width < 20) {
          return true;
        }
      }

      if(image.attr("height").length() > 0) {
        int height = Integer.parseInt(image.attr("height"));
        if(height < 20) {
          return true;
        }
      }
    } catch(Exception ignored) { }

    return false;
  }


  static private boolean isUnlikely(Element element) {
    String styleAttribute = element.attr("style");
    String classAttribute = element.attr("class");
    // TODO: why have captions been removed? keeping them for now.
    return UNLIKELY_CSS_STYLES.matcher(styleAttribute).find()
        || UNLIKELY_CSS_STYLES.matcher(classAttribute).find();
  }

  private static void removeDisallowedAttributes(Element node) {
    for (Element childElement : node.children()) {
      removeDisallowedAttributes(childElement);
    }

    for (Attribute attribute : node.attributes().asList()) { // make a copy otherwise it will throw a ConcurrentModificationException
      if (!ATTRIBUTES_TO_RETAIN_IN_HTML.contains(attribute.getKey())) {
        node.removeAttr(attribute.getKey());
      }
    }
  }


  private static void makeUrlsAbsolute(Element element) {
    for(Element hrefElement : element.select("[href]")) {
        hrefElement.attr("href", makeUrlAbsolute(hrefElement.attr("href"), element.baseUri()));
    }

    for(Element srcElement : element.select("[src]")) {
        srcElement.attr("src", makeUrlAbsolute(srcElement.attr("src"), element.baseUri()));
    }
  }


  private static void makeImageSourceAbsolute(Element imageElement) {
    String absoluteUrl = makeUrlAbsolute(imageElement.attr("src"), imageElement.baseUri());
    imageElement.attr("src", absoluteUrl);
  }

  private static String makeUrlAbsolute(String url, String siteUrl) {
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

  private static String tryToMakeUrlAbsolute(String relativeUrl, String siteUrl) {
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
