package com.auction.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class AuctionSocketClient implements AutoCloseable {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private Thread listenerThread;
    private volatile boolean running;

    private Consumer<Integer> updateListener;
    private Consumer<SocketResult> resultListener;
    private Consumer<String> statusListener;

    public AuctionSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            running = true;
            listenerThread = new Thread(this::listenLoop, "auction-socket-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;
        } catch (IOException e) {
            notifyStatus("Khong ket noi duoc socket server " + host + ":" + port + ". App se dung DB fallback.");
            return false;
        }
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void setUpdateListener(Consumer<Integer> updateListener) {
        this.updateListener = updateListener;
    }

    public void setResultListener(Consumer<SocketResult> resultListener) {
        this.resultListener = resultListener;
    }

    public void setStatusListener(Consumer<String> statusListener) {
        this.statusListener = statusListener;
    }

    public void sendBid(int auctionId, String username, BigDecimal amount) {
        send("BID|" + auctionId + "|" + encode(username) + "|" + encode(amount.toPlainString()));
    }

    public void sendAutoBid(int auctionId, String username, BigDecimal maxBid, BigDecimal increment) {
        send("AUTOBID|" + auctionId + "|" + encode(username) + "|" + encode(maxBid.toPlainString()) + "|" + encode(increment.toPlainString()));
    }

    private void send(String message) {
        if (!isConnected() || out == null) {
            throw new IllegalStateException("Chua ket noi socket server");
        }
        out.println(message);
    }

    private void listenLoop() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = in.readLine()) != null) {
                handleLine(line.trim());
            }
        } catch (Exception e) {
            notifyStatus("Mat ket noi socket server: " + e.getMessage());
        } finally {
            running = false;
            closeQuietly();
        }
    }

    private void handleLine(String line) {
        if (line.isBlank()) {
            return;
        }
        String[] parts = line.split("\\|", -1);
        String command = parts[0];
        if ("UPDATE".equals(command) && parts.length >= 2) {
            Consumer<Integer> listener = updateListener;
            if (listener != null) {
                listener.accept(Integer.parseInt(parts[1]));
            }
        } else if ("RESULT".equals(command) && parts.length >= 4) {
            Consumer<SocketResult> listener = resultListener;
            if (listener != null) {
                boolean success = "OK".equalsIgnoreCase(parts[1]);
                listener.accept(new SocketResult(success, decode(parts[2]), Integer.parseInt(parts[3])));
            }
        } else if ("CONNECTED".equals(command)) {
            notifyStatus(parts.length >= 2 ? decode(parts[1]) : "Da ket noi socket server");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private void notifyStatus(String message) {
        Consumer<String> listener = statusListener;
        if (listener != null) {
            listener.accept(message);
        }
    }

    @Override
    public void close() {
        running = false;
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public record SocketResult(boolean success, String message, int auctionId) {
    }
}
