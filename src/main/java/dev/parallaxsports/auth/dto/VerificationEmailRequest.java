package dev.parallaxsports.auth.dto;

public record VerificationEmailRequest(
	String to,
	String code,
	String displayName
) {
}
