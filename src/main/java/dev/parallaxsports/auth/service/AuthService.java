package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.dto.AuthResponse;
import dev.parallaxsports.auth.dto.LoginRequest;
import dev.parallaxsports.auth.dto.RefreshTokenRequest;
import dev.parallaxsports.auth.dto.RegisterRequest;
import dev.parallaxsports.core.exception.DuplicateResourceException;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.core.exception.UnauthorizedException;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.model.UserRole;
import dev.parallaxsports.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.OffsetDateTime;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final EmailVerificationService emailVerificationService;

	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateResourceException("User already exists with email: " + request.email());
		}

		User user = User.builder()
			.email(request.email())
			.passwordHash(passwordEncoder.encode(request.password()))
			.displayName(request.displayName())
			// want admin? edit it in the DB
			.role(UserRole.USER)
			.build();

		User saved = userRepository.save(user);
		log.info("Registered new user '{}' with role {}", saved.getEmail(), saved.getRole());

		try {
			emailVerificationService.createAndSendVerification(saved);
		} catch (Exception ex) {
			log.warn("Verification email could not be sent for user '{}': {}", saved.getEmail(), ex.getMessage());
		}

		return new AuthResponse(
			saved.getId(),
			jwtTokenProvider.issueAccessToken(saved),
			jwtTokenProvider.issueRefreshToken(saved),
			saved.isEmailVerified()
		);
	}

	public AuthResponse login(LoginRequest request) {
		try {
			// Delegate password verification to Spring Security AuthenticationManager.
			authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.email(), request.password())
			);
		} catch (AuthenticationException ex) {
			throw new UnauthorizedException("Invalid credentials");
		}

		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

		user.setLastLoginAt(OffsetDateTime.now());
		User saved = userRepository.save(user);
		log.info("User '{}' logged in", saved.getEmail());

		return new AuthResponse(
			saved.getId(),
			jwtTokenProvider.issueAccessToken(saved),
			jwtTokenProvider.issueRefreshToken(saved),
			saved.isEmailVerified()
		);
	}

	public AuthResponse refresh(RefreshTokenRequest request) {
		// TODO: persist/rotate refresh token identifiers (e.g., jti hash) for revocation support.
		String refreshToken = request.refreshToken();
		String requestContext = currentRequestContext();
		String subject;
		Claims claims;
		try {
			// Parse token first; invalid/expired/forged tokens map to 401.
			claims = jwtTokenProvider.parseClaims(refreshToken);
			subject = claims.getSubject();
			log.info(
				"Refresh attempt accepted for parsing subject='{}' expiresAt='{}' {}",
				subject,
				claims.getExpiration(),
				requestContext
			);
		} catch (JwtException | IllegalArgumentException ex) {
			log.warn("Refresh token parse failed: {} {}", ex.getMessage(), requestContext);
			throw new UnauthorizedException("Invalid refresh token");
		}

		User user = userRepository.findByEmail(subject)
			.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

		org.springframework.security.core.userdetails.UserDetails userDetails =
			org.springframework.security.core.userdetails.User
				.withUsername(user.getEmail())
				.password(user.getPasswordHash() == null ? "" : user.getPasswordHash())
				.authorities("ROLE_" + user.getRole().name())
				.build();

		if (!jwtTokenProvider.isTokenValid(claims, userDetails, "refresh")) {
			log.warn("Refresh token rejected by validation subject='{}' {}", subject, requestContext);
			throw new UnauthorizedException("Invalid refresh token");
		}

		// Rotate by issuing a new access token and a new refresh token.
		log.info("Refresh token accepted and rotating tokens for subject='{}' {}", user.getEmail(), requestContext);
		return new AuthResponse(
			user.getId(),
			jwtTokenProvider.issueAccessToken(user),
			jwtTokenProvider.issueRefreshToken(user),
			user.isEmailVerified()
		);
	}

	public User resolveUserByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private String currentRequestContext() {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return "uri=unknown ip=unknown";
		}
		HttpServletRequest req = attrs.getRequest();
		String uri = req == null ? "unknown" : req.getRequestURI();
		String ip = req == null ? "unknown" : req.getRemoteAddr();
		return "uri='" + uri + "' ip='" + ip + "'";
	}

}
