package dev.yaadt.controller.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RepositoryRequestValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    @ParameterizedTest
    @ValueSource(strings = {"../outside", "/absolute", "nested/../../outside", "nested\\outside"})
    void rejectsWorkspacePathsOutsideTheWorkspace(String path) {
        assertFalse(validator.validate(request(path)).isEmpty());
    }

    @Test
    void acceptsSafeRelativeWorkspacePaths() {
        assertTrue(Stream.of(".", "repo", "team/repo-1", "repo.v2/src")
                .allMatch(path -> validator.validate(request(path)).isEmpty()));
    }

    private RepositoryRequest request(String path) {
        return new RepositoryRequest("repo", "https://example.test/repo.git", "main", "product",
                path, "./build", "./test", null, true);
    }
}
