package com.vuviet.userservice.controller;

import com.vuviet.userservice.entity.User;
import com.vuviet.userservice.entity.request.LoginDto;
import com.vuviet.userservice.entity.request.RegisterDto;
import com.vuviet.userservice.entity.response.JwtResponse;
import com.vuviet.userservice.entity.response.UserResponse;
import com.vuviet.userservice.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController

@Slf4j
public class AuthController {
    private final AuthService authService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDto registerDto){
        try {
            String result= authService.register(registerDto);
            log.info("Register successful for user: {}",registerDto.getUsername());
            return ResponseEntity.ok(result);
        }catch (RuntimeException e){
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response){
        try{
            JwtResponse jwtResponse=authService.login(loginDto);

            Cookie accessTokenCookie=createAccessTokenCookie(jwtResponse.getAccessToken());
            response.addCookie(accessTokenCookie);

            Cookie refreshTokenCookie=createRefreshTokenCookie(jwtResponse.getRefreshToken());
            response.addCookie(refreshTokenCookie);

            log.info("Login successful for user: {} with cookie",loginDto.getUsername());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e){
            log.error("Login failed for user {}: {}", loginDto.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(HttpServletRequest request, HttpServletResponse response){
        try{
            //Lấy refresh token từ cookie
            String refreshToken=getRefreshTokenFromCookie(request);
            if(refreshToken==null){
                log.error("No refresh token found in cookies");
                return ResponseEntity.badRequest().build();
            }

            JwtResponse jwtResponse=authService.refreshToken(refreshToken);

            Cookie accessTokenCookie=createAccessTokenCookie(jwtResponse.getAccessToken());
            response.addCookie(accessTokenCookie);

            log.info("Token refresh successful with cookies");
            return ResponseEntity.ok(jwtResponse);
        }catch (RuntimeException e){
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response){
        try{
            String refreshToken=getRefreshTokenFromCookie(request);
            if(refreshToken!=null){
                authService.logout(refreshToken);
            }

            clearAuthCookies(response);

            log.info("Logout successful with cookies cleared");
            return ResponseEntity.ok("Logged out successfully");
        }catch (RuntimeException e){
            log.error("Logout failed: {}", e.getMessage());
            // Vẫn xóa cookies dù có lỗi
            clearAuthCookies(response);
            return ResponseEntity.badRequest().body(e.getMessage());

        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user){
        if(user == null){
            return ResponseEntity.badRequest().build();
        }

        UserResponse response=new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName(),
                user.getIsActive(),
                user.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

    //Tạo cookie cho access token
    private Cookie createAccessTokenCookie(String accessToken){
        Cookie cookie=new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration/1000));
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    //Tạo cookie cho refresh token
    private Cookie createRefreshTokenCookie(String refreshToken){
        Cookie cookie=new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration/1000));
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    //Lấy refresh token từ cookie
    private String getRefreshTokenFromCookie(HttpServletRequest request){
        if(request.getCookies()!=null){
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String getAccessTokenFromCookie(HttpServletRequest request){
        if(request.getCookies()!=null){
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    //Xóa tất cả auth cookies
    private void clearAuthCookies(HttpServletResponse response){
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // Xóa ngay lập tức
        response.addCookie(accessTokenCookie);

        // Xóa refresh token cookie
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // Xóa ngay lập tức
        response.addCookie(refreshTokenCookie);

    }
}
