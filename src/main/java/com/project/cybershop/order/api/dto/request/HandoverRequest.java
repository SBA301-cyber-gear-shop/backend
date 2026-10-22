package com.project.cybershop.order.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record HandoverRequest(
        @NotBlank @Size(max=20) String recipientPhone,
        @AssertTrue boolean recipientVerified,
        @NotNull @Size(max=10000) List<@NotNull @Valid Device> devices
) {
    public record Device(@NotNull Integer orderItemId, @NotBlank @Size(max=100) String serialNumber) {}
}
