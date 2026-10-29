package com.project.cybershop;

import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import com.project.cybershop.identity.internal.repository.RoleRepository;
import com.project.cybershop.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@org.springframework.stereotype.Component
public class TestIdentityDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestIdentityDataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
            Map<String, String> defaultRoles = Map.of(
                    "ADMIN", "Quản trị viên toàn quyền hệ thống",
                    "STAFF", "Nhân viên bán hàng và xử lý đơn",
                    "TECHNICIAN", "Kỹ thuật viên bảo hành và sửa chữa",
                    "CUSTOMER", "Khách hàng mua sắm"
            );

            for (Map.Entry<String, String> entry : defaultRoles.entrySet()) {
                if (!roleRepository.existsByNameIgnoreCase(entry.getKey())) {
                    roleRepository.save(new Role(entry.getKey(), entry.getValue()));
                }
            }

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
                    passwordEncoder.encode("123456"),
                    "Hồ Chí Minh, Việt Nam"
            );
            userRepository.save(user);
        }
    }
}
