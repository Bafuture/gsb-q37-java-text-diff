package com.example.gsb.diff;

import java.util.List;

/**
 * Outcome of a three-way merge.
 *
 * @param lines          merged lines; conflicting regions are wrapped in
 *                       {@code <<<<<<<} / {@code =======} / {@code >>>>>>>} markers
 * @param conflictCount  number of conflicting regions (0 means a clean merge)
 */
public record MergeResult(List<String> lines, int conflictCount) {

    public boolean hasConflicts() {
        return conflictCount > 0;
    }

    /** The merged text, lines joined with {@code '\n'}. */
    public String text() {
        return Texts.joinLines(lines);
    }
}
