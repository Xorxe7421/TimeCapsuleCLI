package org.pavl.db.domain;

public enum Tag {
    PERSONAL,
    GOALS,
    CAREER,
    FITNESS,
    NEWYEAR,
    BIRTHDAY,
    FAMILY,
    HEALTH,
    REFLECTION,
    DREAMS,
    TRAVEL,
    WORK,
    ACHIEVEMENT,
    GRATITUDE,
    MEMORIES;

    public static Tag fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        try {
            return Tag.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown tag: " + name);
        }
    }
}
