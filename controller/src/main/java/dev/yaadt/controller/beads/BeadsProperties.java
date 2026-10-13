package dev.yaadt.controller.beads;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yaadt.beads")
public class BeadsProperties {

    private String command = "bd";
    private String directory = "/workspace";
    private String beadsDir = "/workspace/.beads";
    private String actor = "yaadt-controller";
    private Duration timeout = Duration.ofSeconds(30);

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getBeadsDir() {
        return beadsDir;
    }

    public void setBeadsDir(String beadsDir) {
        this.beadsDir = beadsDir;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
