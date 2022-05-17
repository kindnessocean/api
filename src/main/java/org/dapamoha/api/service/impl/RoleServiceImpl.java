package org.dapamoha.api.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dapamoha.api.service.interf.RoleService;
import org.dapamoha.shared.posgresql.entity.Role;
import org.dapamoha.shared.posgresql.entity.RoleType;
import org.dapamoha.shared.posgresql.repository.role.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final List<RoleType> DEFAULT_ROLE_TYPES = Arrays.asList(RoleType.UncompletedUser, RoleType.User);

    private final RoleRepository roleRepository;

    public RoleServiceImpl(final RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Set<Role> getDefaultRoles() {
        Set<Role> roles = new HashSet<>();

        DEFAULT_ROLE_TYPES.forEach(roleType -> roles.add(getOrCreate(roleType)));

        return roles;
    }

    private Role getOrCreate(final RoleType roleType) {
        Role role = roleRepository.getRoleByRoleType(roleType);

        if (role != null) {
            return role;
        }

        return roleRepository.create(roleType);
    }
}