package com.auction.realtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RealtimeBidServer {

    private static final RealtimeBidServer INSTANCE = new RealtimeBidServer();

    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();

    private volatile boolean running;
    private ServerSocket serverSocket;

    private RealtimeBidServer() {
    }

    public static RealtimeBidServer getInstance() {
        return INSTANCE;
    }

    public synchronized void start(int port) {
        if (running) {
            return;
        }

        Thread serverThread = new Thread(() -> runServer(port));
        serverThread.setDaemon(true);
        serverThread.setName("realtime-bid-server");
        serverThread.start();
    }

    private void runServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            while (running) {
                Socket socket = serverSocket.accept();
                ClientConnection clientConnection = new ClientConnection(socket);

                clients.add(clientConnection);

                Thread clientThread = new Thread(
                        () -> listenClient(clientConnection)
                );

                clientThread.setDaemon(true);
                clientThread.setName("realtime-bid-client-handler");
                clientThread.start();
            }

        } catch (BindException exception) {
            running = false;
            System.out.println("RealtimeBidServer: port already used, using existing server.");
        } catch (IOException exception) {
            running = false;
            System.out.println("RealtimeBidServer stopped: " + exception.getMessage());
        }
    }

    private void listenClient(ClientConnection clientConnection) {
        try {
            String message;

            while ((message = clientConnection.reader.readLine()) != null) {
                broadcast(message);
            }

        } catch (IOException exception) {
            System.out.println("Realtime client disconnected.");
        } finally {
            clients.remove(clientConnection);
            clientConnection.close();
        }
    }

    private void broadcast(String message) {
        for (ClientConnection client : clients) {
            client.send(message);
        }
    }

    private static class ClientConnection {

        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        private ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        }

        private void send(String message) {
            writer.println(message);
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}