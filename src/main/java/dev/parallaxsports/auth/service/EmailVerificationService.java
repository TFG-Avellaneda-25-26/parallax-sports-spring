package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.client.EmailVerificationClient;
import dev.parallaxsports.auth.dto.VerificationEmailRequest;
import dev.parallaxsports.core.config.properties.AppProperties;
import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

	private static final int TOKEN_BYTES = 32;

	private final UserRepository userRepository;
	private final EmailVerificationClient emailClient;
	private final AppProperties appProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public void createAndSendVerification(User user) {
		byte[] rawBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(rawBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

		user.setVerificationTokenHash(sha256(rawToken));
		userRepository.save(user);

		String verificationUrl = appProperties.getFrontendUrl() + "/verify-email?token=" + rawToken;

		emailClient.sendVerificationEmail(new VerificationEmailRequest(
			user.getEmail(),
			verificationUrl,
			user.getDisplayName()
		));

		log.info("Verification email queued for userId={}", user.getId());
	}

	@Transactional
	public void verify(String rawToken) {
		String tokenHash = sha256(rawToken);

		User user = userRepository.findByVerificationTokenHash(tokenHash)
			.orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

		if (user.isEmailVerified()) {
			throw new BadRequestException("Email is already verified");
		}

		user.setEmailVerified(true);
		user.setVerificationTokenHash(null);
		userRepository.save(user);

		log.info("Email verified for userId={}", user.getId());
	}

	@Transactional
	public void resendVerification(User user) {
		if (user.isEmailVerified()) {
			throw new BadRequestException("Email is already verified");
		}
		createAndSendVerification(user);
	}

	private String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
