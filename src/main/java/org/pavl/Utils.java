package org.pavl;

import org.pavl.db.domain.Tag;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Utils {

    public static String encode(String text) {
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode(text.getBytes(StandardCharsets.UTF_8));
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    public static String decode(String text) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decodedBytes = decoder.decode(text.getBytes(StandardCharsets.UTF_8));
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    public static <T extends Comparable<? super T>> T upperBound(List<T> items, T item) {
        int index = Collections.binarySearch(items, item);
        int size = items.size();

        if (index < 0) {
            int upperBound = Math.abs(index) - 1;
            if (upperBound < size) {
                return items.get(upperBound);
            } else {
                return null;
            }
        } else {
            while (index < size) {
                if (items.get(index).equals(item)) {
                    index++;
                } else {
                    return items.get(index);
                }
            }
            return null;
        }
    }

    public static Set<Tag> createIndividualTag(String tagAsString) {
        return Optional.ofNullable(tagAsString)
                .map(Tag::fromName)
                .map(tag -> EnumSet.copyOf(Set.of(tag)))
                .orElse(EnumSet.noneOf(Tag.class));
    }

    public static  <T> String toString(List<T> outputs, int numLineBreaks) {
        return String.join("\n".repeat(numLineBreaks), outputs.stream().map(Object::toString).toList());
    }
}
