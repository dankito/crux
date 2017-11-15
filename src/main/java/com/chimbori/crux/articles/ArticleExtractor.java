package com.chimbori.crux.articles;

import com.chimbori.crux.articles.model.PreprocessorOptions;
import com.chimbori.crux.common.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArticleExtractor {
  private final String url;
  private final Document document;
  private final Article article;

  public ArticleExtractor(String url, String html) {
    this.url = url;
    if (html.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.document = Jsoup.parse(html, url);
    this.article = new Article(this.url);
  }

  public ArticleExtractor(String url, Document document) {
    this.url = url;
    this.document = document;
    this.article = new Article(this.url);
  }

  public static ArticleExtractor with(String url, String html) {
    return new ArticleExtractor(url, html);
  }

  public ArticleExtractor extractMetadata() {
    article.title = MetadataHelpers.extractTitle(document);
    article.description = MetadataHelpers.extractDescription(document);
    article.siteName = MetadataHelpers.extractSiteName(document);
    article.themeColor = MetadataHelpers.extractThemeColor(document);
    article.canonicalUrl = StringUtils.makeAbsoluteUrl(article.url, MetadataHelpers.extractCanonicalUrl(document));
    article.ampUrl = StringUtils.makeAbsoluteUrl(article.url, MetadataHelpers.extractAmpUrl(document));
    article.feedUrl = StringUtils.makeAbsoluteUrl(article.url, MetadataHelpers.extractFeedUrl(document));
    article.videoUrl = StringUtils.makeAbsoluteUrl(article.url, MetadataHelpers.extractVideoUrl(document));
    article.faviconUrl = StringUtils.makeAbsoluteUrl(article.url, MetadataHelpers.extractFaviconUrl(document));
    article.keywords = MetadataHelpers.extractKeywords(document);
    return this;
  }

  public ArticleExtractor extractContent() {
    PreprocessHelpers.preprocess(document, new PreprocessorOptions());

    Collection<Element> nodes = ExtractionHelpers.getNodes(document);
    int maxWeight = 0;
    Element bestMatchElement = null;
    List<Element> highRankedElements = new ArrayList<>();

    for (Element element : nodes) {
      int currentWeight = ExtractionHelpers.getWeight(element);
      if(currentWeight > 50) {
        highRankedElements.add(element);
      }

      if (currentWeight > maxWeight) {
        maxWeight = currentWeight;
        bestMatchElement = element;
        if (maxWeight > 200) {
          break;
        }
      }
    }

    // if a lot of high ranked elements have the same parent, then the parent is the node to use as it comprises that high ranked ones
    bestMatchElement = checkIfHighRankedElementsHaveSameParent(bestMatchElement, highRankedElements);

    // Extract images before post-processing, because that step may remove images.
    if(bestMatchElement != null) {
      article.images = ImageHelpers.extractImages(bestMatchElement);
      article.document = PostprocessHelpers.postprocess(bestMatchElement);
    }
    article.imageUrl = StringUtils.makeAbsoluteUrl(article.url, MetadataHelpers.extractImageUrl(document, article.images));
    return this;
  }

  private Element checkIfHighRankedElementsHaveSameParent(Element bestMatchElement, List<Element> highRankedElements) {
    if(bestMatchElement != null && highRankedElements.size() > 2) {
      Element parent = bestMatchElement.parent();
      List<Element> elementsWithSameParent = new ArrayList<>();

      for(Element element : highRankedElements) {
        if(element.parent() == parent) {
          elementsWithSameParent.add(element);
        }
      }

      if(elementsWithSameParent.size() > 1) {
        return parent;
      }
    }

    return bestMatchElement;
  }

  public Article article() {
    return article;
  }

  public String url() {
    return url;
  }
}
