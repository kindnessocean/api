package org.dapamoha.api.service.interf;

import org.dapamoha.shared.domain.service.AuthenticationUser;
import org.dapamoha.shared.posgresql.entity.EmailCode;

public interface AuthService {

    EmailCode sendCodeToEmail(String address);

    AuthenticationUser requestJwtToken(String emailAddress, Integer code);
}
