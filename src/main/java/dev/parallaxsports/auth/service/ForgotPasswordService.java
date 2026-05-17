package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.client.EmailVerificationClient;
import dev.parallaxsports.auth.dto.VerificationEmailRequest;
import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import dev.parallaxsports.user.service.UserService;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final int MAX_ATTEMPTS = 5;
	private static final String CODE_KEY_PREFIX = "forgot-password:";
	private static final String ATTEMPTS_KEY_PREFIX = "forgot-password-attempts:";
	private static final String COOLDOWN_KEY_PREFIX = "forgot-password-cooldown:";
	private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);

	private final UserRepository userRepository;
	private final EmailVerificationClient emailClient;
	private final StringRedisTemplate redisTemplate;
	private final UserService userService;
	private final SecureRandom secureRandom = new SecureRandom();

	public void requestForgotPassword(String email) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

		Boolean set = redisTemplate.opsForValue()
			.setIfAbsent(COOLDOWN_KEY_PREFIX + email, "1", COOLDOWN_TTL);
		if (Boolean.FALSE.equals(set)) {
			throw new BadRequestException("Please wait before requesting another code");
		}

		String code = generateCode();

		redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);
		redisTemplate.delete(ATTEMPTS_KEY_PREFIX + email);

		try {
			emailClient.sendVerificationEmail(new VerificationEmailRequest(
				email,
				code,
				user.getDisplayName()
			));
			log.info("Forgot password OTP email dispatched successfully to={}", email);
		} catch (Exception ex) {
			log.warn("Failed to dispatch forgot password email to={} (Ktor service offline/unreachable: {}). "
				+ "Proceeding for local development.", email, ex.getMessage());
		}

		// Log the OTP code directly so you can easily copy-paste it during testing
		log.info("DEVELOPMENT ONLY - Forgot password OTP code generated for email={}: {}", email, code);
	}

	public void verifyCode(String email, String code) {
		if (!userRepository.existsByEmail(email)) {
			throw new ResourceNotFoundException("User not found with email: " + email);
		}

		String attemptsKey = ATTEMPTS_KEY_PREFIX + email;
		Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
		if (attempts != null && attempts == 1) {
			redisTemplate.expire(attemptsKey, CODE_TTL);
		}
		if (attempts != null && attempts > MAX_ATTEMPTS) {
			throw new BadRequestException("Too many verification attempts. Request a new code.");
		}

		String storedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
		if (storedCode == null) {
			throw new BadRequestException("Verification code expired. Request a new code.");
		}
		if (!storedCode.equals(code)) {
			throw new BadRequestException("Invalid verification code");
		}
	}

	@Transactional
	public void resetPassword(String email, String code, String newPassword) {
		// Verify the code again for security before executing modifications
		verifyCode(email, code);

		// Verify that the new password is not the same as the current password
		if (userService.validatePassword(email, newPassword)) {
			throw new BadRequestException("New password cannot be the same as the current password");
		}

		// Update the password in the database (reusing hashing & persistence)
		userService.updatePassword(email, newPassword);

		// Clean up Redis keys
		redisTemplate.delete(CODE_KEY_PREFIX + email);
		redisTemplate.delete(ATTEMPTS_KEY_PREFIX + email);
		redisTemplate.delete(COOLDOWN_KEY_PREFIX + email);

		log.info("Password successfully reset for email={}", email);
	}

	private String generateCode() {
		return String.format("%06d", secureRandom.nextInt(1_000_000));
	}
}
