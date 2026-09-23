package com.example.gsb.diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PatcherTest {

    @Test
    void applyReconstructsNewText() {
        List<String> oldLines = List.of("a", "b", "c", "d");
        List<String> newLines = List.of("a", "B", "c", "c2", "d");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(Patcher.apply(oldLines, edits)).isEqualTo(newLines);
    }

    @Test
    void revertRestoresOriginalText() {
        List<String> oldLines = List.of("alpha", "beta", "gamma");
        List<String> newLines = List.of("beta", "gamma", "delta");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        assertThat(Patcher.revert(newLines, edits)).isEqualTo(oldLines);
    }

    @Test
    void applyOnEmptyBase() {
        List<Edit> edits = LineDiffer.diff(List.of(), List.of("only", "new"));
        assertThat(Patcher.apply(List.of(), edits)).isEqualTo(List.of("only", "new"));
    }

    @Test
    void mismatchedContextLineIsRejected() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b", "c"), List.of("a", "B", "c"));
        assertThatThrownBy(() -> Patcher.apply(List.of("a", "X", "c"), edits))
                .isInstanceOf(PatchException.class)
                .hasMessageContaining("line 2")
                .hasMessageContaining("expected <b>")
                .hasMessageContaining("found <X>");
    }

    @Test
    void mismatchedDeletedLineIsRejected() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b"), List.of("a"));
        assertThatThrownBy(() -> Patcher.apply(List.of("a", "other"), edits))
                .isInstanceOf(PatchException.class)
                .hasMessageContaining("line 2");
    }

    @Test
    void truncatedBaseIsRejected() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b", "c"), List.of("a", "b", "c", "d"));
        assertThatThrownBy(() -> Patcher.apply(List.of("a", "b"), edits))
                .isInstanceOf(PatchException.class)
                .hasMessageContaining("only 2 lines");
    }

    @Test
    void extraBaseLinesAreRejected() {
        List<Edit> edits = LineDiffer.diff(List.of("a"), List.of("a", "b"));
        assertThatThrownBy(() -> Patcher.apply(List.of("a", "extra"), edits))
                .isInstanceOf(PatchException.class)
                .hasMessageContaining("does not consume the whole base text");
    }

    @Test
    void revertWithWrongModifiedTextIsRejected() {
        List<Edit> edits = LineDiffer.diff(List.of("a", "b"), List.of("a", "b", "c"));
        assertThatThrownBy(() -> Patcher.revert(List.of("a", "b", "WRONG"), edits))
                .isInstanceOf(PatchException.class);
    }

    @Test
    void applyThenRevertIsIdentity() {
        List<String> oldLines = List.of("一", "二", "三", "🚀");
        List<String> newLines = List.of("一", "二改", "🚀", "四");
        List<Edit> edits = LineDiffer.diff(oldLines, newLines);
        List<String> applied = Patcher.apply(oldLines, edits);
        assertThat(applied).isEqualTo(newLines);
        assertThat(Patcher.revert(applied, edits)).isEqualTo(oldLines);
    }
}
