package com.twocall.chat.security;

import com.twocall.chat.exception.UnauthorizedPairAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PairAccessValidator {

    public DevicePrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof DevicePrincipal principal) {
            return principal;
        }
        throw new UnauthorizedPairAccessException("No authenticated device context found");
    }

    public void validatePairAccess(UUID requestedPairId) {
        DevicePrincipal principal = getAuthenticatedPrincipal();
        if (requestedPairId == null || !principal.getPairId().equals(requestedPairId)) {
            throw new UnauthorizedPairAccessException("Access denied: Device does not belong to pair " + requestedPairId);
        }
    }
}
