#!/bin/sh
echo "Dang khoi dong Auction Socket Server tai port 5555..."
./mvnw -q exec:java -Dexec.mainClass=com.auction.socket.AuctionSocketServer
