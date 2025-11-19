package org.pavl.cli.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CapsuleListingFlag {
    CAPSULE_STATE("capsule-state"),
    TAGS("tags"),
    CREATED_FROM("created-From"),
    CREATED_TO("created-to"),
    UNLOCK_FROM("unlock-from"),
    UNLOCK_TO("unlock-to");

    private final String name;

    public static CapsuleListingFlag fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        for (CapsuleListingFlag capsuleListingFlag : values()) {
            if (capsuleListingFlag.name.equals(name)) {
                return capsuleListingFlag;
            }
        }
        throw new IllegalArgumentException("Illegal capsule listing flag name: " + name);
    }
}
