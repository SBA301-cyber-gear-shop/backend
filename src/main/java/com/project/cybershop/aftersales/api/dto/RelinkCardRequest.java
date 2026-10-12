package com.project.cybershop.aftersales.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelinkCardRequest {

    @NotNull(message = "Chưa chọn thẻ bảo hành")
    private Integer warrantyCardId;

    @NotBlank(message = "Ghi chú nội bộ không được để trống")
    private String internalNote;
}
