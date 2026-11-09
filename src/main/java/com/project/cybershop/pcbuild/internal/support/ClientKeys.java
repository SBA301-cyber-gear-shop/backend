package com.project.cybershop.pcbuild.internal.support;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;

/** Khóa đếm rate limit AI: email khi đã đăng nhập, ngược lại là IP của client. */
public final class ClientKeys {

    private ClientKeys() {
    }

    public static String of(Principal principal, HttpServletRequest request) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return "user:" + principal.getName();
        }
        // shortcut: tin X-Forwarded-For nên client tự đặt header là né được hạn mức; khi deploy sau proxy thì chỉ tin header do proxy ghi
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + (request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown");
    }
}
