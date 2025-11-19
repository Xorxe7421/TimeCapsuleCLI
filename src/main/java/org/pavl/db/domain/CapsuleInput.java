package org.pavl.db.domain;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CapsuleInput(
        String title,
        String content,
        String attachmentName,
        InputStream attachment,
        LocalDate unlockedAt,
        int moodScore,
        Set<Tag> tags,
        UUID chainId
) {}
