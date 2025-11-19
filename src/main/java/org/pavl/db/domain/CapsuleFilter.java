package org.pavl.db.domain;

import java.time.LocalDate;
import java.util.Set;

public record CapsuleFilter(
        CapsuleState capsuleState,
        Set<Tag> tags,
        LocalDate createdFrom,
        LocalDate createdTo,
        LocalDate unlockFrom,
        LocalDate unlockTo
) {}