# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN

## 1. Mô tả bài toán

Đây là bài tập lớn môn Lập trình nâng cao, xây dựng hệ thống đấu giá trực tuyến bằng Java.

Ứng dụng cho phép người dùng đăng ký, đăng nhập, tạo phiên đấu giá, tham gia đặt giá và theo dõi kết quả đấu giá. Hệ thống có các vai trò chính:

- **Admin**: quản lý người dùng, sản phẩm và phiên đấu giá.
- **Seller**: đăng sản phẩm, tạo và quản lý phiên đấu giá.
- **Bidder**: xem phiên đấu giá, đặt giá, sử dụng Auto-Bid và theo dõi lịch sử đặt giá.

Project áp dụng các kiến thức lập trình hướng đối tượng như kế thừa, đa hình, đóng gói, trừu tượng hóa, xử lý ngoại lệ, làm việc với cơ sở dữ liệu và giao tiếp socket realtime.

---

## 2. Công nghệ sử dụng

- Java 17
- JavaFX
- FXML
- CSS
- Maven
- MySQL
- JDBC
- Socket TCP
- Mô hình MVC
- Lập trình hướng đối tượng OOP

---

## 3. Môi trường chạy và yêu cầu cài đặt

Máy cần cài đặt:

- JDK 17 trở lên
- Maven hoặc Maven Wrapper có sẵn trong project
- Kết nối Internet để truy cập database
- Hệ điều hành Windows, macOS hoặc Linux

Database được cấu hình trong file:

```text
src/main/java/database/DBConnection.java
```

---

## 4. Cấu trúc thư mục chính

```text
BTL-LAP-TRINH-NANG-CAO_NHOM-6
│
├── src/main/java/com/auction
│   ├── app
│   ├── controller
│   ├── exception
│   ├── factory
│   ├── model
│   ├── repository
│   ├── service
│   ├── socket
│   └── util
│
├── src/main/java/database
│   └── DBConnection.java
│
├── src/main/resources/com/auction
│   ├── view
│   ├── style
│   └── images
│
├── target
│   └── dau-gia-truc-tuyen-1.0-SNAPSHOT.jar
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── CHAY_SOCKET_SERVER_WINDOWS.bat
├── CHAY_SOCKET_SERVER_MAC_LINUX.sh
└── README.md
```

---

## 5. Các module chính

### Model

Chứa các lớp mô hình dữ liệu của hệ thống như:

- User
- Admin
- Seller
- Bidder
- Item
- Auction
- Bid
- AutoBidConfig

Module này thể hiện các đặc điểm của lập trình hướng đối tượng như kế thừa giữa các loại người dùng, đóng gói dữ liệu và đa hình trong xử lý đối tượng.

### Controller

Chứa các lớp điều khiển giao diện JavaFX như:

- LoginController
- RegisterController
- DashboardController

Controller nhận thao tác từ người dùng, gọi service xử lý nghiệp vụ và cập nhật dữ liệu lên giao diện.

### Service

Chứa logic xử lý chính của chương trình như đăng nhập, đăng ký, quản lý người dùng, quản lý sản phẩm, quản lý phiên đấu giá, đặt giá, Auto-Bid và cập nhật trạng thái phiên đấu giá.

### Repository

Chứa các lớp làm việc với dữ liệu. Hệ thống sử dụng JDBC để kết nối MySQL và lưu thông tin người dùng, sản phẩm, phiên đấu giá, lịch sử đặt giá.

### Socket

Chứa phần xử lý realtime bằng Socket TCP, gồm:

- AuctionSocketServer
- AuctionSocketClient

Socket giúp cập nhật giá mới và lịch sử bid cho nhiều client đang tham gia đấu giá cùng lúc.

---

## 6. Vị trí file JAR

File JAR dùng để chạy chương trình nằm tại:

```text
target/dau-gia-truc-tuyen-1.0-SNAPSHOT.jar
```

---

## 7. Hướng dẫn build project

Tại thư mục gốc của project, chạy lệnh:

### Windows

```bash
mvnw.cmd clean package
```

### macOS / Linux

```bash
./mvnw clean package
```

