package dev.yaadt.controller.beads;

import java.util.Locale;
import java.util.Set;

public final class BeadsStatus {
    private static final Set<String> TERMINAL = Set.of("closed", "completed", "cancelled", "canceled", "done");

    private BeadsStatus() { }

    public static boolean isTerminal(String status) {
        return status != null && TERMINAL.contains(status.toLowerCase(Locale.ROOT));
    }
}
