package failchat.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Logging {

    public static void configure() {
        Logger logger = Logger.getLogger("failchat");
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new FailChatLogFormatter());
        logger.addHandler(handler);
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