Sau khi build thành công, file `.jar` sẽ được tạo trong thư mục `target`.

---

## 8. Hướng dẫn chạy chương trình

Chạy ứng dụng bằng lệnh:

```bash
java -jar target/dau-gia-truc-tuyen-1.0-SNAPSHOT.jar
```

## 9. Danh sách chức năng đã hoàn thành

### Chức năng chung

- Đăng ký tài khoản
- Đăng nhập
- Phân quyền người dùng theo vai trò
- Hiển thị giao diện JavaFX
- Kết nối cơ sở dữ liệu MySQL

### Chức năng Seller

- Thêm sản phẩm đấu giá
- Tạo phiên đấu giá
- Quản lý sản phẩm và phiên đấu giá của mình
- Theo dõi trạng thái phiên đấu giá

### Chức năng Bidder

- Xem danh sách phiên đấu giá
- Xem chi tiết phiên đấu giá
- Đặt giá thủ công
- Bật Auto-Bid
- Xem người đang dẫn đầu
- Xem lịch sử đặt giá
- Theo dõi biểu đồ giá

### Chức năng realtime

- Kết nối client với socket server
- Gửi yêu cầu đặt giá qua socket
- Cập nhật giá mới cho các client
- Cập nhật lịch sử bid realtime
- Hỗ trợ nhiều người dùng cùng tham gia đấu giá

### Chức năng nâng cao

- Auto-Bid tự động đặt giá
- Anti-sniping: tự động gia hạn phiên khi có bid gần thời điểm kết thúc
- Kiểm tra bid hợp lệ
- Xử lý ngoại lệ khi đặt giá sai
- Lưu dữ liệu vào database

---

## 10. Một số xử lý nghiệp vụ chính

### Đặt giá thủ công

Khi người dùng đặt giá, hệ thống kiểm tra:

- Người dùng đã đăng nhập hay chưa
- Phiên đấu giá có tồn tại không
- Phiên đấu giá còn đang mở không
- Giá mới có lớn hơn giá hiện tại không
- Người bán không được tự đặt giá sản phẩm của mình

Nếu hợp lệ, hệ thống cập nhật giá hiện tại và lưu lịch sử đặt giá.

### Auto-Bid

Người dùng có thể nhập giá tối đa và bước nhảy. Khi có người khác đặt giá cao hơn, hệ thống tự động đặt giá mới nếu chưa vượt quá mức tối đa đã cấu hình.

### Anti-sniping

Nếu có người đặt giá sát thời điểm kết thúc phiên đấu giá, hệ thống sẽ tự động gia hạn thời gian kết thúc để đảm bảo đấu giá công bằng hơn.

---

## 11. Cách kiểm thử nhanh

1. Chạy Socket Server.
2. Chạy ứng dụng client.
3. Đăng ký hoặc đăng nhập tài khoản Seller.
4. Tạo sản phẩm và phiên đấu giá.
5. Mở thêm một client khác.
6. Đăng ký hoặc đăng nhập tài khoản Bidder.
7. Bidder đặt giá cho phiên đấu giá.
8. Kiểm tra giá mới, người dẫn đầu và lịch sử bid được cập nhật.
9. Thử bật Auto-Bid để kiểm tra đặt giá tự động.
10. Kiểm tra kết quả khi phiên đấu giá kết thúc.

---

## 13. Link báo cáo PDF và video demo

- Link báo cáo PDF: https://drive.google.com/file/d/1pcynLo3Qpg-2DlMlxzYPg9XzER4HVcmQ/view?usp=sharing
- Link video demo: https://drive.google.com/file/d/1yudbULQLKPLk5tVLgvNxgr5tIszObH1H/view?usp=sharing

---

## 14. Ghi chú

- Cần chạy Socket Server trước khi kiểm tra chức năng realtime.
- Nếu client không kết nối được, kiểm tra server đã chạy ở port 5555 chưa.
- Nếu không tải được dữ liệu, kiểm tra kết nối Internet và cấu hình database.
- Có thể mở nhiều cửa sổ client để test nhiều người dùng đấu giá cùng lúc.