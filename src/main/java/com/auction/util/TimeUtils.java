package com.auction.util;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public final class TimeUtils {
    public static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private TimeUtils() {
    }

    public static String format(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(DEFAULT_FORMATTER);
    }

    public static long secondsBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Không xác định được thời gian");
        }
        return Duration.between(from, to).getSeconds();
    }

    public static boolean isExpired(LocalDateTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("Không xác định được thời gian kết thúc");
        }
        return LocalDateTime.now().isAfter(endTime);
    }

    public static boolean isInLastSeconds(LocalDateTime endTime, long thresholdSeconds) {
        if (endTime == null) {
            throw new IllegalArgumentException("Không xác định được thời gian kết thúc");
        }
        long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return secondsLeft >= 0 && secondsLeft <= thresholdSeconds;
    }

    public static LocalDateTime extend(LocalDateTime time, long seconds) {
        if (time == null) {
            throw new IllegalArgumentException("Không xác định được thời gian");
        }
        return time.plusSeconds(seconds);
    }
}