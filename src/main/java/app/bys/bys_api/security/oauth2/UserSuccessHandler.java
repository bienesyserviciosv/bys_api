package app.bys.bys_api.security.oauth2;

import app.bys.bys_api.model.dto.AuthResponseDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.service.FinalUserService;
import app.bys.bys_api.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class UserSuccessHandler implements AuthenticationSuccessHandler {

    private final FinalUserService finalUserService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("Entrando al AuthenticationSuccessHandler");

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();
        String email = googleUser.getAttribute("email");
        String name = googleUser.getAttribute("name");

        log.info("OAuth2 login successful");

        FinalUser user = finalUserService.findOrCreateUser(email, name);

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        String jwt = jwtUtil.generateToken(user.getEmail(), authorities);

        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(
                new AuthResponseDto(jwt, user.getEmail()))
        );
      //  response.sendRedirect("http://localhost:3000/token?jwt=" + jwt);
    }
}
