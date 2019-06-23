package com.smartcity.service;

import com.smartcity.dto.RoleDto;
import com.smartcity.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface UserService {

    UserDto create(UserDto user);

    UserDto findById(Long id);

    List<UserDto> findAll();

    UserDto findByEmail(String email);

    List<UserDto> findByOrganizationId(Long organizationId);

    List<UserDto> findByRoleId(Long rolesIds);

    UserDto update(UserDto user);

    boolean delete(Long id);

    boolean activate(Long id);

    boolean updatePassword(Long userId, String newPassword);

    List<RoleDto> getRoles(Long id);

    boolean setRoles(Long userId, List<Long> newRolesIds);
}
