package dev.yaadt.controller.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenCodeAgentExecutorTest {
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void preservesTimedOutRunnerState() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/run", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes()));
            byte[] response = "{\"state\":\"TIMED_OUT\",\"errorCode\":\"RUN_TIMEOUT\",\"errorMessage\":\"deadline\",\"events\":[]}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        OpenCodeAgentExecutor executor = new OpenCodeAgentExecutor(new ObjectMapper(),
                "http://localhost:" + server.getAddress().getPort(), "runner-token");

        AgentRunResult result = executor.execute(new AgentRunRequest("run-1", UUID.randomUUID(), 1,
                "/workspace/repo", "prompt", Map.of()));

        assertThat(result.state()).isEqualTo(AgentRunResult.State.TIMED_OUT);
        assertThat(result.errorCode()).isEqualTo("RUN_TIMEOUT");
        assertThat(requestBody).hasValueSatisfying(body -> assertThat(body).contains("\"idempotencyKey\":\"run-1\""));
    }

    @Test
    void preservesUnknownStateWhenRunnerResponseIsNotAuthoritative() throws Exception {
        server.createContext("/run", exchange -> {
            byte[] response = "{\"state\":\"UNKNOWN\",\"errorCode\":\"RUNNER_HTTP_502\",\"errorMessage\":\"upstream\",\"events\":[]}".getBytes();
            exchange.sendResponseHeaders(502, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        OpenCodeAgentExecutor executor = new OpenCodeAgentExecutor(new ObjectMapper(),
                "http://localhost:" + server.getAddress().getPort(), "runner-token");

        AgentRunResult result = executor.execute(new AgentRunRequest("run-2", UUID.randomUUID(), 1,
                "/workspace/repo", "prompt", Map.of()));

        assertThat(result.state()).isEqualTo(AgentRunResult.State.UNKNOWN);
        assertThat(result.errorCode()).isEqualTo("RUNNER_HTTP_502");
    }
}
