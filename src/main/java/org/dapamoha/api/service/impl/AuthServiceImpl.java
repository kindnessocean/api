package org.dapamoha.api.service.impl;

import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.dapamoha.api.security.jwt.JwtTokenProvider;
import org.dapamoha.api.service.interf.AuthService;
import org.dapamoha.api.service.interf.EmailService;
import org.dapamoha.api.service.interf.UserService;
import org.dapamoha.shared.domain.exception.IllegalArgAppException;
import org.dapamoha.shared.domain.service.AuthenticationUser;
import org.dapamoha.shared.posgresql.entity.Email;
import org.dapamoha.shared.posgresql.entity.EmailCode;
import org.dapamoha.shared.posgresql.entity.Role;
import org.dapamoha.shared.posgresql.entity.RoleType;
import org.dapamoha.shared.posgresql.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${org.dapamoha.api.auth.token.validityTime}")
    private Long tokenValidityInMs;

    public AuthServiceImpl(
            final UserService userService,
            final EmailService emailService,
            final JwtTokenProvider jwtTokenProvider
    ) {
        this.userService = userService;
        this.emailService = emailService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    private AuthenticationUser auth(final String address, final Set<RoleType> roles) {
        String token = jwtTokenProvider.createToken(address, roles);

        return AuthenticationUser.builder()
                .tokenValidityInMs(tokenValidityInMs)
                .token(token)
                .build();
    }

    @Override
    public EmailCode sendCodeToEmail(final String address) {
        Email email;

        if (emailService.isExistEmailByAddress(address)){
            email = emailService.getEmailByAddress(address);
        } else {
            email = emailService.create(address);
        }

        return emailService.sendEmailCode(email);
    }

    @Override
    public AuthenticationUser requestJwtToken(final String email, final Integer code) {
        EmailCode emailCode = emailService.getEmailCodeByAddress(email);

        if (emailCode == null || !Objects.equals(emailCode.getCode(), code)) {
            throw new IllegalArgAppException("That code " + code + " didn't send to " + email);
        }

        final long expiredAtTime = emailCode.getExpiredAt().getTime();

        // Delete all letter to this email address
        emailService.expireEmailCode(emailCode);

        if (new Date().getTime() >= expiredAtTime) {
            throw new IllegalArgAppException("That code " + code + " expired");
        }

        if (userService.isExistUserByEmailAddress(email)){

            User user = userService.getUserByEmailAddress(email);

            return auth(
                    email,
                    user.getRoles().stream().map(Role::getRole).collect(Collectors.toSet())
            );
        }

        return auth(email, Set.of(RoleType.UncompletedUser));
    }
}