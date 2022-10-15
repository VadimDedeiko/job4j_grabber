package ru.job4j.grabber.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Post {
    private int id;
    private final String title;
    private final String link;
    private final String description;
    private final LocalDateTime created;

    public Post(String title, String link, String description, LocalDateTime created) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.created = created;
    }

    public Post(int id, String title, String link, String description, LocalDateTime created) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.description = description;
        this.created = created;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return "POST №" + id
                + "," + System.lineSeparator() + "TITLE = '" + title + '\''
                + "," + System.lineSeparator() + "LINK = '" + link + '\''
                + "," + System.lineSeparator() + "DESCRIPTION = '" + description + '\''
                + "," + System.lineSeparator() + "CREATED = " + created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Post post = (Post) o;
        return id == post.id && Objects.equals(link, post.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, link);
    }
}
