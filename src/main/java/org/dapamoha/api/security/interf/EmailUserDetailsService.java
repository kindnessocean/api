package org.dapamoha.api.security.interf;

import org.dapamoha.api.security.domain.EmailUserDetails;
import org.dapamoha.api.security.exception.EmailNotFoundAuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface EmailUserDetailsService extends UserDetailsService {
    EmailUserDetails loadUserByEmail(String email) throws EmailNotFoundAuthenticationException;
}
