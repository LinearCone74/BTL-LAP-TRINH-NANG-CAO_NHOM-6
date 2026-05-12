# ONLINE AUCTION SYSTEM

## 1. Giới thiệu dự án

Hệ thống đấu giá trực tuyến được xây dựng bằng Java nhằm mô phỏng hoạt động đấu giá sản phẩm theo thời gian thực.  
Người dùng có thể đăng ký tài khoản, đăng nhập, tạo phiên đấu giá, tham gia đấu giá và theo dõi kết quả đấu giá.

Hệ thống hỗ trợ nhiều vai trò:
- Admin
- Seller
- Bidder

Ngoài ra hệ thống còn hỗ trợ:
- Realtime bidding
- Auto Bid
- Mã hóa mật khẩu
- Quản lý lịch sử đấu giá
- Thông báo đấu giá

---

# 2. Công nghệ sử dụng

## Ngôn ngữ & Framework
- Java 17
- JavaFX
- Maven
- JDBC
- Hibernate
- MySQL

## Thư viện
- MySQL Connector
- Logback
- JUnit

---

# 3. Môi trường chạy và yêu cầu cài đặt

## Yêu cầu:
Cần cài đặt trước:

- JDK 17 trở lên
- Maven
- MySQL Server
- IntelliJ IDEA

---

# 4. Cấu trúc thư mục chính

```text
src/
 ├── controller/      # Xử lý giao diện và sự kiện
 ├── service/         # Xử lý nghiệp vụ
 ├── repository/      # Làm việc với database
 ├── model/           # Các đối tượng hệ thống
 ├── config/          # Cấu hình hệ thống
 ├── network/         # Realtime client/server
 ├── util/            # Hàm hỗ trợ
 └── exception/       # Custom exception