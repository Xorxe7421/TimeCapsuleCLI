package org.pavl.db.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Mood {
    ROCK_BOTTOM("Rock Bottom", 1),
    VERY_DOWN("Very Down", 2),
    LOW("Low", 3),
    BELLOW_AVERAGE("Bellow Average", 4),
    NEUTRAL_MEH("Neutral/Meh", 5),
    DECENT("Decent", 6),
    GOOD("Good", 7),
    GREAT("Great", 8),
    EXCELLENT("Excellent", 9),
    EUPHORIC("Euphoric", 10);

    private final String name;
    private final int score;

    public static Mood fromScore(int score) {
        for (Mood mood : values()) {
            if (mood.score == score) {
                return mood;
            }
        }
        throw new IllegalArgumentException("Illegal mood score: " + score);
    }
}
