package com.boredintern.lead_discovery.collector;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RunLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    public void info(String message) {
        logs.add(String.format("[%s] [INFO] %s", FORMATTER.format(Instant.now()), message));
    }

    public void warn(String message) {
        logs.add(String.format("[%s] [WARN] %s", FORMATTER.format(Instant.now()), message));
    }

    public void error(String message, Throwable t) {
        String detail = t != null ? " - " + t.getClass().getSimpleName() + ": " + t.getMessage() : "";
        logs.add(String.format("[%s] [ERROR] %s%s", FORMATTER.format(Instant.now()), message, detail));
    }

    public String getFormattedLogs() {
        return String.join("\n", logs);
    }
}
