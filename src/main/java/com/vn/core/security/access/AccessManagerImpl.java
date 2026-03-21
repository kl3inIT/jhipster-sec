package com.vn.core.security.access;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link AccessManager} that applies all
 * registered {@link AccessConstraint}s in order.
 */
@Component
public class AccessManagerImpl implements AccessManager {

    private final List<AccessConstraint<?>> constraints;

    public AccessManagerImpl(List<AccessConstraint<?>> constraints) {
        this.constraints = constraints;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends AccessContext> C applyRegisteredConstraints(C context) {
        constraints
            .stream()
            .filter(c -> c.supports().isAssignableFrom(context.getClass()))
            .sorted(Comparator.comparingInt(AccessConstraint::getOrder))
            .forEach(c -> ((AccessConstraint<C>) c).applyTo(context));
        return context;
    }
}
