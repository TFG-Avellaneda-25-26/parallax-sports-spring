package dev.parallaxsports.auth.dto;

public record VerificationEmailRequest(
	String to,
	String verificationUrl,
	String displayName
) {
}
