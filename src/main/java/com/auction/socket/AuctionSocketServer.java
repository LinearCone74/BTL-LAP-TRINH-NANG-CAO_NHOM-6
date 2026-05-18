package com.auction.socket;

import com.auction.repository.RealtimeAuctionRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class AuctionSocketServer {
    public static final int DEFAULT_PORT = 5555;

    private final int port;
    private final RealtimeAuctionRepository repository = new RealtimeAuctionRepository();
    private final Set<ClientHandler> clients = new CopyOnWriteArraySet<>();

    public AuctionSocketServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0 && !args[0].isBlank()) {
            port = Integer.parseInt(args[0]);
        }
        new AuctionSocketServer(port).start();
    }

    public void start() throws IOException {
        System.out.println("Auction socket server dang chay tai port " + port);
        System.out.println("Mo cac client JavaFX va ket noi toi localhost:" + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler, "auction-socket-client-" + socket.getPort()).start();
            }
        }
    }

    private void broadcastAuctionUpdated(int auctionId) {
        String message = "UPDATE|" + auctionId;
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket ignored = socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                send("CONNECTED|" + encode("Ket noi socket server thanh cong"));

                String line;
                while ((line = in.readLine()) != null) {
                    handleLine(line.trim());
                }
            } catch (Exception e) {
                System.out.println("Client ngat ket noi: " + e.getMessage());
            } finally {
                clients.remove(this);
            }
        }

        private void handleLine(String line) {
            if (line.isBlank()) {
                return;
            }
            try {
                String[] parts = line.split("\\|", -1);
                String command = parts[0];
                if ("BID".equals(command)) {
                    handleBid(parts);
                } else if ("AUTOBID".equals(command)) {
                    handleAutoBid(parts);
                } else if ("PING".equals(command)) {
                    send("PONG");
                } else {
                    send("RESULT|FAIL|" + encode("Lenh socket khong hop le") + "|0");
                }
            } catch (Exception e) {
                send("RESULT|FAIL|" + encode("Loi server: " + e.getMessage()) + "|0");
            }
        }

        private void handleBid(String[] parts) {
            if (parts.length < 4) {
                send("RESULT|FAIL|" + encode("Thieu du lieu dat bid") + "|0");
                return;
            }
            int auctionId = Integer.parseInt(parts[1]);
            String username = decode(parts[2]);
            BigDecimal amount = new BigDecimal(decode(parts[3]));
            RealtimeAuctionRepository.BidResponse response = repository.placeBid(auctionId, username, amount);
            sendResult(response, auctionId);
            if (response.success()) {
                broadcastAuctionUpdated(auctionId);
            }
        }

        private void handleAutoBid(String[] parts) {
            if (parts.length < 5) {
                send("RESULT|FAIL|" + encode("Thieu du lieu Auto-Bid") + "|0");
                return;
            }
            int auctionId = Integer.parseInt(parts[1]);
            String username = decode(parts[2]);
            BigDecimal maxBid = new BigDecimal(decode(parts[3]));
            BigDecimal increment = new BigDecimal(decode(parts[4]));
            RealtimeAuctionRepository.BidResponse response = repository.registerAutoBid(auctionId, username, maxBid, increment);
            sendResult(response, auctionId);
            if (response.success()) {
                broadcastAuctionUpdated(auctionId);
            }
        }

        private void sendResult(RealtimeAuctionRepository.BidResponse response, int auctionId) {
            send("RESULT|" + (response.success() ? "OK" : "FAIL") + "|" + encode(response.message()) + "|" + auctionId);
        }

        void send(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
