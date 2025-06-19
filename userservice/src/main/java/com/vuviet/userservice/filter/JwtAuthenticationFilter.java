package com.vuviet.userservice.filter;

import com.vuviet.userservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Lấy Authorization header
        final String authorizationHeader=request.getHeader("Authorization");

        String username=null;
        String jwt=null;

        //Kiểm tra Bearer Token
        if(authorizationHeader!=null && authorizationHeader.startsWith("Bearer")){
            jwt=authorizationHeader.substring(7);
            try {
                username=jwtUtil.extractUsername(jwt);
            }catch (Exception e){
                log.error("JWT Token parsing error: {}", e.getMessage());
            }
        }

        //Nếu có username và chưa authenticate
        if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null){
            try{
                if(jwtUtil.validateToken(jwt)){
                    //Lấy role từ jwt
                    String role=jwtUtil.extractRole(jwt);

                    //Tạo authorities từ role
                    List<SimpleGrantedAuthority> authorities=List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    //Tạo authentication token
                    UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(username,null,authorities);

                    //Set authentication vaò SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                }
            }catch (Exception e){
                log.error("Cannot set user authentication: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request,response);
    }
}
