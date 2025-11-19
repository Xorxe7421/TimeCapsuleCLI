package org.pavl.db.domain.stats;

import lombok.NonNull;
import org.pavl.db.domain.Mood;

public record MoodStats(
        double averageMood,
        int highestMood,
        int lowestMood
) {
    @Override
    @NonNull
    public String toString() {
        int numMoodLevels = Mood.values().length;
        String highestMoodName = Mood.fromScore(highestMood).getName();
        String lowestMoodName = Mood.fromScore(lowestMood).getName();
        return String.format(
                """
                MOOD OVERVIEW
                Average mood: %.1f/%d
                Highest mood: %d/%d (%s)
                Lowest mood: %d/%d (%s)
                """,
                averageMood,
                numMoodLevels,
                highestMood,
                numMoodLevels,
                highestMoodName,
                lowestMood,
                numMoodLevels,
                lowestMoodName
        );
    }
}