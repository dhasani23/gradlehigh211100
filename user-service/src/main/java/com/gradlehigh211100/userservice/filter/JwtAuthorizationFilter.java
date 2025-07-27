package com.gradlehigh211100.userservice.filter;

import com.gradlehigh211100.userservice.service.JwtService;
import com.gradlehigh211100.userservice.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that checks for valid JWT token in the request and sets up the
 * Spring Security context if the token is valid.
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, 
                                  JwtService jwtService,
                                  UserService userService) {
        super(authenticationManager);
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Main filter method that checks for and validates JWT tokens.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        chain.doFilter(request, response);
    }

    /**
     * Parses the JWT token from the request and creates an authentication token if valid.
     */
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.replace("Bearer ", "");
            
            if (jwtService.validateToken(token)) {
                String username = jwtService.getUsernameFromToken(token);
                if (username != null) {
                    // Get roles from token
                    List<String> roles = jwtService.getRolesFromToken(token);
                    List<GrantedAuthority> authorities = roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
                    
                    return new UsernamePasswordAuthenticationToken(username, null, authorities);
                }
            }
        }
        
        return null;
    }
}