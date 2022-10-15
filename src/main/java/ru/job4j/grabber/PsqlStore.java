package ru.job4j.grabber;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.job4j.grabber.model.Post;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {
    private Connection cnn;
    private static final Logger LOG = LogManager.getLogger(PsqlStore.class.getName());

    public static void main(String[] args) throws Exception {
        Properties cfg = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            cfg.load(in);
        } catch (IOException e) {
            LOG.debug("Failed or aborted I/O operations", e);
        }
        try (PsqlStore psqlStore = new PsqlStore(cfg)) {
            DateTimeParser dateTimeParser = new HabrCareerDateTimeParser();
            HabrCareerParse parse = new HabrCareerParse(dateTimeParser);
            List<Post> list = parse.list(cfg.getProperty("link.habr"));
            list.forEach(psqlStore::save);
            List<Post> listGetAll = psqlStore.getAll();
            listGetAll.forEach(System.out::println);
            System.out.println(psqlStore.findById(3));
        }
    }

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(
                    cfg.getProperty("jdbc.url"),
                    cfg.getProperty("jdbc.username"),
                    cfg.getProperty("jdbc.password")
            );
        } catch (Exception e) {
            LOG.error("Error", e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement ps = cnn.prepareStatement(
                "INSERT INTO post(name,description,link,created) values (?,?,?,?) ON CONFLICT (link) DO NOTHING;"
        )) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getDescription());
            ps.setString(3, post.getLink());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.debug("Database access error or other errors", e);
        }
    }

    @Override
    public List<Post> getAll() {
        Post post;
        List<Post> postList = new ArrayList<>();
        try (PreparedStatement ps = cnn.prepareStatement(
                "select * from post;"
        )) {
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                post = setPost(resultSet);
                postList.add(post);
            }
        } catch (SQLException e) {
            LOG.debug("Database access error or other errors", e);
        }
        return postList;
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement ps =
                     cnn.prepareStatement("select * from post where id = ?;")) {
            ps.setInt(1, id);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                post = setPost(resultSet);
            }
        } catch (SQLException e) {
            LOG.debug("Database access error or other errors", e);
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    private Post setPost(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created").toLocalDateTime()
        );
    }
}