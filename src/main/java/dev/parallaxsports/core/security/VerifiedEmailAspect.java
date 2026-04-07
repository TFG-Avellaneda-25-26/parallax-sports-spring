package dev.parallaxsports.core.security;

import io.jsonwebtoken.Claims;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// https://www.baeldung.com/spring-aop-annotation
// mentioned in class but never touched on it, so we wanted to experiment and find a good use
@Aspect
@Component
public class VerifiedEmailAspect {

	@Before("@annotation(RequiresVerifiedEmail) || @within(RequiresVerifiedEmail)")
	public void checkEmailVerified() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof UsernamePasswordAuthenticationToken token)
			|| !(token.getCredentials() instanceof Claims claims)) {
			throw new AccessDeniedException("Authentication required");
		}

		Boolean emailVerified = claims.get("email_verified", Boolean.class);
		if (!Boolean.TRUE.equals(emailVerified)) {
			throw new AccessDeniedException("Email verification required");
		}
	}
}