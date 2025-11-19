package org.pavl.db.domain;

import java.time.LocalDate;
import java.util.UUID;

public record CapsuleChainResultSetRow(
        UUID chainId,
        String chainName,
        UUID capsuleId,
        String capsuleTitle,
        LocalDate capsuleCreatedAt,
        LocalDate capsuleUnlockAt,
        boolean isOpened
) {}