package com.capa.config;

import com.capa.model.OAuthToken;
import com.capa.model.User;
import com.capa.repository.OAuthTokenRepository;
import com.capa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final OAuthTokenRepository tokenRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ClientRegistrationRepository clientRegistrationRepository; // Injected for programmatic customizer query injection

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        // Safe programmatic parameter binding injection via Spring Security API instead of query string hacks
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestResolver(customAuthorizationRequestResolver())
                        )
                        .successHandler((request, response, authentication) -> {
                            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
                            OAuth2User oAuth2User = authToken.getPrincipal();

                            String email = oAuth2User.getAttribute("email");
                            String name = oAuth2User.getAttribute("name");
                            String sub = oAuth2User.getAttribute("sub");

                            User user = userRepository.findByEmail(email).orElseGet(() ->
                                    userRepository.save(User.builder()
                                            .email(email)
                                            .name(name)
                                            .googleSubId(sub)
                                            .build())
                            );

                            // Correct Spring Security 6 structural memory reference lookup hook
                            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                                    authToken.getAuthorizedClientRegistrationId(),
                                    authToken.getName()
                            );

                            String realGoogleAccessToken = (client != null && client.getAccessToken() != null) ?
                                    client.getAccessToken().getTokenValue() : "dev-mock-token";

                            // Capture the true refresh token string sent back under offline visibility scope rules
                            String realGoogleRefreshToken = (client != null && client.getRefreshToken() != null) ?
                                    client.getRefreshToken().getTokenValue() : null;

                            OAuthToken tokenEntity = tokenRepository.findByUser(user).orElse(new OAuthToken());
                            tokenEntity.setUser(user);
                            tokenEntity.setAccessToken(realGoogleAccessToken);

                            // Only overwrite the refresh token record slot if Google explicitly drops a new active hash signature
                            if (realGoogleRefreshToken != null) {
                                tokenEntity.setRefreshToken(realGoogleRefreshToken);
                            }

                            tokenEntity.setExpiryDate(LocalDateTime.now().plusHours(1));
                            tokenRepository.save(tokenEntity);

                            String jwt = jwtProvider.generateToken(email);

                            // Securely hand control back to the active React dashboard view router
                            response.sendRedirect("http://localhost:5173/oauth2/callback?token=" + jwt);
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Programmatic customizer layer configuration utility method setup
    private DefaultOAuth2AuthorizationRequestResolver customAuthorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                this.clientRegistrationRepository,
                "/oauth2/authorization"
        );
        resolver.setAuthorizationRequestCustomizer(customizer -> customizer
                .additionalParameters(params -> {
                    params.put("access_type", "offline"); // Forces Google to surface long-lived background synchronization tokens
                    params.put("prompt", "consent");     // Intercepts account loops to guarantee the visibility check boxes are explicitly rendered
                })
        );
        return resolver;
    }
}