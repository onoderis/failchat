package failchat.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Logger {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("FailChat l");

    public static void configure() {
        logger.setUseParentHandlers(false);
        Handler handler = new ConsoleHandler();
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
        handler.setLevel(Level.ALL);
        handler.setFormatter(new FailChatLogFormatter());
//        l.log(Level.SEVERE, "fuuu");
//        l.log(Level.WARNING, "test log");
//        l.log(Level.INFO, "test log");
//        l.log(Level.CONFIG, "test log");
//        l.log(Level.FINE, "test log 2");
//        l.log(Level.FINER, "finest");
//        l.log(Level.FINEST, "finest");
    }

    public static void log(Level level, String msg) {
        logger.log(level, msg);
    }

    public static void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    public static void warning(String msg) {
        log(Level.WARNING, msg);
    }

    public static void info(String msg) {
        log(Level.INFO, msg);
    }

    public static void config(String msg) {
        log(Level.CONFIG, msg);
    }

    public static void fine(String msg) {
        log(Level.FINE, msg);
    }

    public static void finer(String msg) {
        log(Level.FINER, msg);
    }

    public static void finest(String msg) {
        log(Level.FINEST, msg);
    }


    static class FailChatLogFormatter extends Formatter {

        StringBuilder sb = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        @Override
        public String format(LogRecord record) {
            sb.setLength(0);
            sb.append(dateFormat.format(new Date(record.getMillis()))).append(' ').append(record.getLevel()).append(": ")
                    .append(record.getMessage()).append(lineSeparator);
            return sb.toString();
        }
    }

}
