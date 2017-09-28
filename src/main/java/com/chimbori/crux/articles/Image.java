package com.chimbori.crux.articles;

import com.chimbori.crux.common.StringUtils;

import org.jsoup.nodes.Element;


/**
 * Class which encapsulates the data from an image found under an element
 */
public class Image {
    public String src;
    public int weight;
    public String title;
    public int height;
    public int width;
    public String alt;
    public boolean noFollow;
    public Element element;

    private Image() {
    }

    static Image from(Element imgElement) {
        Image image = new Image();
        image.element = imgElement;
        // Some sites use data-src to load images lazily, so prefer the data-src attribute if it exists.
        image.src = !imgElement.attr("data-src").isEmpty() ? imgElement.attr("data-src") : imgElement.attr("src");
        image.width = StringUtils.parseAttrAsInt(imgElement, "width");
        image.height = StringUtils.parseAttrAsInt(imgElement, "height");
        image.alt = imgElement.attr("alt");
        image.title = imgElement.attr("title");
        image.noFollow = imgElement.parent() != null && imgElement.parent().attr("rel") != null && imgElement.parent().attr("rel").contains("nofollow");
        return image;
    }

    @Override
    public String toString() {
        return "Image{" +
            "src='" + src + '\'' +
            ", weight=" + weight +
            ", title='" + title + '\'' +
            ", height=" + height +
            ", width=" + width +
            ", alt='" + alt + '\'' +
            ", noFollow=" + noFollow +
            ", element=" + element +
            '}';
    }
}
