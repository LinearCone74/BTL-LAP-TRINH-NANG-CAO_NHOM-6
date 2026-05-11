package com.auction.realtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.function.Consumer;

public class RealtimeBidClient {

    private final String host;
    private final int port;
    private final String clientId;
    private final Consumer<RealtimeBidMessage> messageHandler;

    private Socket socket;
    private PrintWriter writer;

    public RealtimeBidClient(
            String host,
            int port,
            Consumer<RealtimeBidMessage> messageHandler
    ) {
        this.host = host;
        this.port = port;
        this.clientId = UUID.randomUUID().toString();
        this.messageHandler = messageHandler;
    }

    public String getClientId() {
        return clientId;
    }

    public void connect() {
        Thread connectThread = new Thread(this::connectInternal);
        connectThread.setDaemon(true);
        connectThread.setName("realtime-bid-client");
        connectThread.start();
    }

    private void connectInternal() {
        try {
            socket = new Socket(host, port);
            writer = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            String line;

            while ((line = reader.readLine()) != null) {
                RealtimeBidMessage message = RealtimeBidMessage.fromJson(line);

                if (!clientId.equals(message.getSourceClientId())) {
                    messageHandler.accept(message);
                }
            }

        } catch (IOException exception) {
            System.out.println("RealtimeBidClient disconnected: " + exception.getMessage());
        }
    }

    public void send(RealtimeBidMessage message) {
        if (writer != null) {
            writer.println(message.toJson());
        }
    }
}