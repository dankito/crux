package com.chimbori.crux.articles;

import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Parsed result from web page containing important title, text and image.
 * <p>
 * All fields are public for performance reasons on Android.
 *
 * @link https://developer.android.com/training/articles/perf-tips.html#GettersSetters
 * <p>
 * Avoid Internal Getters/Setters
 * <p>
 * In native languages like C++ it's common practice to use getters (i = getCount()) instead of
 * accessing the field directly (i = mCount). This is an excellent habit for C++ and is often
 * practiced in other object oriented languages like C# and Java, because the compiler can usually
 * inline the access, and if you need to restrict or debug field access you can add the code at any
 * time.
 * <p>
 * However, this is a bad idea on Android. Virtual method calls are expensive, much more so than
 * instance field lookups. It's reasonable to follow common object-oriented programming practices
 * and have getters and setters in the public interface, but within a class you should always
 * access fields directly.
 * <p>
 * Without a JIT, direct field access is about 3x faster than invoking a trivial getter. With the
 * JIT (where direct field access is as cheap as accessing a local), direct field access is about
 * 7x faster than invoking a trivial getter.
 */
public class Article {
  public final String url;
  public final String originalUrl = "";

  public String title = "";
  public String description = "";
  public String siteName = "";
  public String themeColor = "";

  public String ampUrl = "";
  public String canonicalUrl = "";
  public String imageUrl = "";
  public String videoUrl = "";
  public String feedUrl = "";
  public String faviconUrl = "";

  /**
   * This is null when article could not get extracted.
   */
  public Document document;
  public Collection<String> keywords;
  public List<Image> images = new ArrayList<>();

  Article(String url) {
    // Package private constructor to disallow outside the library.
    this.url = url;
    this.canonicalUrl = url;  // Can be overridden later, but we start off by setting it to the URL itself.
  }

  @Override
  public String toString() {
    return "Article{" +
        "url='" + url + '\'' +
        ", title='" + title + '\'' +
        ", description='" + description + '\'' +
        ", siteName='" + siteName + '\'' +
        ", themeColor='" + themeColor + '\'' +
        ", ampUrl='" + ampUrl + '\'' +
        ", originalUrl='" + originalUrl + '\'' +
        ", canonicalUrl='" + canonicalUrl + '\'' +
        ", imageUrl='" + imageUrl + '\'' +
        ", videoUrl='" + videoUrl + '\'' +
        ", feedUrl='" + feedUrl + '\'' +
        ", faviconUrl='" + faviconUrl + '\'' +
        ", document=" + document +
        ", keywords=" + keywords +
        ", images=" + images +
        '}';
  }

}
