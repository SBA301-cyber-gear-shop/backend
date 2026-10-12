package com.project.cybershop.aftersales.internal.service;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.identity.api.dto.UserDto;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Người đang đăng nhập, lấy từ JWT đã được JwtAuthenticationFilter gắn vào SecurityContext.
 */
@Component
public class CurrentUser {

    public record Principal(Integer id, String name, Actor actor) {
    }

    private final IdentityApi identityApi;

    public CurrentUser(IdentityApi identityApi) {
        this.identityApi = identityApi;
    }

    /** Truyền danh sách vai trò để giới hạn. Để trống nghĩa là chỉ cần đã đăng nhập. */
    public Principal require(Actor... allowed) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("Vui lòng đăng nhập.");
        }
        UserDto user = identityApi.getUserByEmail(auth.getName())
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .orElseThrow(() -> new InsufficientAuthenticationException("Tài khoản không tồn tại hoặc đã bị khóa."));
        Actor actor = actorOf(user.getRole());
        if (allowed.length > 0 && !List.of(allowed).contains(actor)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        return new Principal(user.getId(), user.getFullName(), actor);
    }

    private static Actor actorOf(String role) {
        try {
            return Actor.valueOf(role.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new AccessDeniedException("Vai trò không được hỗ trợ: " + role);
        }
    }
}
