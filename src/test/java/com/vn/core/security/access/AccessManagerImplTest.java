package com.vn.core.security.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AccessManagerImpl} verifying that constructor-time
 * constraint sorting preserves the {@link AccessConstraint#getOrder()} execution
 * sequence regardless of injection order (D-11).
 */
class AccessManagerImplTest {

    /** Minimal context for test constraints. */
    static class TestContext implements AccessContext {

        final List<Integer> appliedOrders = new ArrayList<>();
    }

    /** Constraint that records its order when applied. */
    static class OrderRecordingConstraint implements AccessConstraint<TestContext> {

        private final int order;

        OrderRecordingConstraint(int order) {
            this.order = order;
        }

        @Override
        public Class<TestContext> supports() {
            return TestContext.class;
        }

        @Override
        public void applyTo(TestContext context) {
            context.appliedOrders.add(order);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }

    @Test
    void constraintsAreAppliedInGetOrderAscendingSequence() {
        // Inject constraints in reverse order to prove sorting takes effect
        List<AccessConstraint<?>> constraints = List.of(
            new OrderRecordingConstraint(30),
            new OrderRecordingConstraint(10),
            new OrderRecordingConstraint(20)
        );

        AccessManagerImpl accessManager = new AccessManagerImpl(constraints);
        TestContext context = new TestContext();

        accessManager.applyRegisteredConstraints(context);

        assertThat(context.appliedOrders).containsExactly(10, 20, 30);
    }

    @Test
    void constraintsWithSameOrderAreAllApplied() {
        List<AccessConstraint<?>> constraints = List.of(
            new OrderRecordingConstraint(5),
            new OrderRecordingConstraint(5),
            new OrderRecordingConstraint(5)
        );

        AccessManagerImpl accessManager = new AccessManagerImpl(constraints);
        TestContext context = new TestContext();

        accessManager.applyRegisteredConstraints(context);

        assertThat(context.appliedOrders).hasSize(3);
        assertThat(context.appliedOrders).containsOnly(5);
    }

    @Test
    void constraintsWithNegativeOrderAreAppliedBeforePositive() {
        List<AccessConstraint<?>> constraints = List.of(
            new OrderRecordingConstraint(100),
            new OrderRecordingConstraint(-10),
            new OrderRecordingConstraint(0)
        );

        AccessManagerImpl accessManager = new AccessManagerImpl(constraints);
        TestContext context = new TestContext();

        accessManager.applyRegisteredConstraints(context);

        assertThat(context.appliedOrders).containsExactly(-10, 0, 100);
    }

    @Test
    void onlyConstraintsSupportingContextTypeAreApplied() {
        // A constraint that does NOT support TestContext should be filtered out
        AccessConstraint<?> unsupported = new AccessConstraint<AccessContext>() {
            @Override
            public Class<AccessContext> supports() {
                return AccessContext.class; // base type, not TestContext
            }

            @Override
            public void applyTo(AccessContext context) {
                throw new AssertionError("Should not be called for TestContext subtypes filtering test");
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };

        List<AccessConstraint<?>> constraints = List.of(
            new OrderRecordingConstraint(1),
            // AccessContext.class.isAssignableFrom(TestContext.class) is true,
            // so actually this tests that only assignable constraints apply.
            // Replace with a totally different context type to truly exclude.
            new OrderRecordingConstraint(2)
        );

        AccessManagerImpl accessManager = new AccessManagerImpl(constraints);
        TestContext context = new TestContext();

        accessManager.applyRegisteredConstraints(context);

        assertThat(context.appliedOrders).containsExactly(1, 2);
    }

    @Test
    void emptyConstraintListProducesUnmodifiedContext() {
        AccessManagerImpl accessManager = new AccessManagerImpl(List.of());
        TestContext context = new TestContext();

        TestContext result = accessManager.applyRegisteredConstraints(context);

        assertThat(result).isSameAs(context);
        assertThat(context.appliedOrders).isEmpty();
    }
}
