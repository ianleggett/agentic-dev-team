package dev.yaadt.controller.profile;

import dev.yaadt.controller.user.User;
import dev.yaadt.controller.user.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AgentProfileSeeder implements CommandLineRunner {
    private record Team(String role, String name, String file) { }
    private static final List<Team> TEAM = List.of(
            new Team("spec", "Specification Writer", "spec-writer.md"),
            new Team("architect", "Solution Architect", "solution-architect.md"),
            new Team("tech-lead", "Tech Lead", "tech-lead.md"),
            new Team("fullstack", "Full-Stack Developer", "fullstack-dev.md"),
            new Team("database", "Database Developer", "database-dev.md"),
            new Team("devops", "DevOps Engineer", "devops.md"),
            new Team("qa", "QA Engineer", "qa-test.md"),
            new Team("review", "Senior Code Reviewer", "code-reviewer.md"),
            new Team("security", "Security Reviewer", "security-reviewer.md"),
            new Team("docs", "Documentation Writer", "docs-writer.md"),
            new Team("ui-assessor", "UI Assessment Agent", "ui-assessor.md"),
            new Team("project-manager", "Project Manager", "project-manager.md"));
    private final AgentProfileRepository profiles;
    private final AgentProfileVersionRepository versions;
    private final UserRepository users;
    private final Clock clock;
    public AgentProfileSeeder(AgentProfileRepository profiles, AgentProfileVersionRepository versions,
            UserRepository users, Clock clock) { this.profiles = profiles; this.versions = versions; this.users = users; this.clock = clock; }

    @Override @Transactional
    public void run(String... args) {
        User system = users.findBySubject("controller-seed").orElseGet(() -> users.save(
                new User("controller-seed", "YAADT Controller", Instant.now(clock))));
        for (Team team : TEAM) {
            if (profiles.findByRole(team.role()).isPresent()) continue;
            AgentProfile profile = profiles.save(new AgentProfile(team.name(), team.role()));
            AgentProfileRequest request = new AgentProfileRequest(team.name(), team.role(), "external/qwen-coder",
                    "openai-compatible", "http://host.docker.internal:8000/v1", null, "team/" + team.file(),
                    readPrompt(team.file()), "read,write,test", "workspace-only", "no-docker-credentials", 100, 1800);
            versions.save(new AgentProfileVersion(profile.getId(), 1, request, system.getId(), Instant.now(clock)));
        }
    }
    private String readPrompt(String file) {
        try (InputStream input = new ClassPathResource("team-prompts/" + file).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) { throw new IllegalStateException("Unable to load team prompt " + file, exception); }
    }
}
