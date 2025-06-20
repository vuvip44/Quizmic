package com.vuviet.userservice.service;

import com.vuviet.userservice.entity.Role;
import com.vuviet.userservice.entity.User;
import com.vuviet.userservice.entity.request.CreateUserDto;
import com.vuviet.userservice.entity.request.UpdateProfileDto;
import com.vuviet.userservice.entity.request.UpdateRoleDto;
import com.vuviet.userservice.entity.response.UserResponse;
import com.vuviet.userservice.repository.RoleRepository;
import com.vuviet.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    Page<UserResponse> getAllUsers(Specification<User> spec, Pageable pageable);

    Page<UserResponse> getAllUsers(Pageable pageable);

    void deactivateUser(long userId);

    void activeUser(long userId);

    UserResponse UpdateProfile(String username, UpdateProfileDto updateProfileDto);

    UserResponse CreateUser(CreateUserDto createUserDto);

    UserResponse updateUserRole(long userId, UpdateRoleDto updateRoleDto);
}

@Slf4j
@Service
@Transactional(readOnly = true)
class UserServiceImpl implements UserService{
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public Optional<User> findById(long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Page<UserResponse> getAllUsers(Specification<User> spec, Pageable pageable) {
        Page<User> userPage=userRepository.findAll(spec,pageable);
        return userPage.map(this::covertToUserResponse);
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage=userRepository.findAll(pageable);
        return userPage.map(this::covertToUserResponse);
    }

    @Override
    public void deactivateUser(long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void activeUser(long userId) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse UpdateProfile(String username, UpdateProfileDto updateProfileDto) {
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->new RuntimeException("User not found"));
        boolean needReLogin=false;

        if(updateProfileDto.getEmail()!=null && !updateProfileDto.getEmail().trim().isEmpty()){
            if(!updateProfileDto.getEmail().equals((user.getEmail()))){
                if(userRepository.existsByEmail(updateProfileDto.getEmail())){
                    throw new RuntimeException("Email already exists");
                }
            }
        }

        if(updateProfileDto.getPassword()!=null && !updateProfileDto.getPassword().trim().isEmpty()){
            user.setPassword(passwordEncoder.encode(updateProfileDto.getPassword()));
            needReLogin=true;
        }

        if(updateProfileDto.getFullName()!=null && !updateProfileDto.getFullName().trim().isEmpty()){
            user.setFullName(updateProfileDto.getFullName());
        }

        User savedUser=userRepository.save(user);

        if(needReLogin){
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        }

        log.info("User {} updated profile", username);
        return covertToUserResponse(savedUser);
    }

    @Override
    public UserResponse CreateUser(CreateUserDto createUserDto) {
        if(userRepository.existsByEmail(createUserDto.getEmail())){
            throw new RuntimeException("Email already exists");
        }

        if(userRepository.existsByUsername(createUserDto.getUsername())){
            throw new RuntimeException(("Username already exists"));
        }

        Role role=roleRepository.findByName(createUserDto.getRoleName())
                .orElseThrow(()->new RuntimeException("Role not found: "+createUserDto.getRoleName()));

        User user=new User();
        user.setUsername(createUserDto.getUsername());
        user.setEmail(createUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        user.setRole(role);
        user.setFullName(createUserDto.getFullName());
        user.setIsActive(true);

        User savedUser=userRepository.save(user);

        log.info("Admin created new user: {} with role: {}",createUserDto.getUsername(),createUserDto.getRoleName());

        return covertToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(long userId, UpdateRoleDto updateRoleDto) {
        User user=userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("User not found"));

        Role newRole=roleRepository.findByName(updateRoleDto.getRoleName())
                .orElseThrow(()->new RuntimeException("Role not found"));

        String oldRole=user.getRole().getName();
        user.setRole(newRole);

        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

        User savedUser=userRepository.save(user);

        log.info("Admin changed user {} role from {} to {}", user.getUsername(), oldRole, newRole);
        return covertToUserResponse(savedUser);
    }

    private UserResponse covertToUserResponse(User user){
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
