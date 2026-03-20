package dev.parallaxsports.core.config;

import dev.parallaxsports.auth.security.JwtAuthenticationFilter;
import dev.parallaxsports.auth.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error", "/actuator/health/**", "/actuator/info", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-ui/**", "/api/auth/**", "/api/formula1/**").permitAll()
                .requestMatchers("/api/internal/alerts/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // Replaces default/basic auth with our DB-backed user lookup + password encoder.
            .authenticationProvider(authenticationProvider())
            // Runs before username/password filter to build SecurityContext from Bearer JWT.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Note: httpBasic was intentionally removed. With JWT, sending username/password
        // every request is unnecessary and less secure than short-lived signed access tokens.
        log.info("Security filter chain initialized: stateless JWT with ADMIN route protection");

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DAO provider delegates credential checks to UserDetailsService + PasswordEncoder.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // Exposed for login flow where we authenticate email/password once and then issue JWT.
        return configuration.getAuthenticationManager();
    }
}
