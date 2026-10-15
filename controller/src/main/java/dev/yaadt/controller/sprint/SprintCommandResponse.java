package dev.yaadt.controller.sprint;

import java.util.UUID;

public record SprintCommandResponse(UUID commandId, UUID sprintId, String action, String status, String resultState) { }
