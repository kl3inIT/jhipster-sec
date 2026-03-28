package com.vn.core.config;

import static com.vn.core.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.vn.core.security.SecurityUtils.JWT_ALGORITHM;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import com.vn.core.management.SecurityMetersService;
import com.vn.core.security.AcceptsGrantedAuthorities;
import com.vn.core.security.DomainUserDetailsService;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class SecurityJwtConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityJwtConfiguration.class);

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public JwtDecoder jwtDecoder(SecurityMetersService metersService) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey()).macAlgorithm(JWT_ALGORITHM).build();
        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                if (e.getMessage().contains("Invalid signature")) {
                    metersService.trackTokenInvalidSignature();
                } else if (e.getMessage().contains("Jwt expired at")) {
                    metersService.trackTokenExpired();
                } else if (
                    e.getMessage().contains("Invalid JWT serialization") ||
                    e.getMessage().contains("Malformed token") ||
                    e.getMessage().contains("Invalid unsecured/JWS/JWE")
                ) {
                    metersService.trackTokenMalformed();
                } else {
                    LOG.error("Unknown JWT error {}", e.getMessage());
                }
                throw e;
            }
        };
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
        ObjectProvider<DomainUserDetailsService> userDetailsServiceProvider
    ) {
        JwtGrantedAuthoritiesConverter fallbackAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        fallbackAuthoritiesConverter.setAuthorityPrefix("");
        fallbackAuthoritiesConverter.setAuthoritiesClaimName(AUTHORITIES_CLAIM);

        return jwt -> {
            DomainUserDetailsService userDetailsService = userDetailsServiceProvider.getIfAvailable();
            if (userDetailsService == null) {
                return new JwtAuthenticationToken(jwt, fallbackAuthoritiesConverter.convert(jwt), jwt.getSubject());
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(jwt.getSubject());
            if (userDetails instanceof AcceptsGrantedAuthorities acceptsGrantedAuthorities) {
                acceptsGrantedAuthorities.setGrantedAuthorities(userDetails.getAuthorities());
            }

            return new RefreshedJwtAuthenticationToken(jwt, userDetails, userDetails.getAuthorities());
        };
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    private static final class RefreshedJwtAuthenticationToken extends JwtAuthenticationToken {

        private final Object principal;
        private final String name;

        private RefreshedJwtAuthenticationToken(Jwt jwt, Object principal, java.util.Collection<? extends GrantedAuthority> authorities) {
            super(jwt, authorities, principal instanceof UserDetails userDetails ? userDetails.getUsername() : jwt.getSubject());
            this.principal = principal;
            this.name = principal instanceof UserDetails userDetails ? userDetails.getUsername() : jwt.getSubject();
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
