package com.project.cybershop.review.api.dto.request;

import jakarta.validation.constraints.*;
public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @NotBlank @Size(max=5000) String comment) {}
