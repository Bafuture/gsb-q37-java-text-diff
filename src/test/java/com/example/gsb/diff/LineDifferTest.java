package com.example.gsb.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineDifferTest {

    @Test
    void identicalTextsProduceOnlyKeeps() {
        List<String> lines = List.of("a", "b", "c");
        List<Edit> edits = LineDiffer.diff(lines, lines);
        assertThat(edits).allMatch(e -> e.type() == EditType.KEEP);
        assertThat(edits).hasSize(3);
    }

    @Test
    void emptyToEmptyProducesNoEdits() {
        assertThat(LineDiffer.diff(List.of(), List.of())).isEmpty();
    }

    @Test
    void emptyToNonEmptyIsAllInserts() {
        List<Edit> edits = LineDiffer.diff(List.of(), List.of("x", "y"));
        assertThat(edits).allMatch(e -> e.type() == EditType.INSERT);
        assertThat(edits).extracting(Edit::text).containsExactly("x", "y");
    }

    @Test
    void nonEmptyToEmptyIsAllDeletes() {
        List<Edit> edits = LineDiffer.diff(List.of("x", "y"), List.of());
        assertThat(edits).allMatch(e -> e.type() == EditType.DELETE);
        assertThat(edits).extracting(Edit::text).containsExactly("x", "y");
    }

    @Test
    void singleInsertionInMiddle() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "c"), List.of("a", "b", "c"));
        assertThat(edits).extracting(Edit::type)
                .containsExactly(EditType.KEEP, EditType.INSERT, EditType.KEEP);
        assertThat(edits.get(1).text()).isEqualTo("b");
    }

    @Test
    void singleDeletionInMiddle() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b", "c"), List.of("a", "c"));
        assertThat(edits).extracting(Edit::type)
                .containsExactly(EditType.KEEP, EditType.DELETE, EditType.KEEP);
        assertThat(edits.get(1).text()).isEqualTo("b");
    }

    @Test
    void replacementIsDeleteThenInsert() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "old", "c"), List.of("a", "new", "c"));
        assertThat(edits).extracting(Edit::type).containsExactly(
                EditType.KEEP, EditType.DELETE, EditType.INSERT, EditType.KEEP);
        assertThat(edits.get(1).text()).isEqualTo("old");
        assertThat(edits.get(2).text()).isEqualTo("new");
    }

    @Test
    void keepsOriginalLineOrderAcrossMultipleBlocks() {
        List<String> oldLines = List.of("1", "2", "3", "4", "5", "6", "7");
        List<String> newLines = List.of("1", "two", "3", "4", "five", "six", "7");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(Patcher.apply(oldLines, edits)).isEqualTo(newLines);
        assertThat(Patcher.revert(newLines, edits)).isEqualTo(oldLines);
    }

    @Test
    void duplicateLinesAreHandled() {
        List<String> oldLines = List.of("a", "a", "a");
        List<String> newLines = List.of("a", "a", "a", "a");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(edits).filteredOn(e -> e.type() == EditType.INSERT).hasSize(1);
        assertThat(Patcher.apply(oldLines, edits)).isEqualTo(newLines);
    }

    @Test
    void chineseLines() {
        List<String> oldLines = List.of("第一行", "第二行", "第三行");
        List<String> newLines = List.of("第一行", "改过的第二行", "第三行");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(edits).extracting(Edit::type).containsExactly(
                EditType.KEEP, EditType.DELETE, EditType.INSERT, EditType.KEEP);
        assertThat(Patcher.apply(oldLines, edits)).isEqualTo(newLines);
    }

    @Test
    void emojiLines() {
        List<String> oldLines = List.of("start 🚀", "middle 😀🎉", "end");
        List<String> newLines = List.of("start 🚀", "end");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(edits).extracting(Edit::type)
                .containsExactly(EditType.KEEP, EditType.DELETE, EditType.KEEP);
        assertThat(Patcher.apply(oldLines, edits)).isEqualTo(newLines);
    }

    @Test
    void veryLongLines() {
        String longA = "x".repeat(100_000) + "A";
        String longB = "x".repeat(100_000) + "B";
        List<Edit> edits = LineDiffer.diff(List.of("head", longA), List.of("head", longB));
        assertThat(edits).extracting(Edit::type)
                .containsExactly(EditType.KEEP, EditType.DELETE, EditType.INSERT);
        assertThat(Patcher.apply(List.of("head", longA), edits)).isEqualTo(List.of("head", longB));
    }

    @Test
    void completelyDifferentTexts() {
        List<String> oldLines = List.of("a", "b", "c");
        List<String> newLines = List.of("x", "y");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(Patcher.apply(oldLines, edits)).isEqualTo(newLines);
        assertThat(edits).filteredOn(e -> e.type() == EditType.KEEP).isEmpty();
    }
}
