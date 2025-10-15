package com.agileo.AGILEO.security;

import com.agileo.AGILEO.entity.secondary.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomJwtAuthenticationToken extends JwtAuthenticationToken {
    private final User user;

    public CustomJwtAuthenticationToken(Jwt jwt, Collection<GrantedAuthority> authorities, String login, User user) {
        super(jwt, authorities, login);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        if (user != null && user.getRoles() != null) {
            Set<GrantedAuthority> authorities = new HashSet<>();
            authorities.addAll(super.getAuthorities());

            user.getRoles().forEach(role -> {
                String roleAuthority = "ROLE_" + role.getName().toUpperCase();
                authorities.add(new SimpleGrantedAuthority(roleAuthority));
                authorities.add(new SimpleGrantedAuthority(role.getName().toUpperCase()));
            });

            return authorities;
        }
        return super.getAuthorities();
    }

    @Override
    public String getName() {
        return user != null ? user.getLogin() : super.getName();
    }
}