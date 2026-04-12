package dev.parallaxsports.auth.controller;

import dev.parallaxsports.auth.dto.AuthResponse;
import dev.parallaxsports.auth.dto.EmailVerificationResponse;
import dev.parallaxsports.auth.dto.LoginRequest;
import dev.parallaxsports.auth.dto.RefreshTokenRequest;
import dev.parallaxsports.auth.dto.RegisterRequest;
import dev.parallaxsports.auth.dto.VerifyEmailRequest;
import dev.parallaxsports.auth.service.AuthService;
import dev.parallaxsports.auth.service.EmailVerificationService;
import dev.parallaxsports.auth.service.OAuthUserProvisioningService;
import dev.parallaxsports.auth.service.RefreshTokenService;
import dev.parallaxsports.core.exception.UnauthorizedException;
import dev.parallaxsports.user.model.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final OAuthUserProvisioningService oAuthUserProvisioningService;
	private final EmailVerificationService emailVerificationService;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(
		@Valid @RequestBody RegisterRequest request,
		HttpServletResponse servletResponse
	) {
		return ResponseEntity.ok(authService.register(request, servletResponse));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
		@Valid @RequestBody LoginRequest request,
		HttpServletResponse servletResponse
	) {
		return ResponseEntity.ok(authService.login(request, servletResponse));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(
		@CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String cookieToken,
		@RequestBody(required = false) RefreshTokenRequest body,
		HttpServletResponse servletResponse
	) {
		String token = cookieToken != null ? cookieToken
			: (body != null ? body.refreshToken() : null);
		if (token == null || token.isBlank()) {
			throw new UnauthorizedException("Refresh token required");
		}
		return ResponseEntity.ok(authService.refresh(token, servletResponse));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		@CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String cookieToken,
		@RequestBody(required = false) RefreshTokenRequest body,
		HttpServletResponse servletResponse
	) {
		String token = cookieToken != null ? cookieToken
			: (body != null ? body.refreshToken() : null);
		authService.logout(token, servletResponse);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/verify-email")
	public ResponseEntity<EmailVerificationResponse> verifyEmail(
		@Valid @RequestBody VerifyEmailRequest request,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = authService.resolveUserByEmail(userDetails.getUsername());
		emailVerificationService.verify(user, request.code());
		return ResponseEntity.ok(new EmailVerificationResponse("Email verified successfully"));
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<EmailVerificationResponse> resendVerification(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = authService.resolveUserByEmail(userDetails.getUsername());
		emailVerificationService.resendVerification(user);
		return ResponseEntity.ok(new EmailVerificationResponse("Verification email sent"));
	}

	@DeleteMapping("/identities/{provider}/{providerSubject}")
	public ResponseEntity<Void> unlinkIdentity(
		@PathVariable String provider,
		@PathVariable String providerSubject,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = authService.resolveUserByEmail(userDetails.getUsername());
		oAuthUserProvisioningService.unlinkIdentity(user, provider, providerSubject);
		return ResponseEntity.noContent().build();
	}
}
