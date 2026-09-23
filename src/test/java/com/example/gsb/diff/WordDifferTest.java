package com.example.gsb.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WordDifferTest {

    private static String kept(List<WordEdit> edits) {
        return edits.stream()
                .filter(e -> e.type() != EditType.INSERT)
                .map(WordEdit::text)
                .collect(Collectors.joining());
    }

    private static String result(List<WordEdit> edits) {
        return edits.stream()
                .filter(e -> e.type() != EditType.DELETE)
                .map(WordEdit::text)
                .collect(Collectors.joining());
    }

    @Test
    void changedWordInMiddleOfLine() {
        List<WordEdit> edits = WordDiffer.diff("the quick brown fox", "the quick red fox");
        assertThat(kept(edits)).isEqualTo("the quick brown fox");
        assertThat(result(edits)).isEqualTo("the quick red fox");
        assertThat(edits).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(EditType.DELETE);
            assertThat(e.text()).isEqualTo("brown");
        });
        assertThat(edits).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(EditType.INSERT);
            assertThat(e.text()).isEqualTo("red");
        });
    }

    @Test
    void identicalLinesAreAllKeeps() {
        List<WordEdit> edits = WordDiffer.diff("hello world", "hello world");
        assertThat(edits).allMatch(e -> e.type() == EditType.KEEP);
    }

    @Test
    void chineseCharactersAreIndividualTokens() {
        List<WordEdit> edits = WordDiffer.diff("我喜欢苹果", "我喜欢香蕉");
        assertThat(kept(edits)).isEqualTo("我喜欢苹果");
        assertThat(result(edits)).isEqualTo("我喜欢香蕉");
        assertThat(edits).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(EditType.DELETE);
            assertThat(e.text()).isEqualTo("苹");
        });
    }

    @Test
    void emojiAreSingleTokens() {
        List<WordEdit> edits = WordDiffer.diff("ship it 🚀 now", "ship it 🎉 now");
        assertThat(result(edits)).isEqualTo("ship it 🎉 now");
        assertThat(edits).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(EditType.DELETE);
            assertThat(e.text()).isEqualTo("🚀");
        });
        assertThat(edits).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo(EditType.INSERT);
            assertThat(e.text()).isEqualTo("🎉");
        });
    }

    @Test
    void tokensReassembleIntoOriginalLine() {
        String line = "  int  x = foo(bar, 中文, 🚀);  ";
        assertThat(String.join("", WordDiffer.tokenize(line))).isEqualTo(line);
    }

    @Test
    void punctuationAndWhitespaceArePreserved() {
        List<WordEdit> edits = WordDiffer.diff("a,  b;c", "a,  b;d");
        assertThat(result(edits)).isEqualTo("a,  b;d");
        assertThat(kept(edits)).isEqualTo("a,  b;c");
    }

    @Test
    void emptyLines() {
        assertThat(WordDiffer.diff("", "")).isEmpty();
        List<WordEdit> edits = WordDiffer.diff("", "new words");
        assertThat(edits).allMatch(e -> e.type() == EditType.INSERT);
        assertThat(result(edits)).isEqualTo("new words");
    }
}
