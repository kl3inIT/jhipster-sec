package com.vn.core.security.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.vn.core.security.domain.SecPermission;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PermissionMatrix} verifying key construction, ALLOW/DENY semantics,
 * entity vs. attribute dispatch, and wildcard attribute resolution.
 */
class PermissionMatrixTest {

    private static SecPermission entityAllow(String target, String action) {
        return new SecPermission().targetType(TargetType.ENTITY).target(target).action(action).effect("ALLOW");
    }

    private static SecPermission entityDeny(String target, String action) {
        return new SecPermission().targetType(TargetType.ENTITY).target(target).action(action).effect("DENY");
    }

    private static SecPermission attributeAllow(String target, String action) {
        return new SecPermission().targetType(TargetType.ATTRIBUTE).target(target).action(action).effect("ALLOW");
    }

    private static SecPermission attributeDeny(String target, String action) {
        return new SecPermission().targetType(TargetType.ATTRIBUTE).target(target).action(action).effect("DENY");
    }

    // --- EMPTY / deny-default ---

    @Test
    void emptyPermissionListDeniesEntityOp() {
        PermissionMatrix matrix = new PermissionMatrix(List.of());

        assertThat(matrix.isEntityPermitted("ORDER", "READ")).isFalse();
    }

    @Test
    void emptyConstantDeniesEverything() {
        assertThat(PermissionMatrix.EMPTY.isEntityPermitted("ORDER", "READ")).isFalse();
        assertThat(PermissionMatrix.EMPTY.isAttributePermitted("ORDER.AMOUNT", "VIEW")).isFalse();
    }

    // --- Entity ALLOW ---

    @Test
    void entityAllowGrantsMatchingOp() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(entityAllow("ORDER", "READ")));

        assertThat(matrix.isEntityPermitted("ORDER", "READ")).isTrue();
    }

    @Test
    void entityAllowDoesNotGrantDifferentOp() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(entityAllow("ORDER", "READ")));

        assertThat(matrix.isEntityPermitted("ORDER", "CREATE")).isFalse();
    }

    @Test
    void entityAllowDoesNotGrantDifferentEntity() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(entityAllow("ORDER", "READ")));

        assertThat(matrix.isEntityPermitted("INVOICE", "READ")).isFalse();
    }

    // --- Entity DENY only ---

    @Test
    void entityDenyOnlyReturnsFalse() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(entityDeny("ORDER", "READ")));

        assertThat(matrix.isEntityPermitted("ORDER", "READ")).isFalse();
    }

    // --- ALLOW + DENY union-of-ALLOW ---

    @Test
    void entityAllowAndDenyTogetherStillPermits() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(entityAllow("ORDER", "READ"), entityDeny("ORDER", "READ")));

        assertThat(matrix.isEntityPermitted("ORDER", "READ")).isTrue();
    }

    // --- Attribute specific ---

    @Test
    void specificAttributeAllowGrantsView() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeAllow("ORDER.AMOUNT", "VIEW")));

        assertThat(matrix.isAttributePermitted("ORDER.AMOUNT", "VIEW")).isTrue();
    }

    @Test
    void specificAttributeAllowDoesNotGrantEdit() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeAllow("ORDER.AMOUNT", "VIEW")));

        assertThat(matrix.isAttributePermitted("ORDER.AMOUNT", "EDIT")).isFalse();
    }

    @Test
    void attributeDenyOnlyReturnsFalse() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeDeny("ORDER.AMOUNT", "VIEW")));

        assertThat(matrix.isAttributePermitted("ORDER.AMOUNT", "VIEW")).isFalse();
    }

    // --- Wildcard attribute ---

    @Test
    void wildcardAttributeAllowGrantsSpecificAttribute() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeAllow("ORDER.*", "VIEW")));

        assertThat(matrix.isAttributePermitted("ORDER.AMOUNT", "VIEW")).isTrue();
    }

    @Test
    void wildcardAttributeAllowGrantsMultipleFields() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeAllow("ORDER.*", "EDIT")));

        assertThat(matrix.isAttributePermitted("ORDER.AMOUNT", "EDIT")).isTrue();
        assertThat(matrix.isAttributePermitted("ORDER.STATUS", "EDIT")).isTrue();
    }

    @Test
    void wildcardAttributeDoesNotGrantDifferentEntity() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeAllow("ORDER.*", "VIEW")));

        assertThat(matrix.isAttributePermitted("INVOICE.TOTAL", "VIEW")).isFalse();
    }

    // --- Entity vs attribute non-interference ---

    @Test
    void entityAllowDoesNotGrantAttributeCheck() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(entityAllow("ORDER", "READ")));

        assertThat(matrix.isAttributePermitted("ORDER.AMOUNT", "VIEW")).isFalse();
    }

    @Test
    void attributeAllowDoesNotGrantEntityCheck() {
        PermissionMatrix matrix = new PermissionMatrix(List.of(attributeAllow("ORDER.AMOUNT", "VIEW")));

        assertThat(matrix.isEntityPermitted("ORDER", "READ")).isFalse();
    }

    // --- Multiple permissions ---

    @Test
    void multipleEntityAllowsGrantAll() {
        PermissionMatrix matrix = new PermissionMatrix(
            List.of(entityAllow("ORDER", "READ"), entityAllow("ORDER", "CREATE"), entityAllow("INVOICE", "READ"))
        );

        assertThat(matrix.isEntityPermitted("ORDER", "READ")).isTrue();
        assertThat(matrix.isEntityPermitted("ORDER", "CREATE")).isTrue();
        assertThat(matrix.isEntityPermitted("INVOICE", "READ")).isTrue();
        assertThat(matrix.isEntityPermitted("ORDER", "DELETE")).isFalse();
    }
}
