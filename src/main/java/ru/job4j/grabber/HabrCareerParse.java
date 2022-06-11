package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.model.Post;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private static final int PAGES = 5;
    private final DateTimeParser dateTimeParser;
    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element elements = document.selectFirst(".style-ugc");
        return elements.wholeText();
    }

    public static void main(String[] args) throws IOException {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        habrCareerParse.list(PAGE_LINK).forEach(System.out::println);
    }

    Post getPostFromElement(Element element) throws IOException {
        Element titleElement = element.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        String link = SOURCE_LINK + linkElement.attr("href");
        String vacancyName = titleElement.text();
        Element titleElementDate = element.select(".vacancy-card__date").first();
        Element linkElementDate = titleElementDate.child(0);
        String description = retrieveDescription(link);
        String linkDate = linkElementDate.attr("datetime");
        LocalDateTime created = this.dateTimeParser.parse(linkDate);
        return new Post(vacancyName, link, description, created);
    }

    @Override
    public List<Post> list(String link) throws IOException {
        List<Post> list = new ArrayList();
        Post post = null;
        for (int index = 1; index <= PAGES; index++) {
            Connection connection = Jsoup.connect(link + "?page=" + index);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            for (Element element : rows) {
                post = getPostFromElement(element);
                list.add(post);
            }
        }
        return list;
    }
}
