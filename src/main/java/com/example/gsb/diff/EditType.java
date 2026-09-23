package com.example.gsb.diff;

/** Kind of a single edit operation in a diff script. */
public enum EditType {
    /** Line exists unchanged in both old and new text. */
    KEEP,
    /** Line exists only in the new text. */
    INSERT,
    /** Line exists only in the old text. */
    DELETE
}
