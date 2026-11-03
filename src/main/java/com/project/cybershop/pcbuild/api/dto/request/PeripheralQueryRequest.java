package com.project.cybershop.pcbuild.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Request POST /peripherals: chỉ nhận ngân sách và danh sách linh kiện, giá do server tính. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeripheralQueryRequest {
    @NotNull(message = "Ngân sách không được để trống")
    @DecimalMin(value = "1", message = "Ngân sách phải lớn hơn 0")
    @DecimalMax(value = "9999999999", message = "Ngân sách tối đa 9.999.999.999 ₫")
    private BigDecimal budget;

    private String archetype; // giá trị lạ hoặc null được coi là GENERAL

    @Valid
    @NotEmpty(message = "Danh sách linh kiện không được để trống")
    @Size(max = 16, message = "Tối đa 16 dòng linh kiện")
    private List<ValidateBuildRequest.ItemPart> items;
}
