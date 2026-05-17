package dev.parallaxsports.core.config;

import dev.parallaxsports.auth.security.JwtAuthenticationFilter;
import dev.parallaxsports.auth.security.OAuth2SuccessHandler;
import dev.parallaxsports.auth.security.UserDetailsServiceImpl;
import dev.parallaxsports.auth.service.OAuthService;
import dev.parallaxsports.bot.filter.BotApiKeyFilter;
import dev.parallaxsports.core.exception.RestAccessDeniedHandler;
import dev.parallaxsports.core.exception.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BotApiKeyFilter botApiKeyFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final OAuthService oAuthService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
            )
            //! SAVE ALL ENDPOINTS HERE,ORGANIZED. NO @Preauthorize's IN CONTROLLERS!!!
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/bot/**").permitAll()
                .requestMatchers("/api/users/email").permitAll()
                .requestMatchers("/api/formula1/**", "/api/basketball/**").permitAll()
                .requestMatchers("/api/league-of-legends/**", "/api/valorant/**", "/api/dota2/**", "/api/counter-strike/**", "/api/overwatch/**").permitAll()
                .requestMatchers("/api/internal/alerts/**").permitAll()
                .requestMatchers("/api/internal/discord/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuthService))
                .successHandler(oAuth2SuccessHandler)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(botApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
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
    
    // https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html
    //
    // Uses a dedicated CORS property so `app.frontend-url` can stay a single
    // canonical URL for redirect construction (OAuth2SuccessHandler, etc.).
    // Defaults to app.frontend-url when app.cors.allowed-origins is unset, so
    // existing deployments don't need a config change.
    //
    // Example .env that allows the deployed Angular AND a local ng-serve:
    //   APP_FRONTEND_URL=http://192.168.1.29
    //   APP_CORS_ALLOWED_ORIGINS=http://192.168.1.29,http://localhost:4200
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins:${app.frontend-url}}") String corsAllowedOrigins
    ) {
        List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        log.info("CORS allowed origins: {}", origins);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
