package org.pavl.cli.domain;

public enum Command {
    CREATE, LIST, OPEN, STATS;

    public static Command fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        try {
            return Command.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown command: " + name);
        }
    }
}
