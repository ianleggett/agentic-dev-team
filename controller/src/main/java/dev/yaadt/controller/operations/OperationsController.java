package dev.yaadt.controller.operations;

import dev.yaadt.controller.executor.RunnerAdminClient;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {
    private final RunnerAdminClient runner;
    public OperationsController(RunnerAdminClient runner) { this.runner = runner; }
    @PostMapping("/beads/reset-ownership")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> resetBeadsOwnership() { return runner.resetBeadsOwnership(); }
}
