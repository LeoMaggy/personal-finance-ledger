package com.mahesh.expenses.security;

import com.mahesh.expenses.common.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtCurrentUser implements CurrentUser {
    @Override
    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return Long.valueOf(jwtAuthenticationToken.getToken().getSubject());
        }
        //only reachable if a public endpoint calls a service that needs a user: a programming error
        throw new IllegalStateException("No authenticated user for this request");
    }
}
