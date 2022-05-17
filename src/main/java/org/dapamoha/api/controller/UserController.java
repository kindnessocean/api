package org.dapamoha.api.controller;

import java.util.UUID;
import org.dapamoha.api.security.domain.EmailUserDetails;
import org.dapamoha.api.service.interf.AuthService;
import org.dapamoha.api.service.interf.UserService;
import org.dapamoha.shared.domain.dto.user.JwtTokenDto;
import org.dapamoha.shared.domain.dto.user.SendCodeToEmailDto;
import org.dapamoha.shared.domain.dto.user.SendCodeToEmailResultDto;
import org.dapamoha.shared.domain.dto.user.RegistrationFormDto;
import org.dapamoha.shared.domain.dto.user.UserPrivateProfileDto;
import org.dapamoha.shared.domain.service.AuthenticationUser;
import org.dapamoha.shared.domain.util.UuidUtil;
import org.dapamoha.shared.posgresql.entity.EmailCode;
import org.dapamoha.shared.posgresql.entity.User;
import org.dapamoha.shared.posgresql.util.ConverterSqlEntityToDtoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    private final UuidUtil uuidUtil;

    public UserController(
            final UserService userService,
            final AuthService authService,
            final UuidUtil uuidUtil
    ) {
        this.userService = userService;
        this.authService = authService;
        this.uuidUtil = uuidUtil;
    }

    @PostMapping("/registration")
    public ResponseEntity<UserPrivateProfileDto> registration(
            @AuthenticationPrincipal EmailUserDetails emailUserDetails,
            @RequestBody RegistrationFormDto form
    ) {

        User user = userService.signUp(emailUserDetails,
                form.getFirstName(), form.getLastName(), form.getUsername());

        return new ResponseEntity<UserPrivateProfileDto>(
                ConverterSqlEntityToDtoUtil.convert(user),
                HttpStatus.OK
        );
    }

    @GetMapping
    public ResponseEntity<UserPrivateProfileDto> getUserPrivateProfileDto(
            @AuthenticationPrincipal EmailUserDetails emailUserDetails
    ){
        return new ResponseEntity<>(
                ConverterSqlEntityToDtoUtil.convert(userService.getUserByEmailAddress(emailUserDetails.getEmail())),
                HttpStatus.OK
        );
    }

    @PostMapping("/sendCodeToEmail")
    public ResponseEntity<SendCodeToEmailResultDto> sendCodeToEmail(
            @RequestBody SendCodeToEmailDto form
    ) {

        UUID uuid = uuidUtil.randomUUID();

        authService.sendCodeToEmail(form.getEmail());

        return new ResponseEntity<>(
                SendCodeToEmailResultDto
                        .builder()
                        .id(uuid.toString())
                        .build(),
                HttpStatus.OK);
    }

    @GetMapping("/signIn")
    public ResponseEntity<JwtTokenDto> signIn(
            @RequestParam Integer code,
            @RequestParam String address
    ) {

        AuthenticationUser result = authService.requestJwtToken(address, code);

        return new ResponseEntity<>(
                JwtTokenDto
                        .builder()
                        .token(result.getToken())
                        .tokenValidityInMs(result.getTokenValidityInMs())
                        .build(),
                HttpStatus.OK
        );
    }
}