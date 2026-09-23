package com.example.gsb.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies an edit script (a patch) to a text, or reverts it. The base text is
 * verified line by line against the script's expectations; any mismatch makes
 * the operation fail with a {@link PatchException} instead of silently
 * producing a wrong result.
 */
public final class Patcher {

    private Patcher() {
    }

    /**
     * Applies {@code edits} (computed from {@code original} to some new text)
     * to {@code original}, returning the new text's lines.
     *
     * @throws PatchException if {@code original} does not match the script
     */
    public static List<String> apply(List<String> original, List<Edit> edits) {
        return run(original, edits, false);
    }

    /**
     * Reverse-applies {@code edits} to the <em>new</em> text, restoring the
     * original text's lines (undo).
     *
     * @throws PatchException if {@code modified} does not match the script
     */
    public static List<String> revert(List<String> modified, List<Edit> edits) {
        return run(modified, edits, true);
    }

    private static List<String> run(List<String> base, List<Edit> edits, boolean reverse) {
        List<String> out = new ArrayList<>(base.size());
        int pos = 0;
        for (Edit e : edits) {
            EditType type = e.type();
            if (reverse) {
                type = switch (type) {
                    case INSERT -> EditType.DELETE;
                    case DELETE -> EditType.INSERT;
                    case KEEP -> EditType.KEEP;
                };
            }
            switch (type) {
                case KEEP -> {
                    requireMatch(base, pos, e);
                    out.add(e.text());
                    pos++;
                }
                case DELETE -> {
                    requireMatch(base, pos, e);
                    pos++;
                }
                case INSERT -> out.add(e.text());
            }
        }
        if (pos != base.size()) {
            throw new PatchException("patch does not consume the whole base text: consumed "
                    + pos + " of " + base.size() + " lines");
        }
        return out;
    }

    private static void requireMatch(List<String> base, int pos, Edit e) {
        if (pos >= base.size()) {
            throw new PatchException("patch expects line " + (pos + 1)
                    + " to be <" + e.text() + "> but the base text has only "
                    + base.size() + " lines");
        }
        String actual = base.get(pos);
        if (!actual.equals(e.text())) {
            throw new PatchException("patch mismatch at line " + (pos + 1)
                    + ": expected <" + e.text() + "> but found <" + actual + ">");
        }
    }
}
