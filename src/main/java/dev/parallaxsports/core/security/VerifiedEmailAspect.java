package dev.parallaxsports.core.security;

import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

// https://www.baeldung.com/spring-aop-annotation
@Aspect
@Component
@RequiredArgsConstructor
public class VerifiedEmailAspect {

	private final UserRepository userRepository;

	@Before("@annotation(RequiresVerifiedEmail) || @within(RequiresVerifiedEmail)")
	public void checkEmailVerified() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
			throw new AccessDeniedException("Authentication required");
		}

		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new AccessDeniedException("User not found"));

		if (!user.isEmailVerified()) {
			throw new AccessDeniedException("Email verification required");
		}
	}
}