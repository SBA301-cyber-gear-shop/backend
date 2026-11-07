package com.project.cybershop.identity.internal.service.impl;

import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import com.project.cybershop.identity.internal.repository.RoleRepository;
import com.project.cybershop.identity.internal.repository.UserRepository;
import com.project.cybershop.order.internal.entity.Cart;
import com.project.cybershop.order.internal.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khởi tạo dữ liệu người dùng (Users) và vai trò (Roles) mặc định khi ứng dụng
 * khởi động.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.default-password:123456}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== [CYBERSHOP] Bắt đầu kiểm tra và khởi tạo vai trò & người dùng mẫu ===");
        seedRoles();
        seedUsers();
        log.info("=== [CYBERSHOP] Hoàn tất khởi tạo vai trò & người dùng mẫu ===");
    }

    private void seedRoles() {
        Map<String, String> defaultRoles = new LinkedHashMap<>();
        defaultRoles.put("ADMIN", "Quản trị viên toàn quyền hệ thống");
        defaultRoles.put("STAFF", "Nhân viên bán hàng và xử lý đơn hàng");
        defaultRoles.put("TECHNICIAN", "Kỹ thuật viên bảo hành và sửa chữa");
        defaultRoles.put("CUSTOMER", "Khách hàng mua sắm");

        for (Map.Entry<String, String> entry : defaultRoles.entrySet()) {
            String roleName = entry.getKey();
            String description = entry.getValue();
            if (!roleRepository.existsByNameIgnoreCase(roleName)) {
                Role role = new Role(roleName, description);
                roleRepository.save(role);
                log.info("-> Đã khởi tạo vai trò: {} ({})", roleName, description);
            }
        }
    }

    private void seedUsers() {
        // Tài khoản demo chính thức tương thích frontend và test suite
        seedUser("admin@techzone.demo", "0900000131", "Quản trị viên", "ADMIN", "Hồ Chí Minh, Việt Nam");
        seedUser("staff@techzone.demo", "0900000129", "Nhân viên cửa hàng", "STAFF", "Hồ Chí Minh, Việt Nam");
        seedUser("technician@techzone.demo", "0900000130", "Kỹ thuật viên", "TECHNICIAN", "Hồ Chí Minh, Việt Nam");
        seedUser("customer@techzone.demo", "0900000128", "Nguyễn Minh Anh", "CUSTOMER", "Hồ Chí Minh, Việt Nam");

        // Tài khoản mẫu bổ sung mang thương hiệu CyberShop
        seedUser("admin@cybershop.vn", "0900000001", "Admin CyberShop", "ADMIN", "Hà Nội, Việt Nam");
        seedUser("customer@cybershop.vn", "0900000002", "Khách Hàng CyberShop", "CUSTOMER", "Đà Nẵng, Việt Nam");
    }

    private void seedUser(String email, String phone, String fullName, String roleName, String address) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        // Kiểm tra tránh trùng số điện thoại nếu đã tồn tại ở account khác
        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone)) {
            log.warn("Bỏ qua tạo user {} vì số điện thoại {} đã tồn tại.", email, phone);
            return;
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName, roleName)));

        User user = new User(
                role,
                fullName,
                email,
                phone,
                passwordEncoder.encode(defaultPassword),
                address);
        user.setIsActive(true);
        user = userRepository.save(user);
        log.info("-> Đã tạo tài khoản mẫu: {} | Email: {} | SĐT: {} | Role: {}", fullName, email, phone, roleName);

        // Khởi tạo giỏ hàng cho tài khoản Khách hàng nếu chưa có
        if ("CUSTOMER".equalsIgnoreCase(roleName) && !cartRepository.existsByUserId(user.getId())) {
            Cart cart = new Cart();
            cart.setUserId(user.getId());
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
            log.info("   -> Đã khởi tạo giỏ hàng rỗng cho user ID: {}", user.getId());
        }
    }
}
