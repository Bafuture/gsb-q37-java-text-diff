package com.example.gsb.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnifiedDiffFormatterTest {

    @Test
    void singleHunkWithContextAndHeader() {
        List<String> oldLines = List.of("a", "b", "c", "d", "e");
        List<String> newLines = List.of("a", "b", "C", "d", "e");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        String out = UnifiedDiffFormatter.format(edits, 1);
        assertThat(out).isEqualTo("""
                @@ -2,3 +2,3 @@
                 b
                -c
                +C
                 d
                """);
    }

    @Test
    void fileHeadersAreIncluded() {
        List<Edit> edits = LineDiffer.diff(List.of("a"), List.of("b"));
        String out = UnifiedDiffFormatter.format("old.txt", "new.txt", edits, 3);
        assertThat(out).startsWith("--- old.txt\n+++ new.txt\n");
    }

    @Test
    void countOfOneOmitsComma() {
        List<Edit> edits = LineDiffer.diff(List.of("a"), List.of("b"));
        String out = UnifiedDiffFormatter.format(edits, 0);
        assertThat(out).startsWith("@@ -1 +1 @@");
    }

    @Test
    void pureInsertionUsesZeroCountHeader() {
        List<Edit> edits = LineDiffer.diff(List.of(), List.of("x", "y"));
        String out = UnifiedDiffFormatter.format(edits, 0);
        assertThat(out).isEqualTo("""
                @@ -0,0 +1,2 @@
                +x
                +y
                """);
    }

    @Test
    void pureDeletionUsesZeroCountHeader() {
        List<Edit> edits = LineDiffer.diff(List.of("x", "y"), List.of());
        String out = UnifiedDiffFormatter.format(edits, 0);
        assertThat(out).isEqualTo("""
                @@ -1,2 +0,0 @@
                -x
                -y
                """);
    }

    @Test
    void distantChangesProduceSeparateHunks() {
        List<String> oldLines = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
        List<String> newLines = List.of("one", "2", "3", "4", "5", "6", "7", "8", "9", "10", "eleven");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        String out = UnifiedDiffFormatter.format(edits, 1);
        assertThat(out.split("(?m)^@@ ", -1)).hasSize(3); // leading empty + 2 hunks
        assertThat(out).contains("@@ -1,2 +1,2 @@").contains("@@ -10,2 +10,2 @@");
    }

    @Test
    void nearbyChangesMergeIntoOneHunk() {
        List<String> oldLines = List.of("1", "2", "3", "4", "5");
        List<String> newLines = List.of("one", "2", "3", "4", "five");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        String out = UnifiedDiffFormatter.format(edits, 2);
        assertThat(out.split("@@", -1)).hasSize(3); // exactly one hunk header
    }

    @Test
    void zeroContextShowsOnlyChangedLines() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b", "c"), List.of("a", "B", "c"));
        String out = UnifiedDiffFormatter.format(edits, 0);
        assertThat(out).isEqualTo("""
                @@ -2 +2 @@
                -b
                +B
                """);
    }

    @Test
    void noChangesProduceEmptyOutput() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b"), List.of("a", "b"));
        assertThat(UnifiedDiffFormatter.format(edits, 3)).isEmpty();
    }

    @Test
    void multibyteContentIsRenderedVerbatim() {
        List<Edit> edits = LineDiffer.diff(List.of("你好 🚀"), List.of("你好 🎉"));
        String out = UnifiedDiffFormatter.format(edits, 0);
        assertThat(out).contains("-你好 🚀").contains("+你好 🎉");
    }
}
