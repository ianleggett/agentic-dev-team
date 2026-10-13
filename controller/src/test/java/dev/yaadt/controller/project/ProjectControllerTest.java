package dev.yaadt.controller.project;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createsListsUpdatesAndDeletesProject() throws Exception {
        String body = """
                {
                  "name": "OnlyHabits",
                  "beadsProject": "onlyhabits",
                  "workspaceProvider": "local-devbox",
                  "defaultBranch": "main",
                  "executionMode": "GLOBAL_SERIAL",
                  "maxConcurrentAgentRuns": 1,
                  "maxConcurrentRunsPerProject": 1,
                  "approvalRequired": true
                }
                """;

        String location = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/projects/.+")))
                .andExpect(jsonPath("$.name").value("OnlyHabits"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        String update = """
                {
                  "name": "OnlyHabits Platform",
                  "beadsProject": "onlyhabits",
                  "workspaceProvider": "local-devbox",
                  "active": false,
                  "defaultBranch": "trunk",
                  "executionMode": "GLOBAL_SERIAL",
                  "maxConcurrentAgentRuns": 2,
                  "maxConcurrentRunsPerProject": 1,
                  "approvalRequired": false
                }
                """;

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OnlyHabits Platform"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsInvalidProject() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
