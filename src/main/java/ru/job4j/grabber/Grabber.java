package ru.job4j.grabber;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.model.Post;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Properties cfg = new Properties();
    private static final Logger LOG = LogManager.getLogger(Grabber.class.getName());

    public Store store() {
        return new PsqlStore(cfg);
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public void cfg() throws IOException {
        try (InputStream in = Grabber.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            cfg.load(in);
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class).usingJobData(data).build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(cfg.getProperty("time")))
                .repeatForever();
        Trigger trigger = newTrigger().startNow().withSchedule(times).build();
        scheduler.scheduleJob(job, trigger);
    }

    public static class GrabJob implements Job {
        Grabber grabber = new Grabber();
        @Override
        public void execute(JobExecutionContext context) {
            try {
                grabber.cfg();
            } catch (IOException e) {
                LOG.debug("Failed or aborted I/O operations", e);
            }
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");
            List<Post> list = parse.list(grabber.cfg.getProperty("link.habr"));
            list.forEach(post -> {
                try {
                    store.save(post);
                } catch (SQLException e) {
                    LOG.debug("Database access error or other errors", e);
                }
            });
        }
    }

    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(cfg.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (Post post : store.getAll()) {
                            String postStr = " ".repeat(5) + post.toString();
                            out.write(postStr.getBytes(Charset.forName("Windows-1251")));
                            out.write(System.lineSeparator().getBytes());
                            out.write("___".repeat(20).getBytes());
                            out.write(System.lineSeparator().repeat(2).getBytes());
                        }
                    } catch (IOException io) {
                        LOG.debug("Failed or aborted I/O operations", io);
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error", e);
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
        grab.web(store);
    }
}