package com.project.cybershop.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình Security cho môi trường phát triển:
 * - Cho phép xem sản phẩm và Swagger công khai; giỏ hàng/checkout cần JWT
 * - Đính kèm JwtAuthenticationFilter để trích xuất user token khi có
 * - Kích hoạt CORS cho phép Frontend (localhost:5173, etc.) kết nối API
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/orders/**", "/api/checkout/**", "/api/operations/**", "/api/order-items/**", "/api/me/**", "/api/shipping/orders/**").authenticated()
                        // Toàn bộ /api/admin chỉ dành cho Admin, kể cả các API đọc và API thêm về sau.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // PC Build (DD 9.7): đề xuất/kiểm tra công khai (có rate limit); build đã lưu cần đăng nhập.
                        // /options phải đứng trước vì "GET /api/v1/pc-build/*" (chi tiết build) cũng khớp nó.
                        .requestMatchers(HttpMethod.GET, "/api/v1/pc-build/options").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/pc-build/save").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/pc-build/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/pc-build/*").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Bạn cần đăng nhập để tiếp tục.\"}");
                }).accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Bạn không có quyền thực hiện thao tác này.\"}");
                }))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // FE khác origin chỉ đọc được Retry-After (503 AI_UNAVAILABLE) khi header được expose
        config.setExposedHeaders(List.of("Retry-After"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

