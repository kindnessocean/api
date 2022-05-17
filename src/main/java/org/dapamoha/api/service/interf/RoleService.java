package org.dapamoha.api.service.interf;

import java.util.Set;
import org.dapamoha.shared.posgresql.entity.Role;

public interface RoleService {
    Set<Role> getDefaultRoles();
}
