package com.example.gsb.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Line-level diff based on Eugene Myers' O(ND) greedy LCS/SES algorithm
 * ("An O(ND) Difference Algorithm and Its Variations", 1986).
 *
 * <p>Running time is O((N+M)D) and memory is O((N+M)D) where N and M are the
 * input sizes and D is the size of the minimal edit script. The full trace of
 * V arrays is kept so the script can be backtracked exactly.</p>
 */
public final class LineDiffer {

    private LineDiffer() {
    }

    /**
     * Computes the minimal edit script transforming {@code oldLines} into
     * {@code newLines}. Edits are returned in old-to-new order; within one
     * changed block DELETEs are emitted before INSERTs.
     */
    public static List<Edit> diff(List<String> oldLines, List<String> newLines) {
        int n = oldLines.size();
        int m = newLines.size();
        if (n == 0 && m == 0) {
            return List.of();
        }
        List<int[]> trace = computeTrace(oldLines, newLines);
        List<Edit> edits = backtrack(trace, oldLines, newLines);
        return normalize(edits);
    }

    /** Forward pass: records the V array after every D step. */
    private static List<int[]> computeTrace(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();
        int max = n + m;
        int offset = max;
        int[] v = new int[2 * max + 1];
        List<int[]> trace = new ArrayList<>();
        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int x;
                if (k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1])) {
                    x = v[offset + k + 1]; // down move: insertion
                } else {
                    x = v[offset + k - 1] + 1; // right move: deletion
                }
                int y = x - k;
                while (x < n && y < m && a.get(x).equals(b.get(y))) {
                    x++;
                    y++;
                }
                v[offset + k] = x;
                if (x >= n && y >= m) {
                    trace.add(v.clone());
                    return trace;
                }
            }
            trace.add(v.clone());
        }
        return trace; // unreachable for equal-comparable inputs
    }

    /** Backward pass: walks the trace from (N, M) back to (0, 0). */
    private static List<Edit> backtrack(List<int[]> trace, List<String> a, List<String> b) {
        int offset = (a.size() + b.size());
        int x = a.size();
        int y = b.size();
        List<Edit> edits = new ArrayList<>();
        for (int d = trace.size() - 1; d > 0; d--) {
            int[] vPrev = trace.get(d - 1);
            int k = x - y;
            int prevK;
            if (k == -d || (k != d && vPrev[offset + k - 1] < vPrev[offset + k + 1])) {
                prevK = k + 1;
            } else {
                prevK = k - 1;
            }
            int prevX = vPrev[offset + prevK];
            int prevY = prevX - prevK;
            while (x > prevX && y > prevY) {
                edits.add(Edit.keep(x - 1, y - 1, a.get(x - 1)));
                x--;
                y--;
            }
            if (x == prevX) {
                edits.add(Edit.insert(y - 1, b.get(y - 1)));
                y--;
            } else {
                edits.add(Edit.delete(x - 1, a.get(x - 1)));
                x--;
            }
        }
        while (x > 0 && y > 0) {
            edits.add(Edit.keep(x - 1, y - 1, a.get(x - 1)));
            x--;
            y--;
        }
        Collections.reverse(edits);
        return edits;
    }

    /**
     * Reorders each maximal non-KEEP block so DELETEs precede INSERTs, which
     * is the conventional display order. The script semantics are unchanged.
     */
    private static List<Edit> normalize(List<Edit> edits) {
        List<Edit> out = new ArrayList<>(edits.size());
        int i = 0;
        while (i < edits.size()) {
            Edit e = edits.get(i);
            if (e.type() == EditType.KEEP) {
                out.add(e);
                i++;
                continue;
            }
            List<Edit> deletes = new ArrayList<>();
            List<Edit> inserts = new ArrayList<>();
            while (i < edits.size() && edits.get(i).type() != EditType.KEEP) {
                Edit c = edits.get(i);
                (c.type() == EditType.DELETE ? deletes : inserts).add(c);
                i++;
            }
            out.addAll(deletes);
            out.addAll(inserts);
        }
        return out;
    }
}
