package org.dapamoha.api.security;

import java.util.ArrayList;
import java.util.Collection;
import org.dapamoha.api.security.domain.EmailUserDetails;
import org.dapamoha.api.security.exception.EmailNotFoundAuthenticationException;
import org.dapamoha.api.security.interf.EmailUserDetailsService;
import org.dapamoha.shared.posgresql.entity.Email;
import org.dapamoha.shared.posgresql.entity.RoleType;
import org.dapamoha.shared.posgresql.entity.User;
import org.dapamoha.shared.posgresql.repository.email.EmailRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EmailUserDetailsServiceImpl implements EmailUserDetailsService {

    private final EmailRepository emailRepository;

    public EmailUserDetailsServiceImpl(final EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }


    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        throw new UnsupportedOperationException("This implementation didn't implement this method");
    }

    @Override
    public EmailUserDetails loadUserByEmail(final String email) throws EmailNotFoundAuthenticationException {
        Email result = emailRepository.getEmailByAddress(email);

        if (result == null) {
            throw new UsernameNotFoundException("Address: " + result + " not found");
        }


        if (result.getUser() != null) {
            return new EmailUserDetails(
                    result.getUser().getUsername(),
                    email,
                    getGrantedAuthorities(result.getUser())
            );
        } else {
            return new EmailUserDetails(
                    null,
                    email,
                    getDefaultGrantedAuthorities()
            );
        }
    }

    private Collection<GrantedAuthority> getGrantedAuthorities(User user) {

        Collection<GrantedAuthority> grantedAuthority = new ArrayList<>();

        user.getRoles().forEach(role -> grantedAuthority.add(new SimpleGrantedAuthority(role.getRole().getValue())));

        return grantedAuthority;
    }

    private Collection<GrantedAuthority> getDefaultGrantedAuthorities() {

        Collection<GrantedAuthority> grantedAuthority = new ArrayList<>();

        grantedAuthority.add(new SimpleGrantedAuthority(RoleType.UncompletedUser.getValue()));

        return grantedAuthority;
    }
}