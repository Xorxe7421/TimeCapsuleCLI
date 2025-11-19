package org.pavl.db.domain.stats;

import lombok.NonNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record TimeStats(
        LocalDate oldestCapsule,
        LocalDate newestCapsule,
        LocalDate nextUnlock,
        int averageWaitPeriod
) {
    @Override
    @NonNull
    public String toString() {
        int averageDaysInMonth = 30;
        long daysUntilNextUnlock = ChronoUnit.DAYS.between(LocalDate.now(), nextUnlock);
        return String.format(
                """
                TIME INSIGHTS
                Oldest capsule: %tb %te, %tY
                Newest capsule: %tb %te, %tY
                Next unlock: %tb %te, %tY (%d days)
                Average wait time: %.1f months
                """,
                oldestCapsule,
                oldestCapsule,
                oldestCapsule,
                newestCapsule,
                newestCapsule,
                newestCapsule,
                nextUnlock,
                nextUnlock,
                nextUnlock,
                daysUntilNextUnlock,
                (double) averageWaitPeriod / averageDaysInMonth
        );
    }
}