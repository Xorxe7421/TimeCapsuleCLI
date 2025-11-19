package org.pavl.cli.domain;

public enum Entity {
    CAPSULE, CHAIN;

    public static Entity fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        try {
            return Entity.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown entity: " + name);
        }
    }
}
