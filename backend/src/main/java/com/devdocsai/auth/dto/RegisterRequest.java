// ─── RegisterRequest.java ─────────────────────────────────────────────────────
package com.devdocsai.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be 2–100 characters")
    String companyName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).+$",
        message = "Password must contain at least one uppercase letter, one number, and one special character"
    )
    String password
) {}
