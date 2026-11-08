package com.project.cybershop.pcbuild.internal.service;

import com.project.cybershop.pcbuild.internal.model.BuildLine;

import java.util.List;

/**
 * Bước 8: AI viết lời giải thích cho bộ PC vừa tự build, chỉ diễn đạt lại số liệu code đã tính.
 * Không bao giờ ném lỗi: AI lỗi, bị giới hạn tần suất hay viết sai số thì trả câu mẫu soạn sẵn.
 */
public interface BuildExplainer {

    /**
     * @param purposeLabel nhãn mục đích khách đọc được, ví dụ "PC chơi game"
     * @param lines        bộ PC VALID theo step_order
     * @param total        tổng tiền code đã tính ở bước 6 (≤ budget)
     * @param clientKey    khóa đếm rate limit AI (user hoặc IP)
     */
    Explanation explain(String purposeLabel, long budget, List<BuildLine> lines, long total, String clientKey);

    /**
     * @param reason lý do dùng câu mẫu, null khi dùng được câu của AI
     */
    record Explanation(String message, Source source, String reason) {
    }

    enum Source {
        AI,
        TEMPLATE
    }
}
