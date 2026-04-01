package com.vn.core.security.access;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link AccessManager} that applies all
 * registered {@link AccessConstraint}s in order.
 *
 * <p>D-11: Constraints are sorted once at construction time by {@link AccessConstraint#getOrder()}
 * rather than on every {@link #applyRegisteredConstraints} call, eliminating per-request sort
 * overhead in the hot security-check path.
 */
@Component
public class AccessManagerImpl implements AccessManager {

    private final List<AccessConstraint<?>> constraints;

    public AccessManagerImpl(List<AccessConstraint<?>> constraints) {
        // D-11: sort once at construction time; the order never changes at runtime.
        this.constraints = constraints.stream().sorted(Comparator.comparingInt(AccessConstraint::getOrder)).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends AccessContext> C applyRegisteredConstraints(C context) {
        constraints
            .stream()
            .filter(c -> c.supports().isAssignableFrom(context.getClass()))
            .forEach(c -> ((AccessConstraint<C>) c).applyTo(context));
        return context;
    }
}
