package dev.parallaxsports.auth.security;

import dev.parallaxsports.auth.service.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		String authorization = request.getHeader("Authorization");
		// No Bearer token -> leave request unauthenticated and let authorization rules decide.
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring(7);
		try {
			Claims claims = jwtTokenProvider.parseClaims(token);
			String subject = claims.getSubject();

			// Skip if authentication already exists; do not override existing principal.
			if (subject == null
				|| SecurityContextHolder.getContext().getAuthentication() != null) {
				filterChain.doFilter(request, response);
				return;
			}

			UserDetails userDetails = userDetailsService.loadUserByUsername(subject);
			if (jwtTokenProvider.isTokenValid(claims, userDetails, "access")) {
				// Build Spring Security principal with authorities extracted from DB-backed UserDetails.
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails,
					null,
					userDetails.getAuthorities()
				);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
				log.debug("Authenticated request '{}' as '{}'", request.getRequestURI(), subject);
			}
		} catch (JwtException | IllegalArgumentException ex) {
			// Invalid token: continue without authentication and let security rules decide.
			log.debug("JWT validation failed for request '{}': {}", request.getRequestURI(), ex.getMessage());
		}

		filterChain.doFilter(request, response);
	}
}
