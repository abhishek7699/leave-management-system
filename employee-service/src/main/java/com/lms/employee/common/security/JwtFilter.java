package com.lms.employee.common.security;

import com.lms.employee.infrastructure.redis.JwtBlacklistService;
import com.lms.employee.infrastructure.persistence.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtBlacklistService blacklistService;
    private final EmployeeRepository employeeRepository;

    public JwtFilter(JwtService jwtService,
                     JwtBlacklistService blacklistService,
                     EmployeeRepository employeeRepository) {
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (blacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                employeeRepository.findByEmail(email).ifPresent(employee -> {
                    if (jwtService.isTokenValid(token, email)) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                employee,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        } catch (Exception ignored) {
            // invalid token — leave SecurityContext empty, Spring Security will reject
        }

        filterChain.doFilter(request, response);
    }
}
