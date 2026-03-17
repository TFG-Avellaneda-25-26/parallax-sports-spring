package dev.parallaxsports.auth.security;

import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// Using email as the login identifier keeps parity with Register/Login request DTOs.
		User user = userRepository.findByEmail(username)
			.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

		// If password hash is missing, this account is expected to authenticate through OAuth later.
		if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
			throw new BadCredentialsException("Invalid credentials");
		}

		// ROLE_ prefix is required by hasRole("...") checks in SecurityConfig.
		log.debug("Loaded security principal for '{}'", username);
		return org.springframework.security.core.userdetails.User
			.withUsername(user.getEmail())
			.password(user.getPasswordHash())
			.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
			.build();
	}
}
