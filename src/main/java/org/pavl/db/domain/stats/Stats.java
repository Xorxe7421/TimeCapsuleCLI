package org.pavl.db.domain.stats;

import lombok.NonNull;
import org.pavl.Utils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record Stats(
        CapsuleStats capsuleStats,
        TimeStats timeStats,
        MoodStats moodStats,
        TagStats tagStats
) {
    @Override
    @NonNull
    public String toString() {
        return Utils.toString(
                Stream.of(
                        Optional.ofNullable(capsuleStats).map(Objects::toString).orElse(null),
                        Optional.ofNullable(timeStats).map(Objects::toString).orElse(null),
                        Optional.ofNullable(moodStats).map(Objects::toString).orElse(null),
                        Optional.ofNullable(tagStats).map(Objects::toString).orElse(null)
                ).filter(Objects::nonNull).toList(),
                1
        );
    }
}