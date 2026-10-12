package com.project.cybershop.aftersales.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteRequest {

    @NotBlank(message = "Ghi chú nội bộ không được để trống")
    private String internalNote;

    private String publicNote;

    private LocalDate expectedReturnDate;
}
