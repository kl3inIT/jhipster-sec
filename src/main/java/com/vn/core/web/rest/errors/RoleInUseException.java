package com.vn.core.web.rest.errors;

import java.io.Serial;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;

@SuppressWarnings("java:S110") // Inheritance tree of classes should not be too deep
public class RoleInUseException extends ErrorResponseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RoleInUseException() {
        super(
            HttpStatus.CONFLICT,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(HttpStatus.CONFLICT.value())
                .withType(URI.create("https://www.jhipster.tech/problem/problem-with-message"))
                .withTitle("Role is assigned to users and cannot be deleted")
                .withProperty("message", "error.roleinuse")
                .withProperty("params", "secRole")
                .build(),
            null
        );
    }
}
