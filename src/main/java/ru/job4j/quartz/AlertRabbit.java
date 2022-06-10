package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            Properties properties = getProperties();
            try (Connection connection = getConnection(properties)) {
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                JobDataMap dataMap = new JobDataMap();
                dataMap.put("connect", connection);
                JobDetail job = newJob(Rabbit.class).usingJobData(dataMap).build();
                int interval = Integer.parseInt(properties.getProperty("rabbit.interval"));
                SimpleScheduleBuilder times = simpleSchedule()
                        .withIntervalInSeconds(interval)
                        .repeatForever();
                Trigger trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(job, trigger);
                Thread.sleep(10000);
                scheduler.shutdown();
            }
        } catch (SchedulerException | IOException | InterruptedException
                 | SQLException | ClassNotFoundException se) {
            se.printStackTrace();
        }
    }

    private static Connection getConnection(Properties properties) throws IOException,
            SQLException, ClassNotFoundException {
        Class.forName(properties.getProperty("jdbc.driver"));
        return DriverManager.getConnection(
                properties.getProperty("jdbc.url"),
                properties.getProperty("jdbc.username"),
                properties.getProperty("jdbc.password"));
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            Connection connection = (Connection) context.getJobDetail().getJobDataMap().get("connect");
            try (PreparedStatement ps = connection
                    .prepareStatement("insert into rabbit(created_date) values (?);")) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Rabbit runs here ...");
        }
    }

    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = AlertRabbit.class
                .getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            properties.load(inputStream);
        }
        return properties;
    }
}