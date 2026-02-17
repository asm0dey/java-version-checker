package com.github.asm0dey;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.FormParam;

public record AnalysisFormData(
        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @FormParam("email")
        String email,

        @NotBlank(message = "Company name is required")
        @FormParam("companyName")
        String companyName,

        @FormParam("role")
        String role
) {
}
