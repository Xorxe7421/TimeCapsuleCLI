package org.pavl.cli.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CapsuleCreationFlag {
    ATTACHMENT("attachment"),
    TAGS("tags"),
    CHAIN_ID("chain-id");

    private final String name;

    public static CapsuleCreationFlag fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        for (CapsuleCreationFlag capsuleCreationFlag : values()) {
            if (capsuleCreationFlag.name.equals(name)) {
                return capsuleCreationFlag;
            }
        }
        throw new IllegalArgumentException("Illegal capsule creation flag name: " + name);
    }
}
