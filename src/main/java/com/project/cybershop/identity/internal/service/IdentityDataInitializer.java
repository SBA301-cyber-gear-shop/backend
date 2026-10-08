package com.project.cybershop.identity.internal.service;

import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import com.project.cybershop.identity.internal.repository.RoleRepository;
import com.project.cybershop.identity.internal.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IdentityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityDataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityDataInitializer(RoleRepository roleRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Roles
        Map<String, String> defaultRoles = Map.of(
                "ADMIN", "Quản trị viên toàn quyền hệ thống",
                "STAFF", "Nhân viên bán hàng và xử lý đơn",
                "TECHNICIAN", "Kỹ thuật viên bảo hành và sửa chữa",
                "CUSTOMER", "Khách hàng mua sắm"
        );

        for (Map.Entry<String, String> entry : defaultRoles.entrySet()) {
            if (!roleRepository.existsByNameIgnoreCase(entry.getKey())) {
                roleRepository.save(new Role(entry.getKey(), entry.getValue()));
                log.info("Initialized role: {}", entry.getKey());
            }
        }

        // 2. Seed Demo Accounts
        seedDemoUser("admin@techzone.demo", "0900000131", "Quản trị viên", "ADMIN");
        seedDemoUser("staff@techzone.demo", "0900000129", "Nhân viên cửa hàng", "STAFF");
        seedDemoUser("technician@techzone.demo", "0900000130", "Kỹ thuật viên", "TECHNICIAN");
        seedDemoUser("customer@techzone.demo", "0900000128", "Nguyễn Minh Anh", "CUSTOMER");
    }

    private void seedDemoUser(String email, String phone, String fullName, String roleName) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            Role role = roleRepository.findByNameIgnoreCase(roleName)
                    .orElseGet(() -> roleRepository.save(new Role(roleName, roleName)));
            User user = new User(
                    role,
                    fullName,
                    email,
                    phone,
                    passwordEncoder.encode("Demo@123"),
                    "Hồ Chí Minh, Việt Nam"
            );
            userRepository.save(user);
            log.info("Initialized demo account: {} ({})", email, roleName);
        }
    }
}
