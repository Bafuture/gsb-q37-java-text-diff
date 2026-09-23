package com.example.gsb.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders an edit script in the unified diff format: hunks with configurable
 * context lines and {@code @@ -oldStart,oldCount +newStart,newCount @@}
 * headers, following the same conventions as {@code diff -u} / git.
 */
public final class UnifiedDiffFormatter {

    private UnifiedDiffFormatter() {
    }

    /** Formats edits with the given number of context lines, without file headers. */
    public static String format(List<Edit> edits, int context) {
        return format(null, null, edits, context);
    }

    /** Formats edits with {@code --- oldName} / {@code +++ newName} file headers. */
    public static String format(String oldName, String newName, List<Edit> edits, int context) {
        if (context < 0) {
            throw new IllegalArgumentException("context must be >= 0");
        }
        List<int[]> hunks = findHunks(edits, context);
        StringBuilder sb = new StringBuilder();
        if (oldName != null && newName != null) {
            sb.append("--- ").append(oldName).append('\n');
            sb.append("+++ ").append(newName).append('\n');
        }
        for (int[] hunk : hunks) {
            appendHunk(sb, edits, hunk[0], hunk[1]);
        }
        return sb.toString();
    }

    /** Returns [start, end) index ranges (into the edit list) of each hunk. */
    private static List<int[]> findHunks(List<Edit> edits, int context) {
        List<int[]> hunks = new ArrayList<>();
        int i = 0;
        int size = edits.size();
        while (i < size) {
            if (edits.get(i).type() == EditType.KEEP) {
                i++;
                continue;
            }
            int start = Math.max(i - context, 0);
            int lastChange = i;
            int j = i;
            while (j < size) {
                if (edits.get(j).type() != EditType.KEEP) {
                    lastChange = j;
                    j++;
                } else if (j - lastChange > 2L * context) {
                    break;
                } else {
                    j++;
                }
            }
            int end = Math.min(lastChange + 1 + context, size);
            hunks.add(new int[]{start, end});
            i = end;
        }
        return hunks;
    }

    private static void appendHunk(StringBuilder sb, List<Edit> edits, int start, int end) {
        int oldCount = 0;
        int newCount = 0;
        for (int i = start; i < end; i++) {
            EditType t = edits.get(i).type();
            if (t != EditType.INSERT) {
                oldCount++;
            }
            if (t != EditType.DELETE) {
                newCount++;
            }
        }
        int oldStart = oldCount == 0 ? countOld(edits, start) : countOld(edits, start) + 1;
        int newStart = newCount == 0 ? countNew(edits, start) : countNew(edits, start) + 1;
        sb.append("@@ -").append(range(oldStart, oldCount))
          .append(" +").append(range(newStart, newCount))
          .append(" @@\n");
        for (int i = start; i < end; i++) {
            Edit e = edits.get(i);
            char marker = switch (e.type()) {
                case KEEP -> ' ';
                case DELETE -> '-';
                case INSERT -> '+';
            };
            sb.append(marker).append(e.text()).append('\n');
        }
    }

    private static int countOld(List<Edit> edits, int before) {
        int count = 0;
        for (int i = 0; i < before; i++) {
            if (edits.get(i).type() != EditType.INSERT) {
                count++;
            }
        }
        return count;
    }

    private static int countNew(List<Edit> edits, int before) {
        int count = 0;
        for (int i = 0; i < before; i++) {
            if (edits.get(i).type() != EditType.DELETE) {
                count++;
            }
        }
        return count;
    }

    private static String range(int start, int count) {
        if (count == 1) {
            return Integer.toString(start);
        }
        return start + "," + count;
    }
}
