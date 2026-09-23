package com.example.gsb.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MergerTest {

    @Test
    void nonOverlappingEditsMergeCleanly() {
        List<String> base = List.of("1", "2", "3", "4", "5");
        List<String> ours = List.of("one", "2", "3", "4", "5");
        List<String> theirs = List.of("1", "2", "3", "4", "five");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isFalse();
        assertThat(result.lines()).containsExactly("one", "2", "3", "4", "five");
    }

    @Test
    void identicalChangeOnBothSidesAppliesOnce() {
        List<String> base = List.of("a", "b", "c");
        List<String> ours = List.of("a", "B", "c");
        List<String> theirs = List.of("a", "B", "c");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isFalse();
        assertThat(result.lines()).containsExactly("a", "B", "c");
    }

    @Test
    void conflictingEditProducesConflictMarkers() {
        List<String> base = List.of("a", "b", "c");
        List<String> ours = List.of("a", "ours", "c");
        List<String> theirs = List.of("a", "theirs", "c");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isTrue();
        assertThat(result.conflictCount()).isEqualTo(1);
        assertThat(result.lines()).containsExactly(
                "a", "<<<<<<< ours", "ours", "=======", "theirs", ">>>>>>> theirs", "c");
    }

    @Test
    void deleteVersusEditIsAConflict() {
        List<String> base = List.of("a", "b", "c");
        List<String> ours = List.of("a", "c");
        List<String> theirs = List.of("a", "B", "c");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isTrue();
        assertThat(result.lines()).containsExactly(
                "a", "<<<<<<< ours", "=======", "B", ">>>>>>> theirs", "c");
    }

    @Test
    void bothSidesInsertAtSamePointWithSameContent() {
        List<String> base = List.of("a", "b");
        List<String> ours = List.of("a", "new", "b");
        List<String> theirs = List.of("a", "new", "b");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isFalse();
        assertThat(result.lines()).containsExactly("a", "new", "b");
    }

    @Test
    void bothSidesInsertDifferentContentAtSamePoint() {
        List<String> base = List.of("a", "b");
        List<String> ours = List.of("a", "ours", "b");
        List<String> theirs = List.of("a", "theirs", "b");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isTrue();
        assertThat(result.lines()).containsExactly(
                "a", "<<<<<<< ours", "ours", "=======", "theirs", ">>>>>>> theirs", "b");
    }

    @Test
    void emptyBaseWithBothSidesAdding() {
        MergeResult result = Merger.merge(List.of(), List.of("x"), List.of("x"));
        assertThat(result.hasConflicts()).isFalse();
        assertThat(result.lines()).containsExactly("x");
    }

    @Test
    void chineseAndEmojiMerge() {
        List<String> base = List.of("标题", "正文 🚀", "结尾");
        List<String> ours = List.of("新标题", "正文 🚀", "结尾");
        List<String> theirs = List.of("标题", "正文 🎉", "结尾");
        MergeResult result = Merger.merge(base, ours, theirs);
        assertThat(result.hasConflicts()).isFalse();
        assertThat(result.lines()).containsExactly("新标题", "正文 🎉", "结尾");
    }

    @Test
    void oneSideUnchangedTakesOtherSide() {
        List<String> base = List.of("a", "b", "c");
        List<String> theirs = List.of("a", "b", "c", "d");
        MergeResult result = Merger.merge(base, base, theirs);
        assertThat(result.hasConflicts()).isFalse();
        assertThat(result.lines()).containsExactly("a", "b", "c", "d");
    }
}
