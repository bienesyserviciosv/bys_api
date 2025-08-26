package app.bys.bys_api.security.oauth2;

import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.service.ServiceProviderService;
import app.bys.bys_api.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProviderSuccessHandler implements AuthenticationSuccessHandler {

    private final ServiceProviderService serviceProviderService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String email = googleUser.getAttribute("email");
        String name = googleUser.getAttribute("name");

        ServiceProvider provider = serviceProviderService.findOrCreateProvider(email, name);

        List<GrantedAuthority> authorities = provider.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        String jwt = jwtUtil.generateToken(provider.getEmail(), authorities);

        response.setContentType("application/json");
//        response.getWriter().write(new ObjectMapper().writeValueAsString(
//                new AuthResponseDto(jwt, provider.getEmail(), provider.getRoles()))
//        );
    }

}
