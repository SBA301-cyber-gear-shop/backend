package com.project.cybershop.aftersales.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NoFaultComponentsRequest(
        @NotEmpty(message = "Chọn ít nhất một linh kiện") List<@Valid @NotNull Component> components,
        @NotBlank(message = "Ghi chú nội bộ không được để trống") String internalNote,
        String publicNote) {
    public record Component(@NotNull Integer id, @NotNull Long version) {}
}
