package com.vuviet.userservice.service;

import com.vuviet.userservice.entity.Role;
import com.vuviet.userservice.entity.User;
import com.vuviet.userservice.entity.request.LoginDto;
import com.vuviet.userservice.entity.request.RegisterDto;
import com.vuviet.userservice.entity.response.JwtResponse;
import com.vuviet.userservice.repository.RoleRepository;
import com.vuviet.userservice.repository.UserRepository;
import com.vuviet.userservice.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    String register(RegisterDto registerDto);

    JwtResponse login(LoginDto loginDto);

    JwtResponse refreshToken(String refreshToken);

    void logout(String refreshToken);
}

@Service
@Slf4j
@Transactional
class AuthServiceImpl implements AuthService{
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String register(RegisterDto registerDto) {
        if(userRepository.existsByUsername(registerDto.getUsername())){
            throw new RuntimeException(("User already exists"));
        }
        if(userRepository.existsByEmail(registerDto.getEmail())){
            throw new RuntimeException(("Email already exists"));
        }

        Role studientRole=roleRepository.findByName("STUDENT")
                .orElseThrow(()->new RuntimeException("Default role not found"));

        User user=new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setEmail(registerDto.getEmail());
        user.setFullName(registerDto.getFullName());
        user.setRole(studientRole);
        user.setIsActive(true);

        userRepository.save(user);
        log.info("User register successfully:{}",registerDto.getUsername());

        return "User registered successfully";
    }

    @Override
    public JwtResponse login(LoginDto loginDto) {
        Authentication authentication=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        User user=(User) authentication.getPrincipal();

        String accessToken=jwtUtil.generateToken(user);

        String refreshToken= jwtUtil.generateRefreshToken();

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(jwtUtil.getRefreshTokenExpiry());
        userRepository.save(user);

        log.info("User logged in successfully:{}",user.getUsername());

        return new JwtResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName()
        );
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        User user=userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(()->new RuntimeException("Invalid refresh token"));

        if(jwtUtil.isRefreshTokenExpired(user.getRefreshTokenExpiry())){
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Refresh token expired");
        }

        String newAccessToken= jwtUtil.generateToken(user);
        log.info("Token refresh for user: {}", user.getUsername());
        return new JwtResponse(
                newAccessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName()
        );
    }

    @Override
    public void logout(String refreshToken) {
        User user=userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(()->new RuntimeException("Invalid refresh token"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
        log.info("User logged out successfully:{}", user.getUsername());
    }
}
