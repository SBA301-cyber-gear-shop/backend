package com.project.cybershop.aiconfig.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigCreateRequest {

    @NotBlank(message = "Mã cấu hình (configKey) không được để trống")
    @Size(max = 50, message = "Mã cấu hình tối đa 50 ký tự")
    private String configKey;

    @NotBlank(message = "Tên cấu hình (configName) không được để trống")
    @Size(max = 255, message = "Tên cấu hình tối đa 255 ký tự")
    private String configName;

    @NotBlank(message = "Nhà cung cấp (provider) không được để trống")
    @Size(max = 50, message = "Nhà cung cấp tối đa 50 ký tự")
    private String provider;

    @NotBlank(message = "Tên mô hình (modelName) không được để trống")
    @Size(max = 100, message = "Tên mô hình tối đa 100 ký tự")
    private String modelName;

    @NotBlank(message = "API Key không được để trống")
    private String apiKey;

    private String systemPrompt;

    @Builder.Default
    private Boolean isActive = false;
}

