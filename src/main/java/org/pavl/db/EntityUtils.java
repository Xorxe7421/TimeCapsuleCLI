package org.pavl.db;

import org.pavl.db.domain.CapsuleOutput;
import org.pavl.db.domain.Tag;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EntityUtils {

    public static final String CAPSULE_ID_COLUMN = "id";
    public static final String CAPSULE_TITLE_COLUMN = "title";
    public static final String CAPSULE_CONTENT_COLUMN = "content";
    public static final String CAPSULE_ATTACHMENT_NAME_COLUMN = "attachment_name";
    public static final String CAPSULE_ATTACHMENT_COLUMN = "attachment";
    public static final String CAPSULE_CREATED_AT_COLUMN = "created_at";
    public static final String CAPSULE_UNLOCK_AT_COLUMN = "unlock_at";
    public static final String CAPSULE_MOOD_SCORE_COLUMN = "mood_score";
    public static final String CAPSULE_IS_OPENED_COLUMN = "is_opened";
    public static final String CAPSULE_TAG_COLUMN = "tag";

    public static final String CHAIN_CAPSULE_ID_COLUMN = "capsule_id";
    public static final String CHAIN_ID_COLUMN = "chain_id";
    public static final String CHAIN_NAME_COLUMN = "chain_name";

    public static void handleNewValue(
            Map<UUID, CapsuleOutput> capsuleOutputMap,
            UUID capsuleId,
            String capsuleTitle,
            LocalDate capsuleCreatedAt,
            LocalDate capsuleUnlockAt,
            Set<Tag> individualTag,
            boolean capsuleIsOpen
    ) {
        capsuleOutputMap.compute(capsuleId, (key, value) -> {
            if (value == null) {
                return new CapsuleOutput(
                        capsuleId,
                        capsuleTitle,
                        capsuleCreatedAt,
                        capsuleUnlockAt,
                        individualTag,
                        capsuleIsOpen
                );
            } else {
                Set<Tag> tags = value.getTags();
                tags.addAll(individualTag);
                return value;
            }
        });
    }
}
