package org.pavl.db.domain.stats;

import lombok.NonNull;
import org.pavl.db.domain.Tag;

public record TagStats(
        int totalUniqueTags,
        Tag mostUsedTag,
        int mostUsedTagOccurrence,
        Tag leastUsedTag,
        int leastUsedTagOccurrence
) {
    @Override
    @NonNull
    public String toString() {
        return String.format(
                """
                TAG SUMMARY
                Total unique tags: %d
                Most used tag: %s (%d capsules)
                Least used tag: %s (%d capsules)
                """,
                totalUniqueTags,
                mostUsedTag,
                mostUsedTagOccurrence,
                leastUsedTag,
                leastUsedTagOccurrence
        );
    }
}