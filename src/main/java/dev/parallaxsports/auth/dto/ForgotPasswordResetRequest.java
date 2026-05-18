package dev.parallaxsports.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordResetRequest(
    @Email @NotBlank String email,
    @NotBlank @Pattern(regexp = "\\d{6}") String code,
    @NotBlank String password
) {
}
