package com.example.gsb.diff;

import java.util.Arrays;
import java.util.List;

/** Helpers to convert between a text block and a list of lines. */
public final class Texts {

    private Texts() {
    }

    /**
     * Splits a text into lines on {@code '\n'}. A trailing newline produces a
     * trailing empty line, so {@code joinLines(splitLines(t)).equals(t)} holds
     * for every {@code t}. The empty string yields an empty list.
     */
    public static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(text.split("\n", -1));
    }

    /** Joins lines with {@code '\n'}; the inverse of {@link #splitLines}. */
    public static String joinLines(List<String> lines) {
        return String.join("\n", lines);
    }
}
