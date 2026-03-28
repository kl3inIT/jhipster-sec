package com.vn.core.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 * Principal contract for replacing granted authorities after authentication creation.
 */
public interface AcceptsGrantedAuthorities {
    Collection<? extends GrantedAuthority> getGrantedAuthorities();

    void setGrantedAuthorities(Collection<? extends GrantedAuthority> grantedAuthorities);
}
