package org.dapamoha.api.testUtil;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dapamoha.shared.domain.util.StringUtil;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeneratorUtil {

    public static String generateEmail(long usernameLength, String dns) {
        return StringUtil.generateString(usernameLength) + "@" + dns;
    }
}
