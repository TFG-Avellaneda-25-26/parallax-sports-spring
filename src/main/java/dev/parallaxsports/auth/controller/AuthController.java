package dev.parallaxsports.auth.controller;

import dev.parallaxsports.auth.dto.AuthResponse;
import dev.parallaxsports.auth.dto.EmailVerificationResponse;
import dev.parallaxsports.auth.dto.LoginRequest;
import dev.parallaxsports.auth.dto.RefreshTokenRequest;
import dev.parallaxsports.auth.dto.RegisterRequest;
import dev.parallaxsports.auth.service.AuthService;
import dev.parallaxsports.auth.service.EmailVerificationService;
import dev.parallaxsports.auth.service.OAuthService;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {

	private final AuthService authService;
	private final OAuthService oAuthService;
	private final EmailVerificationService emailVerificationService;
	private final UserRepository userRepository;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.ok(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		// Exchanges email/password for access + refresh token pair.
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		// Refresh endpoint is the only endpoint where refresh tokens are accepted.
		return ResponseEntity.ok(authService.refresh(request));
	}

	@GetMapping("/verify-email")
	public ResponseEntity<EmailVerificationResponse> verifyEmail(@RequestParam String token) {
		emailVerificationService.verify(token);
		return ResponseEntity.ok(new EmailVerificationResponse("Email verified successfully"));
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<EmailVerificationResponse> resendVerification(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		emailVerificationService.resendVerification(user);
		return ResponseEntity.ok(new EmailVerificationResponse("Verification email sent"));
	}

	//TODO OAuth2
}
