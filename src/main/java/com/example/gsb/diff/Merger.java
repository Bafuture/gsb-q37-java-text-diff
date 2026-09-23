package com.example.gsb.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Three-way merge: given a common base and two independently modified
 * versions ("ours" and "theirs"), produces a merged text. Regions both sides
 * changed differently are emitted as conflict blocks:
 *
 * <pre>
 * &lt;&lt;&lt;&lt;&lt;&lt;&lt; ours
 * ...our lines...
 * =======
 * ...their lines...
 * &gt;&gt;&gt;&gt;&gt;&gt;&gt; theirs
 * </pre>
 *
 * Identical changes on both sides are applied once without conflict.
 */
public final class Merger {

    public static final String OURS_LABEL = "ours";
    public static final String THEIRS_LABEL = "theirs";

    private Merger() {
    }

    public static MergeResult merge(List<String> base, List<String> ours, List<String> theirs) {
        List<Change> oursChanges = changesOf(base, ours);
        List<Change> theirsChanges = changesOf(base, theirs);
        List<String> out = new ArrayList<>();
        int conflicts = 0;
        int basePos = 0;
        int i = 0;
        int j = 0;
        while (i < oursChanges.size() || j < theirsChanges.size()) {
            Change c1 = i < oursChanges.size() ? oursChanges.get(i) : null;
            Change c2 = j < theirsChanges.size() ? theirsChanges.get(j) : null;
            if (c2 == null || (c1 != null && strictlyBefore(c1, c2))) {
                out.addAll(base.subList(basePos, c1.start));
                out.addAll(c1.replacement);
                basePos = c1.end;
                i++;
            } else if (c1 == null || strictlyBefore(c2, c1)) {
                out.addAll(base.subList(basePos, c2.start));
                out.addAll(c2.replacement);
                basePos = c2.end;
                j++;
            } else {
                // Overlapping regions: expand to cover every overlapping change.
                int start = Math.min(c1.start, c2.start);
                int end = Math.max(c1.end, c2.end);
                List<Change> oursGroup = new ArrayList<>();
                List<Change> theirsGroup = new ArrayList<>();
                oursGroup.add(c1);
                i++;
                theirsGroup.add(c2);
                j++;
                while (i < oursChanges.size() && oursChanges.get(i).start < end) {
                    Change c = oursChanges.get(i);
                    end = Math.max(end, c.end);
                    oursGroup.add(c);
                    i++;
                }
                while (j < theirsChanges.size() && theirsChanges.get(j).start < end) {
                    Change c = theirsChanges.get(j);
                    end = Math.max(end, c.end);
                    theirsGroup.add(c);
                    j++;
                }
                out.addAll(base.subList(basePos, start));
                List<String> oursRegion = resolveRegion(base, start, end, oursGroup);
                List<String> theirsRegion = resolveRegion(base, start, end, theirsGroup);
                if (oursRegion.equals(theirsRegion)) {
                    out.addAll(oursRegion);
                } else {
                    out.add("<<<<<<< " + OURS_LABEL);
                    out.addAll(oursRegion);
                    out.add("=======");
                    out.addAll(theirsRegion);
                    out.add(">>>>>>> " + THEIRS_LABEL);
                    conflicts++;
                }
                basePos = end;
            }
        }
        out.addAll(base.subList(basePos, base.size()));
        return new MergeResult(out, conflicts);
    }

    /**
     * Whether {@code a} ends strictly before {@code b} starts. Two pure
     * insertions at the same position are considered overlapping so that
     * identical inserts merge into one and different inserts conflict.
     */
    private static boolean strictlyBefore(Change a, Change b) {
        if (a.end < b.start) {
            return true;
        }
        if (a.end == b.start) {
            boolean bothInserts = a.start == a.end && b.start == b.end;
            return !bothInserts;
        }
        return false;
    }

    /** Applies one side's changes to base[start, end) to get that side's view. */
    private static List<String> resolveRegion(List<String> base, int start, int end, List<Change> changes) {
        List<String> out = new ArrayList<>();
        int p = start;
        for (Change c : changes) {
            out.addAll(base.subList(p, c.start));
            out.addAll(c.replacement);
            p = c.end;
        }
        out.addAll(base.subList(p, end));
        return out;
    }

    /** Groups a diff into changes: [start, end) base lines replaced by replacement. */
    private static List<Change> changesOf(List<String> base, List<String> modified) {
        List<Edit> edits = LineDiffer.diff(base, modified);
        List<Change> changes = new ArrayList<>();
        int basePos = 0;
        int i = 0;
        while (i < edits.size()) {
            Edit e = edits.get(i);
            if (e.type() == EditType.KEEP) {
                basePos++;
                i++;
                continue;
            }
            int start = basePos;
            List<String> replacement = new ArrayList<>();
            while (i < edits.size() && edits.get(i).type() != EditType.KEEP) {
                Edit c = edits.get(i);
                if (c.type() == EditType.DELETE) {
                    basePos++;
                } else {
                    replacement.add(c.text());
                }
                i++;
            }
            changes.add(new Change(start, basePos, replacement));
        }
        return changes;
    }

    private record Change(int start, int end, List<String> replacement) {
    }
}
