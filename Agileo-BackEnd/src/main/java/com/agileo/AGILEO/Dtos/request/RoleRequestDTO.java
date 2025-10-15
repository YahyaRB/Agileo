package com.agileo.AGILEO.Dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoleRequestDTO {
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    private String name;

    public RoleRequestDTO() {}

    public RoleRequestDTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RoleRequestDTO{" +
                "name='" + name + '\'' +
                '}';
    }
}