package com.auction.realtime;

import java.math.BigDecimal;

public class RealtimeBidMessage {

    private String auctionId;
    private String auctionTitle;
    private String bidderName;
    private BigDecimal amount;
    private long timestamp;
    private String sourceClientId;

    public RealtimeBidMessage(
            String auctionId,
            String auctionTitle,
            String bidderName,
            BigDecimal amount,
            long timestamp,
            String sourceClientId
    ) {
        this.auctionId = auctionId;
        this.auctionTitle = auctionTitle;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.sourceClientId = sourceClientId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getAuctionTitle() {
        return auctionTitle;
    }

    public String getBidderName() {
        return bidderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSourceClientId() {
        return sourceClientId;
    }

    public String toJson() {
        return "{"
                + "\"auctionId\":\"" + escape(auctionId) + "\","
                + "\"auctionTitle\":\"" + escape(auctionTitle) + "\","
                + "\"bidderName\":\"" + escape(bidderName) + "\","
                + "\"amount\":\"" + amount + "\","
                + "\"timestamp\":" + timestamp + ","
                + "\"sourceClientId\":\"" + escape(sourceClientId) + "\""
                + "}";
    }

    public static RealtimeBidMessage fromJson(String json) {
        String auctionId = getString(json, "auctionId");
        String auctionTitle = getString(json, "auctionTitle");
        String bidderName = getString(json, "bidderName");
        String amountText = getString(json, "amount");
        String sourceClientId = getString(json, "sourceClientId");
        long timestamp = getLong(json, "timestamp");

        return new RealtimeBidMessage(
                auctionId,
                auctionTitle,
                bidderName,
                new BigDecimal(amountText),
                timestamp,
                sourceClientId
        );
    }

    private static String getString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);

        if (start < 0) {
            return "";
        }

        start += pattern.length();

        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (int i = start; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaping) {
                result.append(current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                break;
            } else {
                result.append(current);
            }
        }

        return result.toString();
    }

    private static long getLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);

        if (start < 0) {
            return 0L;
        }

        start += pattern.length();

        int end = start;
        while (end < json.length()) {
            char current = json.charAt(end);

            if (!Character.isDigit(current)) {
                break;
            }

            end++;
        }

        return Long.parseLong(json.substring(start, end));
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
