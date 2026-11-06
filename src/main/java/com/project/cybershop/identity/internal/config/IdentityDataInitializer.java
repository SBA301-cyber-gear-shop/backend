package com.project.cybershop.identity.internal.config;

import com.project.cybershop.identity.internal.entity.Role;
import com.project.cybershop.identity.internal.entity.User;
import com.project.cybershop.identity.internal.repository.RoleRepository;
import com.project.cybershop.identity.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tạo role và tài khoản mẫu ban đầu khi khởi động. Chỉ thêm cái còn thiếu, không sửa hay xóa dữ liệu đã có,
 * nên chạy lại nhiều lần vẫn an toàn. Tắt bằng app.seed.enabled=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityDataInitializer implements ApplicationRunner {

    /** Tên role phải khớp enum Actor (aftersales), hasRole(...) trong controller và UserRole ở frontend. */
    private record SeedAccount(String role, String roleDescription, String fullName, String email) {
    }

    private static final List<SeedAccount> SEED_ACCOUNTS = List.of(
            new SeedAccount("ADMIN", "Quản trị viên hệ thống", "Quản trị viên", "admin@cybershop.vn"),
            new SeedAccount("STAFF", "Nhân viên bán hàng, vận hành đơn", "Nhân viên cửa hàng", "staff@cybershop.vn"),
            new SeedAccount("TECHNICIAN", "Kỹ thuật viên bảo hành, sửa chữa", "Kỹ thuật viên", "technician@cybershop.vn"),
            new SeedAccount("CUSTOMER", "Khách hàng mua sắm", "Khách hàng mẫu", "customer@cybershop.vn"));

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password:Cybershop@123}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int createdRoles = 0;
        int createdUsers = 0;
        for (SeedAccount account : SEED_ACCOUNTS) {
            Role role = roleRepository.findByNameIgnoreCase(account.role()).orElse(null);
            if (role == null) {
                role = roleRepository.save(new Role(account.role(), account.roleDescription()));
                createdRoles++;
            }
            if (!userRepository.existsByEmailIgnoreCase(account.email())) {
                userRepository.save(new User(role, account.fullName(), account.email(), null,
                        passwordEncoder.encode(seedPassword), null));
                createdUsers++;
                log.info("Khởi tạo tài khoản mẫu {} ({})", account.email(), account.role());
            }
        }
        log.info("Khởi tạo dữ liệu identity xong: thêm {} role, {} tài khoản", createdRoles, createdUsers);
    }
}
