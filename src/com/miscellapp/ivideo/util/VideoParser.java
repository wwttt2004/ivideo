package com.miscellapp.ivideo.util;

import com.miscellapp.ivideo.model.Video;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chenjishi on 13-12-28.
 */
public class VideoParser {
    private static final String URL_56 = "http://fun.56.com/";
    private static final String URL_YOUKU = "http://fun.youku.com/";

    public static ArrayList<Video> parse56Video(String html) {
        Document doc = Jsoup.parse(html);
        if (null == doc) return null;

        ArrayList<Video> videoList = new ArrayList<Video>();
        Pattern p = Pattern.compile("(v_|vid-)(\\w+)\\.html");

        Elements topList = doc.select("ul.js_switch_ct");
        if (null != topList && topList.size() > 0) {
            Elements links = topList.get(0).select("a");

            for (Element link : links) {
                String href = link.attr("href");
                if (href.endsWith("html")) {
                    Video video = new Video();
                    video.url = href;
                    Matcher m = p.matcher(href);
                    while (m.find()) {
                        video.id = m.group(2);
                    }
                    video.title = link.attr("title");
                    Element img = link.select("img").first();
                    video.thumbUrl = img.attr("src");

                    videoList.add(video);
                }
            }
        }

        Elements rankList = doc.select("ul.mod56_video_rank_list");
        if (null != rankList && rankList.size() > 0) {
            Elements items = rankList.get(0).getElementsByTag("li");
            for (Element li : items) {
                Video video = new Video();
                Element rankImg = li.select("a.rank_img").first();
                video.url = rankImg.attr("href");
                Matcher m = p.matcher(video.url);
                while (m.find()) {
                    video.id = m.group(2);
                }
                video.thumbUrl = rankImg.select("img").first().attr("src");

                Element rankTitle = li.select("a.rank_title").first();
                video.title = rankTitle.text();

                videoList.add(video);
            }
        }

        Elements hotList = doc.select("div.tv_hot_H");
        if (null != hotList && hotList.size() > 0) {
            Elements links = hotList.get(0).select("a.m_v_list_cover");
            for (Element link : links) {
                Video video = new Video();

                video.url = link.attr("href");
                Matcher m = p.matcher(video.url);
                while (m.find()) {
                    video.id = m.group(2);
                }
                video.title = link.attr("title");
                video.thumbUrl = link.select("img").first().attr("src");

                videoList.add(video);
            }
        }

        Elements fashionList = doc.select("div.fashion_bd");
        if (null != fashionList && fashionList.size() > 0) {
            Elements links = fashionList.get(0).select("a.pic");
            for (Element link : links) {
                Video video = new Video();

                video.url = link.attr("href");
                Matcher m = p.matcher(video.url);
                while (m.find()) {
                    video.id = m.group(2);
                }
                video.title = link.attr("title");
                video.thumbUrl = link.select("img").first().attr("src");

                videoList.add(video);
            }
        }

        return videoList;
    }

    public static ArrayList<Video> parseYouKuVideo(String html) {
        Document doc = Jsoup.parse(html);
        if (null == doc) return null;

        ArrayList<Video> videoList = new ArrayList<Video>();
        ArrayList<Video> tempList = null;

        String[] ids = new String[]{"m_88536", "m_89545", "m_90718", "m_89996"};

        for (int i = 0; i < ids.length; i++) {
            Element list1 = doc.getElementById(ids[i]);
            tempList = getYouKuList(list1);
            if (null != tempList && tempList.size() > 0) {
                videoList.addAll(tempList);
            }
        }

        return videoList;
    }

    private static ArrayList<Video> getYouKuList(Element element) {
        if (null == element) return null;

        ArrayList<Video> videoList = new ArrayList<Video>();
        Pattern p = Pattern.compile("id_(\\w+)\\.html");

        Elements items = element.getElementsByClass("v");
        for (Element item : items) {
            Video video = new Video();
            Elements thumb = item.getElementsByClass("v-thumb");
            if (null != thumb && thumb.size() > 0) {
                video.thumbUrl = thumb.get(0).select("img").get(0).attr("src");
            }

            Elements link = item.getElementsByClass("v-link");
            if (null != link && link.size() > 0) {
                Element vLink = link.get(0).select("a").get(0);
                String href = vLink.attr("href");
                video.url = href;
                Matcher m = p.matcher(href);
                while (m.find()) {
                    video.id = m.group(1);
                }
                video.title = vLink.attr("title");
            }

            videoList.add(video);
        }

        return videoList;
    }
}
