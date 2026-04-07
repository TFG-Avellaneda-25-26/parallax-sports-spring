package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.client.EmailVerificationClient;
import dev.parallaxsports.auth.dto.VerificationEmailRequest;
import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
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
public class EmailVerificationService {

	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final int MAX_ATTEMPTS = 5;
	private static final String CODE_KEY_PREFIX = "email-verify:";
	private static final String ATTEMPTS_KEY_PREFIX = "email-verify-attempts:";
	private static final String COOLDOWN_KEY_PREFIX = "email-verify-cooldown:";
	private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);

	private final UserRepository userRepository;
	private final EmailVerificationClient emailClient;
	private final StringRedisTemplate redisTemplate;
	private final SecureRandom secureRandom = new SecureRandom();

	public void createAndSendVerification(User user) {
		String code = generateCode();

		redisTemplate.opsForValue().set(CODE_KEY_PREFIX + user.getEmail(), code, CODE_TTL);
		redisTemplate.delete(ATTEMPTS_KEY_PREFIX + user.getEmail());

		emailClient.sendVerificationEmail(new VerificationEmailRequest(
			user.getEmail(),
			code,
			user.getDisplayName()
		));

		log.info("Verification code sent for userId={}", user.getId());
	}

	@Transactional
	public void verify(User user, String code) {
		if (user.isEmailVerified()) {
			throw new BadRequestException("Email is already verified");
		}

		String attemptsKey = ATTEMPTS_KEY_PREFIX + user.getEmail();
		Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
		if (attempts != null && attempts == 1) {
			redisTemplate.expire(attemptsKey, CODE_TTL);
		}
		if (attempts != null && attempts > MAX_ATTEMPTS) {
			throw new BadRequestException("Too many verification attempts. Request a new code.");
		}

		String storedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + user.getEmail());
		if (storedCode == null) {
			throw new BadRequestException("Verification code expired. Request a new code.");
		}
		if (!storedCode.equals(code)) {
			throw new BadRequestException("Invalid verification code");
		}

		user.setEmailVerified(true);
		userRepository.save(user);

		redisTemplate.delete(CODE_KEY_PREFIX + user.getEmail());
		redisTemplate.delete(attemptsKey);

		log.info("Email verified for userId={}", user.getId());
	}

	public void resendVerification(User user) {
		if (user.isEmailVerified()) {
			throw new BadRequestException("Email is already verified");
		}
		Boolean set = redisTemplate.opsForValue()
			.setIfAbsent(COOLDOWN_KEY_PREFIX + user.getEmail(), "1", COOLDOWN_TTL);
		if (Boolean.FALSE.equals(set)) {
			throw new BadRequestException("Please wait before requesting another verification code");
		}
		createAndSendVerification(user);
	}

	private String generateCode() {
		return String.format("%06d", secureRandom.nextInt(1_000_000));
	}
}
