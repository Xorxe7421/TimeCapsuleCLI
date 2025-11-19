package org.pavl.db.domain;

import lombok.Getter;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
public class CapsuleOutputDetails extends CapsuleOutput {
    private final String content;
    private final String attachmentName;
    private final InputStream attachment;
    private final Mood mood;

    public CapsuleOutputDetails(UUID id,
                                String title,
                                String content,
                                String attachmentName,
                                InputStream attachment,
                                LocalDate createdAt,
                                LocalDate unlockedA,
                                Set<Tag> tags,
                                Mood mood,
                                boolean isOpened
    ) {
        super(id, title, createdAt, unlockedA, tags, isOpened);
        this.content = content;
        this.attachmentName = attachmentName;
        this.attachment = attachment;
        this.mood = mood;
    }

    @Override
    public String toString() {
        return getIdLine() +
                getTitleLine() +
                "Content: " + content + "\n" +
                "Mood: " + mood + "\n" +
                getTemporalLine() +
                getTagsLine() +
                getStateLine() +
                (attachmentName != null ? "\nAttachment with the name " + attachmentName + " will be put in the current directory\n" : "");
    }
}
