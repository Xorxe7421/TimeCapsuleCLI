package org.pavl.db.domain;

import lombok.NonNull;
import org.pavl.Utils;

import java.util.List;
import java.util.UUID;

public record CapsuleChainOutput(UUID id, String name, List<CapsuleOutput> capsules) {

    @Override
    @NonNull
    public String toString() {
        int numDashes = 20;
        return "Id: " + id + "\n" +
                "Name: " + name + "\n" +
                "-".repeat(numDashes) + "\n" +
                "Capsules" + "\n" +
                "-".repeat(numDashes) + "\n" +
                Utils.toString(capsules, 1);
    }
}