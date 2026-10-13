package dev.yaadt.controller.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "agent_profiles")
public class AgentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 60)
    private String role;

    @Column(nullable = false)
    private boolean active;

    protected AgentProfile() {
    }

    public AgentProfile(String name, String role) {
        this.name = name;
        this.role = role;
        this.active = true;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
}
