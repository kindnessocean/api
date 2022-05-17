package org.dapamoha.api.service.interf;

import org.dapamoha.api.security.domain.EmailUserDetails;
import org.dapamoha.shared.posgresql.entity.User;

public interface UserService {

    User signUp(EmailUserDetails emailUserDetails, String firstName, String lastName, String username);

    User getUserByEmailAddress(String address);

    boolean isExistUserByEmailAddress(String address);
}
