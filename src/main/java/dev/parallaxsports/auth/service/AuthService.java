package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.dto.AuthResponse;
import dev.parallaxsports.auth.dto.LoginRequest;
import dev.parallaxsports.auth.dto.RegisterRequest;
import dev.parallaxsports.auth.model.TokenType;
import dev.parallaxsports.auth.security.UserDetailsServiceImpl;
import dev.parallaxsports.core.exception.DuplicateResourceException;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.core.exception.UnauthorizedException;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.model.UserRole;
import dev.parallaxsports.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final EmailVerificationService emailVerificationService;
	private final RefreshTokenService refreshTokenService;
	private final UserDetailsServiceImpl userDetailsService;

	public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
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

		return issueAndSetCookies(saved, response);
	}

	public AuthResponse login(LoginRequest request, HttpServletResponse response) {
		try {
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

		return issueAndSetCookies(saved, response);
	}

	public AuthResponse refresh(String refreshToken, HttpServletResponse response) {
		Claims claims;
		try {
			claims = jwtTokenProvider.parseClaims(refreshToken);
		} catch (JwtException | IllegalArgumentException ex) {
			log.warn("Refresh token parse failed: {}", ex.getMessage());
			throw new UnauthorizedException("Invalid refresh token");
		}

		String jti = claims.getId();
		String subject = claims.getSubject();

		User user = userRepository.findByEmail(subject)
			.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

		UserDetails userDetails = userDetailsService.loadUserByUsername(subject);

		if (!jwtTokenProvider.isTokenValid(claims, userDetails, "refresh")) {
			log.warn("Refresh token rejected by validation subject='{}'", subject);
			throw new UnauthorizedException("Invalid refresh token");
		}

		// Validate against DB: check not revoked, not expired, hash matches.
		var stored = refreshTokenService.validate(jti, refreshToken);
		if (stored.isEmpty()) {
			// Valid JWT but not in DB — possible refresh token reuse attack.
			log.warn("Refresh token not found in DB for subject='{}', possible reuse attack — revoking all tokens", subject);
			refreshTokenService.revokeAllByUser(user.getId());
			throw new UnauthorizedException("Invalid refresh token");
		}

		// Rotate: revoke old token + store new one in one transaction.
		String newAccessToken = jwtTokenProvider.issueAccessToken(user);
		String newRefreshToken = jwtTokenProvider.issueRefreshToken(user);
		Claims newRefreshClaims = jwtTokenProvider.parseClaims(newRefreshToken);

		refreshTokenService.rotateToken(jti, user, newRefreshToken, newRefreshClaims);

		refreshTokenService.addTokenCookie(response, TokenType.REFRESH_TOKEN, newRefreshToken);
		refreshTokenService.addTokenCookie(response, TokenType.ACCESS_TOKEN, newAccessToken);

		log.info("Tokens rotated for subject='{}'", user.getEmail());
		return new AuthResponse(user.getId(), user.isEmailVerified());
	}

	public void logout(String refreshToken, HttpServletResponse response) {
		if (refreshToken != null && !refreshToken.isBlank()) {
			try {
				Claims claims = jwtTokenProvider.parseClaims(refreshToken);
				String jti = claims.getId();
				if (jti != null) {
					refreshTokenService.revokeByJti(jti);
				}
			} catch (JwtException | IllegalArgumentException ex) {
				log.debug("Logout with invalid token (ignored): {}", ex.getMessage());
			}
		}
		refreshTokenService.clearAuthCookies(response);
	}

	public User resolveUserByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private AuthResponse issueAndSetCookies(User user, HttpServletResponse response) {
		String accessToken = jwtTokenProvider.issueAccessToken(user);
		String refreshToken = jwtTokenProvider.issueRefreshToken(user);
		Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
		refreshTokenService.store(user, refreshToken, refreshClaims);
		refreshTokenService.addTokenCookie(response, TokenType.REFRESH_TOKEN, refreshToken);
		refreshTokenService.addTokenCookie(response, TokenType.ACCESS_TOKEN, accessToken);
		return new AuthResponse(user.getId(), user.isEmailVerified());
	}
}
