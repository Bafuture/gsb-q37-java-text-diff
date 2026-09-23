package com.example.gsb.diff;

/**
 * A single token-level edit inside one line.
 *
 * @param type operation kind
 * @param text the token text (a word, whitespace run, punctuation, CJK char or emoji)
 */
public record WordEdit(EditType type, String text) {
}
