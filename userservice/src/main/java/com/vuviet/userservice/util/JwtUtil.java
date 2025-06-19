package com.vuviet.userservice.util;


import com.vuviet.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh.expiration")
    private  Long refreshExpiration;

    //Tạo key
    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    //Hàm generic để lấy bất kỳ claim nào tử token
    private Claims extractAllClaims(String token){
        try {
            return Jwts.parserBuilder()
                    .setSigningKey((getSigningKey()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.error("JWT parsing error: {}", e.getMessage());
            throw e;
        }
    }
    public <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        final Claims claims=extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    //Lấy username từ jwt token
    public String extractUsername(String token){
        return extractClaim(token,Claims::getSubject);
    }

    //Lấy thời gian hết hạn từ jwt
    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    //Lấy user id từ jwt token
    public long extractUserId(String token){
        return extractClaim(token,claims -> {
            Object userId=claims.get("userId");
            return userId!=null?Long.valueOf(userId.toString()):null;
        });
    }

    //Lấy role từ jwt token
    public String extractRole(String token){
        return extractClaim(token,claims -> (String) claims.get("role"));
    }

    //Lấy email từ jwt token
    public String extractEmail(String token){
        return extractClaim(token, claims -> (String) claims.get("email"));
    }

    //Kiểm tra đã hết hạn chưa
    public Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    //Tạo token
    private String createToken(Map<String, Object> claims, String subject){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) //username
                .setIssuedAt((new Date(System.currentTimeMillis())))
                .setExpiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.ES256)
                .compact();
    }

    //Tạo jwt token
    public String generateToken(UserDetails userDetails){
        Map<String, Object> claims=new HashMap<>();

        if(userDetails instanceof User){
            User user=(User) userDetails;
            claims.put("id", user.getId());
            claims.put("email",user.getEmail());
            claims.put("role",user.getRole().getName());
        }
        return createToken(claims,userDetails.getUsername());
    }

    public String generateToken(UserDetails userDetails,Map<String, Object> extractClaims){
        Map<String, Object> claims=new HashMap<>(extractClaims);

        if(userDetails instanceof User){
            User user=(User) userDetails;
            claims.put("id", user.getId());
            claims.put("email",user.getEmail());
            claims.put("role",user.getRole().getName());
        }
        return createToken(claims,userDetails.getUsername());
    }

    //Validation
    public Boolean validateToken(String token, UserDetails userDetails){
        try {
            final String username=extractUsername(token);
            return ((username.equals(userDetails.getUsername())) && !isTokenExpired(token));
        }catch (JwtException e){
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (JwtException | IllegalArgumentException e){
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateTokenWithRole(String token, String requiredRole){
        try {
            String tokenRole=extractRole(token);
            return (validateToken(token) && requiredRole.equals(tokenRole));
        }catch (Exception e){
            log.error("JWT role validation error: {}", e.getMessage());
            return false;
        }
    }

    //Refresh token
    public String generateRefreshToken(){
        return UUID.randomUUID().toString();
    }

    public Instant getRefreshTokenExpiry(){
        return Instant.now().plusMillis(refreshExpiration);
    }

    public boolean isRefreshTokenExpired(Instant expiry){
        return expiry.isBefore(Instant.now());
    }
}
