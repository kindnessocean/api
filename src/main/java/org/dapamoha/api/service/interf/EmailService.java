package org.dapamoha.api.service.interf;

import org.dapamoha.api.security.domain.EmailUserDetails;
import org.dapamoha.shared.posgresql.entity.Email;
import org.dapamoha.shared.posgresql.entity.EmailCode;

public interface EmailService {
    Email getEmailByAddress(String address);

    EmailCode sendEmailCode(Email email);

    EmailCode getEmailCodeByAddress(String address);

    void expireEmailCode(EmailCode emailCode);

    Email getEmailByEmailUserDetails(EmailUserDetails emailUserDetails);

    Email update(Email email);

    Email create(String address);

    Boolean isExistEmailByAddress(String address);
}
