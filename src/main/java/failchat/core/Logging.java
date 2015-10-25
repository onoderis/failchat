package failchat.core;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Logging {
    private static Logger logger;
    private static Formatter formatter;

    public static void configure() {
        logger = Logger.getLogger("failchat");
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        Handler cHandler = new ConsoleHandler();
        cHandler.setLevel(Level.FINE);
        formatter = new  FailChatLogFormatter();
        cHandler.setFormatter(formatter);
        logger.addHandler(cHandler);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                logger.log(Level.SEVERE, "Uncaught exception from thread " + thread.getClass().getName(), ex);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String logMessage = sw.toString();
                FileUtils.writeStringToFile(new File("err"), logMessage, true);
                logger.severe(logMessage);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        });
    }

    public static void configureLoggingInFile() {
        if (Configurator.config.getBoolean("log")) {
            try {
                Handler fileHandler = new FileHandler("log", true);
                fileHandler.setFormatter(formatter);
                fileHandler.setLevel(Level.ALL);
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Something goes wrong...", e);
            }
        }
    }

    static class FailChatLogFormatter extends Formatter {
        StringBuilder sb = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            sb.setLength(0);
            sb.append(dateFormat.format(new Date(record.getMillis()))).append(' ').append(record.getLevel())
                    .append(' ').append(record.getSourceClassName()).append(": ")
                    .append(record.getMessage()).append(lineSeparator);
            Throwable throwable = record.getThrown();
            if (throwable != null) {
                Writer writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter(writer);
                throwable.printStackTrace(printWriter);
                sb.append(writer.toString());
            }
            return sb.toString();
        }
    }

}
