@echo off
echo Dang khoi dong Auction Socket Server tai port 5555...
mvnw.cmd -q exec:java -Dexec.mainClass=com.auction.socket.AuctionSocketServer
pause
