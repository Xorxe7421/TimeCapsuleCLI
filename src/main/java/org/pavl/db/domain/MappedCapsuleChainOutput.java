package org.pavl.db.domain;

import java.util.Map;
import java.util.UUID;

public record MappedCapsuleChainOutput(UUID id, String name, Map<UUID, CapsuleOutput> capsuleMap) {}
