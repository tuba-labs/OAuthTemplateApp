package org.tubalabs.app.users.password.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalSessionAuthentication {

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    private final LocalUserDetailsService localUserDetailsService;

    public void authenticate(@NonNull String email,
                             @NonNull HttpServletRequest servletRequest,
                             @NonNull HttpServletResponse servletResponse) {
        final UserDetails userDetails = localUserDetailsService.loadUserByUsername(email);
        final UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, servletRequest, servletResponse);
    }
}
