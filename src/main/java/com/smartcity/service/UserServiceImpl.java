package com.smartcity.service;

import com.smartcity.dao.RoleDao;
import com.smartcity.dao.UserDao;
import com.smartcity.domain.Role;
import com.smartcity.domain.User;
import com.smartcity.dto.UserDto;
import com.smartcity.mapperDto.UserDtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private UserDtoMapper userDtoMapper;
    private UserDao userDao;
    private RoleDao roleDao;

    @Autowired
    public UserServiceImpl(UserDao userDao, UserDtoMapper userDtoMapper, RoleDao roleDao) {
        this.userDao = userDao;
        this.userDtoMapper = userDtoMapper;
        this.roleDao = roleDao;
    }

    @Override
    public UserDto create(UserDto userDto) {
        User user = userDtoMapper.convertUserDtoIntoUser(userDto);
        return userDtoMapper.convertUserIntoUserDto(userDao.create(user));
    }

    @Override
    public UserDto findById(Long id) {
        return userDtoMapper.convertUserIntoUserDto(userDao.findById(id));
    }

    @Override
    public List<UserDto> getAll() {
        List<User> users = userDao.findAll();
        return users.stream().
                map(userDtoMapper::convertUserIntoUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto findByEmail(String email) {
        return userDtoMapper.convertUserIntoUserDto(userDao.findByEmail(email));
    }

    @Override
    public UserDto update(UserDto userDto) {
        User user = userDtoMapper.convertUserDtoIntoUser(userDto);
        return userDtoMapper.convertUserIntoUserDto(userDao.update(user));
    }

    @Override
    public boolean delete(Long id) {
        return userDao.delete(id);
    }

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.findByEmail(username);
        List<Role> rolesByUserId = roleDao.getRolesByUserId(user.getId());
        user.setAuthorities(rolesByUserId);
        return user;
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        return userDao.updatePassword(userId, newPassword);
    }

    @Override
    public List<Role> getRoles(Long id) {
        return roleDao.getRolesByUserId(id);
    }

    @Override
    public boolean setRoles(Long userId, List<Role> roles) {
        List<Role> existingRoles = roleDao.findAll();

        List<Role> currentRoles = roleDao.getRolesByUserId(userId);

        // Adding new roles
        for (Role role : roles) {
            if (!currentRoles.contains(role) && existingRoles.contains(role)) {
                roleDao.addRoleToUser(userId, role.getId());
            }
        }

        // Removing non-actual roles
        for (Role role : currentRoles) {
            if (!roles.contains(role)) {
                roleDao.removeRoleFromUser(userId, role.getId());
            }
        }

        return true;
    }
}