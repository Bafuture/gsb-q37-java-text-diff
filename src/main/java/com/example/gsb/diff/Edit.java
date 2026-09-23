package com.example.gsb.diff;

/**
 * A single edit operation. Indices are 0-based; an index is {@code -1} when it
 * does not apply (e.g. {@code oldIndex} of an INSERT).
 *
 * @param type     operation kind
 * @param oldIndex index of the line in the old text, or -1 for INSERT
 * @param newIndex index of the line in the new text, or -1 for DELETE
 * @param text     the line content (without trailing newline)
 */
public record Edit(EditType type, int oldIndex, int newIndex, String text) {

    public static Edit keep(int oldIndex, int newIndex, String text) {
        return new Edit(EditType.KEEP, oldIndex, newIndex, text);
    }

    public static Edit insert(int newIndex, String text) {
        return new Edit(EditType.INSERT, -1, newIndex, text);
    }

    public static Edit delete(int oldIndex, String text) {
        return new Edit(EditType.DELETE, oldIndex, -1, text);
    }
}
