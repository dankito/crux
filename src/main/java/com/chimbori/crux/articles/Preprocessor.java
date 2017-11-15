package com.chimbori.crux.articles;

import com.chimbori.crux.articles.model.PreprocessorOptions;
import com.chimbori.crux.common.Log;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * Performs basic sanitization before starting the extraction process.
 */
public class Preprocessor extends ProcessorBase {

  public void preprocess(Element bodyElement, PreprocessorOptions options) {
    Log.i("preprocess");

    if(options.isStripUnlikelyCandidates()) {
      stripUnlikelyCandidates(bodyElement);
    }

    if(options.isRemoveScriptsStylesForms()) {
      removeScriptsStylesForms(bodyElement);
    }

    if(options.isRemoveComments()) {
      removeComments(bodyElement);
    }
  }

  /**
   * Removes unlikely candidates from HTML. It often ends up removing more than just the unlikely
   * candidates, so exercise caution when enabling this.
   */
  protected void stripUnlikelyCandidates(Element element) {
    for(Element child : element.select("*")) {
      String classNameAndId = child.className().toLowerCase() + " " + child.id().toLowerCase();
      if(ExtractionHelpers.NEGATIVE_CSS_CLASSES_AND_IDS.matcher(classNameAndId).find() &&
          ExtractionHelpers.POSITIVE_CSS_CLASSES_AND_IDS.matcher(classNameAndId).find() == false && containsImage(child) == false) {
        Log.printAndRemove(child, "stripUnlikelyCandidates");
      }
    }
  }

  protected void removeScriptsStylesForms(Element element) {
    Elements scripts = element.getElementsByTag("script");
    for (Element item : scripts) {
      Log.printAndRemove(item, "removeScriptsStylesForms('script')");
    }

    Elements noscripts = element.getElementsByTag("noscript");
    for (Element item : noscripts) {
      if(item.select("img").size() > 0) { // keep images in noscript elements
        item.unwrap();
      }
      else {
        Log.printAndRemove(item, "removeScriptsStylesForms('noscript')");
      }
    }

    Elements styles = element.getElementsByTag("style");
    for (Element item : styles) {
      Log.printAndRemove(item, "removeScriptsStylesForms('style')");
    }

    Elements forms = element.getElementsByTag("form");
    for (Element item : forms) {
      Log.printAndRemove(item, "removeScriptsStylesForms('form')");
    }
  }

  protected void removeComments(Node node) {
    for (int i = 0; i < node.childNodes().size();) {
      Node child = node.childNode(i);
      if (child.nodeName().equals("#comment"))
        Log.printAndRemove(child, "removeComments");
      else {
        removeComments(child);
        i++;
      }
    }
  }
}
