package com.project.cybershop.pcbuild.internal.support;

import java.text.Normalizer;
import java.util.Locale;

/** Chuẩn hóa chuỗi để so khớp: lowercase, bỏ dấu tiếng Việt, gộp khoảng trắng ("Trắng " → "trang"). */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
