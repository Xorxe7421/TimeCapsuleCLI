package org.pavl.cli.domain;

public enum StatFlag {
    CAPSULE, TIME, MOOD, TAG;

    public static StatFlag fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        try {
            return StatFlag.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown stat: " + name);
        }
    }
}
