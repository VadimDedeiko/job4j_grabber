package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    public String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element elements = document.selectFirst(".style-ugc");
        return elements.wholeText();
    }

    public static void main(String[] args) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 1; index <= 5; index++) {
            Connection connection = Jsoup.connect(PAGE_LINK + "?page=" + index);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                Element titleElementDate = row.select(".vacancy-card__date").first();
                Element linkElementDate = titleElementDate.child(0);
                String linkDate = linkElementDate.attr("datetime");
                String rsl = String.format("%s %s %s%n", vacancyName, link, linkDate);
                stringBuilder.append(rsl);
            });
        }
        System.out.printf(stringBuilder.toString());
    }
}
