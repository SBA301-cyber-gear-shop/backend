package com.project.cybershop.pcbuild.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Kết quả AI phân tích nhu cầu và nhận diện mục đích ở lần gọi thứ nhất. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeedAnalysis {
    private boolean needsClarification;
    private String clarificationQuestion;
    private String purpose;
    @Builder.Default
    private List<String> secondaryPurposes = new ArrayList<>();
    private String performanceExpectation;
    private String reason;
}

