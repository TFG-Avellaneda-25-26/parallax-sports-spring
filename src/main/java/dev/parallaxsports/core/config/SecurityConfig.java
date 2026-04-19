package dev.parallaxsports.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.parallaxsports.auth.security.JwtAuthenticationFilter;
import dev.parallaxsports.auth.security.UserDetailsServiceImpl;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.UNAUTHORIZED, "Authentication required"
                    );
                    problem.setType(URI.create("/problems/unauthorized"));
                    problem.setTitle("Unauthorized");
                    problem.setInstance(URI.create(request.getRequestURI()));
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), problem);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN, "Access denied"
                    );
                    problem.setType(URI.create("/problems/forbidden"));
                    problem.setTitle("Forbidden");
                    problem.setInstance(URI.create(request.getRequestURI()));
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), problem);
                })
            )
            //! SAVE ALL ENDPOINTS HERE,ORGANIZED. NO @Preauthorize's IN CONTROLLERS!!!
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/formula1/**", "/api/basketball/**").permitAll()
                // Permitir acceso público (sin ADMIN) a endpoints públicos por videojuego
                .requestMatchers("/api/league-of-legends/**", "/api/valorant/**", "/api/dota2/**", "/api/counter-strike/**").permitAll()
                .requestMatchers("/api/internal/alerts/**").permitAll()
                    .requestMatchers("/api/admin/pandascore/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security filter chain initialized: stateless JWT with ADMIN route protection");

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
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
        return configuration.getAuthenticationManager();
    }
}
