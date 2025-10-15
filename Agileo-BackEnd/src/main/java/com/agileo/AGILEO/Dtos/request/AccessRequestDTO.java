package com.agileo.AGILEO.Dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccessRequestDTO {
    @NotBlank(message = "Access code is required")
    @Size(max = 50, message = "Access code must not exceed 50 characters")
    private String code;

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}