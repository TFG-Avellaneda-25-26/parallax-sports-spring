package dev.parallaxsports.auth.dto;

public record VerificationEmailRequest(
	String email,
	String verificationCode,
	String displayName
) {
}
