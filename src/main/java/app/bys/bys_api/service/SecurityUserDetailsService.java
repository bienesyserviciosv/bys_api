package app.bys.bys_api.service;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        return finalUserRepository.findByEmail(email)
                .map(this::buildUserDetails)
                .or(() -> serviceProviderRepository.findByEmail(email).map(this::buildUserDetails))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    private UserDetails buildUserDetails(Object userEntity) {
        String email;
        String password;
        Set<Role> roles;

        if (userEntity instanceof FinalUser) {
            FinalUser user = (FinalUser) userEntity;
            email = user.getEmail();
            password = user.getPassword();
            roles = user.getRoles();
        } else if (userEntity instanceof ServiceProvider) {
            ServiceProvider provider = (ServiceProvider) userEntity;
            email = provider.getEmail();
            password = provider.getPassword();
            roles = provider.getRoles();
        } else {
            throw new IllegalStateException("Unknown entity for authentication");
        }

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(email, password, authorities);
    }
}
