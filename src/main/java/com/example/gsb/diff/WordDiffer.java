package com.example.gsb.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Word-level diff for a pair of changed lines. The line is tokenized and the
 * same Myers engine used for lines is applied to the token sequence, so
 * intra-line changes can be highlighted precisely.
 *
 * <p>Tokenization rules:</p>
 * <ul>
 *   <li>runs of ASCII letters/digits/underscore form one word token;</li>
 *   <li>runs of whitespace form one token;</li>
 *   <li>each CJK ideograph / kana / hangul syllable is its own token;</li>
 *   <li>each supplementary-plane code point (emoji etc.) is its own token;</li>
 *   <li>any other character (punctuation) is its own token.</li>
 * </ul>
 */
public final class WordDiffer {

    private WordDiffer() {
    }

    /** Diffs two lines word by word. */
    public static List<WordEdit> diff(String oldLine, String newLine) {
        List<String> oldTokens = tokenize(oldLine);
        List<String> newTokens = tokenize(newLine);
        List<Edit> edits = LineDiffer.diff(oldTokens, newTokens);
        List<WordEdit> out = new ArrayList<>(edits.size());
        for (Edit e : edits) {
            out.add(new WordEdit(e.type(), e.text()));
        }
        return out;
    }

    /** Splits a line into diff tokens; concatenating the tokens yields the line. */
    public static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int len = line.length();
        while (i < len) {
            int cp = line.codePointAt(i);
            int width = Character.charCount(cp);
            if (Character.isWhitespace(cp)) {
                int j = i + width;
                while (j < len && Character.isWhitespace(line.codePointAt(j))) {
                    j += Character.charCount(line.codePointAt(j));
                }
                tokens.add(line.substring(i, j));
                i = j;
            } else if (isCjk(cp) || cp > 0xFFFF) {
                tokens.add(new String(Character.toChars(cp)));
                i += width;
            } else if (Character.isLetterOrDigit(cp) || cp == '_') {
                int j = i + width;
                while (j < len) {
                    int c = line.codePointAt(j);
                    if (!(Character.isLetterOrDigit(c) || c == '_') || isCjk(c) || c > 0xFFFF) {
                        break;
                    }
                    j += Character.charCount(c);
                }
                tokens.add(line.substring(i, j));
                i = j;
            } else {
                tokens.add(new String(Character.toChars(cp)));
                i += width;
            }
        }
        return tokens;
    }

    private static boolean isCjk(int cp) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }
}
