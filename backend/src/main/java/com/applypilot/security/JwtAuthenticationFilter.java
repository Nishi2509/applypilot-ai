package com.applypilot.security;

import io.jsonwebtoken.JwtException; import jakarta.servlet.*; import jakarta.servlet.http.*; import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt; public JwtAuthenticationFilter(JwtService jwt) { this.jwt = jwt; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String value = request.getHeader("Authorization");
        if (value != null && value.startsWith("Bearer ")) try { var authentication = new UsernamePasswordAuthenticationToken(jwt.userId(value.substring(7)), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))); SecurityContextHolder.getContext().setAuthentication(authentication); } catch (JwtException | IllegalArgumentException ignored) { }
        chain.doFilter(request, response);
    }
}
