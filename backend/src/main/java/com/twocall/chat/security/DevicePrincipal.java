package com.twocall.chat.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class DevicePrincipal implements UserDetails {

    private final UUID deviceId;
    private final UUID pairId;
    private final String fingerprint;

    public DevicePrincipal(UUID deviceId, UUID pairId, String fingerprint) {
        this.deviceId = deviceId;
        this.pairId = pairId;
        this.fingerprint = fingerprint;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public UUID getPairId() {
        return pairId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE"));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return deviceId.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
