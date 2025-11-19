package org.pavl.db.domain.stats;

import lombok.NonNull;

public record CapsuleStats(
        int totalCapsules,
        int openedCapsules,
        int unlockedCapsules,
        int lockedCapsules
) {
    @Override
    @NonNull
    public String toString() {
        return String.format(
                """
                CAPSULE SUMMARY
                Total capsules: %d
                Opened: %d
                Unlocked: %d
                Still locked: %d
                """,
                totalCapsules,
                openedCapsules,
                unlockedCapsules,
                lockedCapsules
        );
    }
}