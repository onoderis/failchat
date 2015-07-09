package failchat.core;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String logMessage = sw.toString();
                FileUtils.writeStringToFile(new File("err"), logMessage, true);
                logger.severe(logMessage);
            } catch (IOException e) {
                e.printStackTrace();
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
                e.printStackTrace();
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
            return sb.toString();
        }
    }

}
