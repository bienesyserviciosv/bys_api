package app.bys.bys_api.config;

import app.bys.bys_api.error.ErrorResponseDto;
import app.bys.bys_api.security.filter.JwtAuthenticationFilter;
import app.bys.bys_api.security.handler.CustomAccessDeniedHandler;
import app.bys.bys_api.security.handler.CustomAuthenticationEntryPoint;
import app.bys.bys_api.security.oauth2.UserSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserSuccessHandler userSuccessHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                   JwtAuthenticationFilter jwtFilter) throws Exception {
        return httpSecurity
                .cors(httpSecurityCorsConfigurer ->
                        httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/auth/**", "/login/oauth2/**", "/oauth2/**").permitAll()
                                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")
                                .requestMatchers("/swagger-ui/**").permitAll()
                                .requestMatchers("/v3/api-docs*/**").permitAll()
                                .requestMatchers("/specialization", "/province", "/fcm-test", "/fcm/**").permitAll()
                                .requestMatchers("/api/v1/fcm/**").permitAll()
                                .requestMatchers("/bancamiga/webhook").permitAll()
                                .requestMatchers("/firebase-messaging-sw.js", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                                .anyRequest().authenticated()
                )
                .with(new ExceptionHandlingConfigurer<>(), exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
                )

                .oauth2Login(oauth -> oauth
                        .successHandler(userSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ErrorResponseDto.builder()
                                            .status(401)
                                            .error("Unauthorized")
                                            .message("Authentication required")
                                            .path(request.getRequestURI())
                                            .timestamp(LocalDateTime.now())
                                            .build()
                            ));
                        })
                )

                .csrf(AbstractHttpConfigurer::disable)
                .headers(a -> a.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS with allowed origins: localhost:5173, localhost:63342, localhost:8080, api-dev.bienesyservicios.app, admin.bienesyservicios.app");

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:63342",
                "http://localhost:8080",
                "https://api-dev.soytubys.com",
                "https://admin.soytubys.com"
        ));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("CORS configuration completed");
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception { return authenticationConfiguration.getAuthenticationManager(); }

//    @Bean
//    public AuthenticationSuccessHandler compositeSuccessHandler() {
//        log.info("aaaaaaaAAAAAAAAAAaaaaaaaaAAA");
//        return  new UserSuccessHandler(finalUserService, jwtUtil);
//            if (uri.contains("google-user")) {
//                userSuccessHandler.onAuthenticationSuccess(request, response, authentication);
//            } else if (uri.contains("google-provider")) {
//                providerSuccessHandler.onAuthenticationSuccess(request, response, authentication);
//            } else {
//                log.warn("No matching URI in handler: " + uri);
//          }
//        };
    }


