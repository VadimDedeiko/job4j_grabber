package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {
    public static void main(String[] args) {
        try {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("connect", getConnection());
            JobDetail job = newJob(Rabbit.class).usingJobData(dataMap).build();
            int interval = Integer.parseInt(getProperties().getProperty("rabbit.interval"));
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
        } catch (SchedulerException | IOException | InterruptedException
                 | SQLException | ClassNotFoundException se) {
            se.printStackTrace();
        }
    }

    private static Connection getConnection() throws IOException, SQLException, ClassNotFoundException {
        Class.forName(getProperties().getProperty("jdbc.driver"));
        return DriverManager.getConnection(
                getProperties().getProperty("jdbc.url"),
                getProperties().getProperty("jdbc.username"),
                getProperties().getProperty("jdbc.password"));
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            try {
                getConnection()
                        .prepareStatement("insert into rabbit(created_date) values ('2020-12-01');")
                        .executeUpdate();
            } catch (SQLException | IOException | ClassNotFoundException e) {
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