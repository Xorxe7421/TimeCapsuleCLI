package org.pavl.db.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CapsuleOutput {
    private final UUID id;
    private final String title;
    private final LocalDate createdAt;
    private final LocalDate unlockAt;
    private Set<Tag> tags;
    private final boolean isOpened;

    @Override
    public String toString() {
        return getIdLine() +
                getTitleLine() +
                getTemporalLine() +
                getTagsLine() +
                getStateLine();
    }

    protected String getIdLine() {
        return "Id: " + id + "\n";
    }

    protected String getTitleLine() {
        return "Title: " + title + "\n";
    }

    protected String getTemporalLine() {
        if (unlockAt.isAfter(LocalDate.now())) {
            Period timeLeft = Period.between(LocalDate.now(), unlockAt);
            if (timeLeft.getYears() == 0) {
                long daysUntilUnlock = ChronoUnit.DAYS.between(LocalDate.now(), unlockAt);
                return String.format(
                        """
                        Created: %tb %te, %tY
                        Unlocks in %d days (%tb %te, %tY)
                        """,
                        createdAt,
                        createdAt,
                        createdAt,
                        daysUntilUnlock,
                        unlockAt,
                        unlockAt,
                        unlockAt
                );
            }else {
                if (timeLeft.getMonths() != 0) {
                    return String.format(
                            """
                                    Created: %tb %te, %tY
                                    Unlocks in %d year, %d months (%tb %te, %tY)
                                    """,
                            createdAt,
                            createdAt,
                            createdAt,
                            timeLeft.getYears(),
                            timeLeft.getMonths(),
                            unlockAt,
                            unlockAt,
                            unlockAt
                    );
                }else {
                    return String.format(
                            """
                                    Created: %tb %te, %tY
                                    Unlocks in %d years (%tb %te, %tY)
                                    """,
                            createdAt,
                            createdAt,
                            createdAt,
                            timeLeft.getYears(),
                            unlockAt,
                            unlockAt,
                            unlockAt
                    );
                }
            }
        }else {
            return String.format(
                    "Created: %tb %te, %tY | Unlocked: %tb %te, %tY\n",
                    createdAt,
                    createdAt,
                    createdAt,
                    unlockAt,
                    unlockAt,
                    unlockAt
            );
        }
    }

    protected String getTagsLine() {
        return "tags: " + tags + "\n";
    }

    protected String getStateLine() {
        return "State: " + (isOpened ? "Open" : "Closed") + "\n";
    }
}
