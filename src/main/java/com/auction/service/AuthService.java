package com.auction.service;

import com.auction.exception.ValidationException;
import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Role;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;
import com.auction.util.PasswordHasher;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                    Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public User register(String username,
                         String rawPassword,
                         String fullName,
                         String email,
                         Role role) {

        String normalizedUsername = normalize(username);

        validateRegistration(
                normalizedUsername,
                rawPassword,
                fullName,
                email,
                role
        );

        userRepository.findByUsername(normalizedUsername).ifPresent(user -> {
            throw new ValidationException("Username đã tồn tại.");
        });

        String hash = PasswordHasher.hash(rawPassword.trim());

        User user = switch (role) {
            case BIDDER -> new Bidder(
                    normalizedUsername,
                    hash,
                    fullName.trim(),
                    email.trim()
            );
            case SELLER -> new Seller(
                    normalizedUsername,
                    hash,
                    fullName.trim(),
                    email.trim()
            );
            case ADMIN -> new Admin(
                    normalizedUsername,
                    hash,
                    fullName.trim(),
                    email.trim()
            );
        };

        if (role == Role.ADMIN) {
            user.activate();
        }

        return userRepository.save(user);
    }

    public User login(String username, String rawPassword) {
        String normalizedUsername = normalize(username);

        if (normalizedUsername.isBlank()
                || rawPassword == null
                || rawPassword.isBlank()) {
            throw new ValidationException("Vui lòng nhập username và mật khẩu.");
        }

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() ->
                        new ValidationException("Sai username hoặc mật khẩu.")
                );

        if (!PasswordHasher.matches(rawPassword.trim(), user.getPasswordHash())) {
            throw new ValidationException("Sai username hoặc mật khẩu.");
        }

        if (!user.isActive()) {
            throw new ValidationException("Tài khoản chưa được duyệt hoặc đang bị khóa.");
        }

        return user;
    }

    private void validateRegistration(String username,
                                      String password,
                                      String fullName,
                                      String email,
                                      Role role) {

        if (username == null || username.isBlank()) {
            throw new ValidationException("Username không được để trống.");
        }

        if (!username.matches("[a-z0-9_]{4,30}")) {
            throw new ValidationException(
                    "Username chỉ gồm a-z, 0-9, _, từ 4 đến 30 ký tự."
            );
        }

        if (password == null || password.trim().length() < 6) {
            throw new ValidationException("Mật khẩu phải có ít nhất 6 ký tự.");
        }

        if (fullName == null || fullName.trim().length() < 2) {
            throw new ValidationException("Họ tên không hợp lệ.");
        }

        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Email không hợp lệ.");
        }

        if (role == null) {
            throw new ValidationException("Vui lòng chọn vai trò.");
        }
    }

    private String normalize(String username) {
        return username == null
                ? ""
                : username.trim().toLowerCase(Locale.ROOT);
    }
}