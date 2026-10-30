package dev.yaadt.controller.delivery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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
class DeliveryPlanControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void createsSubmitsAndApprovesDeliveryPlan() throws Exception {
        String projectLocation = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Delivery Plan Test","objective":"test","beadsProject":"delivery-plan-test","workspaceProvider":"local-devbox","defaultBranch":"main","executionMode":"GLOBAL_SERIAL","maxConcurrentAgentRuns":1,"maxConcurrentRunsPerProject":1,"approvalRequired":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String directiveLocation = mockMvc.perform(post(projectLocation + "/directives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Subject", "operator")
                        .header("X-User-Name", "Operator")
                        .content("{\"content\":\"Delivery plan test directive\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String directiveId = directiveLocation.substring(directiveLocation.lastIndexOf('/') + 1);

        String planBody = """
                {
                  "objective":"Ship the test slice",
                  "reasoning":"Keep the first sprint bounded",
                  "acceptanceCriteria":"The test passes",
                  "suggestedSprintBoundaries":"One sprint",
                  "planSnapshot":"{\\"approvalPolicy\\":\\"human\\",\\"failurePolicy\\":\\"stop\\",\\"steps\\":[]}",
                  "milestones":[],
                  "beads":[{"beadId":"yaadt-test-1","titleSnapshot":"Test bead","statusSnapshot":"open","beadSnapshot":"{}"}],
                  "directiveRevisionId":"%s"
                }
                """.formatted(directiveId);

        String planLocation = mockMvc.perform(post(projectLocation + "/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Subject", "operator")
                        .header("X-User-Name", "Operator")
                        .content(planBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/delivery-plans/.+")))
                .andExpect(jsonPath("$.state").value("DRAFT"))
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(post(planLocation + "/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING_APPROVAL"));

        mockMvc.perform(post(planLocation + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Subject", "reviewer")
                        .content("{\"version\":1,\"reason\":\"ready\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value("reviewer"));
    }

    @Test
    void rejectsPlanWithoutDirectiveRevision() throws Exception {
        String projectLocation = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Invalid Delivery Plan Test","objective":"test","beadsProject":"invalid-delivery-plan-test","workspaceProvider":"local-devbox","defaultBranch":"main","executionMode":"GLOBAL_SERIAL","maxConcurrentAgentRuns":1,"maxConcurrentRunsPerProject":1,"approvalRequired":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(post(projectLocation + "/delivery-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"objective\":\"test\",\"reasoning\":\"test\",\"acceptanceCriteria\":\"test\",\"suggestedSprintBoundaries\":\"test\",\"planSnapshot\":\"{}\",\"beads\":[{\"beadId\":\"b1\",\"titleSnapshot\":\"b\",\"statusSnapshot\":\"open\",\"beadSnapshot\":\"{}\"}]" +
                                "}"))
                .andExpect(status().isBadRequest());
    }
}
