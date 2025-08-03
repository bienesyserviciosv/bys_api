package app.bys.bys_api.config;

import app.bys.bys_api.security.filter.JwtAuthenticationFilter;
import app.bys.bys_api.service.FinalUserService;
import app.bys.bys_api.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final AuthenticationSuccessHandler userSuccessHandler;
    private final AuthenticationSuccessHandler providerSuccessHandler;
    private final FinalUserService finalUserService;
    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                   JwtAuthenticationFilter jwtFilter) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers("/auth/**", "/oauth2/**").permitAll()
                                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
                                .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        //.defaultSuccessUrl("http://localhost:3000/token", true)
                        .successHandler(compositeSuccessHandler())
                )

                .csrf(AbstractHttpConfigurer::disable)
                .headers(a -> a.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception { return authenticationConfiguration.getAuthenticationManager(); }

    @Bean
    public AuthenticationSuccessHandler compositeSuccessHandler() {
        return (request, response, authentication) -> {
            String uri = request.getRequestURI();
            log.info("Redirect URI after OAuth2: " + uri);
            userSuccessHandler.onAuthenticationSuccess(request, response, authentication);
//            if (uri.contains("google-user")) {
//                userSuccessHandler.onAuthenticationSuccess(request, response, authentication);
//            } else if (uri.contains("google-provider")) {
//                providerSuccessHandler.onAuthenticationSuccess(request, response, authentication);
//            } else {
//                log.warn("No matching URI in handler: " + uri);
                response.sendRedirect("/login?error");
          //  }
        };
    }


}
