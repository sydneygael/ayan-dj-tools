package com.djtools.ayan.musictagger.domain.model.vo;

import com.djtools.ayan.musictagger.domain.model.AudioFeatures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CamelotKeyTest {

    @Test
    void from_mapsMajorAndMinorReferenceKeys() {
        assertThat(CamelotKey.from("C", "major")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("8B"));
        assertThat(CamelotKey.from("A", "minor")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("8A"));
        assertThat(CamelotKey.from("G", "major")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("9B"));
        assertThat(CamelotKey.from("E", "minor")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("9A"));
        assertThat(CamelotKey.from("B", "major")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("1B"));
        assertThat(CamelotKey.from("F", "major")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("7B"));
        assertThat(CamelotKey.from("D", "minor")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("7A"));
    }

    @Test
    void from_handlesSharpAndFlatAliases() {
        // C#/Db major → 3B
        assertThat(CamelotKey.from("C#", "major")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("3B"));
        assertThat(CamelotKey.from("Db", "major")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("3B"));
        // A#/Bb minor → 3A
        assertThat(CamelotKey.from("A#", "minor")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("3A"));
        assertThat(CamelotKey.from("Bb", "minor")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("3A"));
        // Unicode accidentals
        assertThat(CamelotKey.from("F♯", "minor")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("11A"));
    }

    @Test
    void from_acceptsModePrefixesCaseInsensitive() {
        assertThat(CamelotKey.from("C", "MAJOR")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("8B"));
        assertThat(CamelotKey.from("C", "min")).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("5A"));
    }

    @Test
    void from_emptyWhenUnmappable() {
        assertThat(CamelotKey.from(null, "major")).isEmpty();
        assertThat(CamelotKey.from("C", null)).isEmpty();
        assertThat(CamelotKey.from("", "major")).isEmpty();
        assertThat(CamelotKey.from("H", "major")).isEmpty();
        assertThat(CamelotKey.from("C", "dorian")).isEmpty();
    }

    @Test
    void fromAudioFeatures_derivesKey() {
        var af = new AudioFeatures(null, null, null, null, null, null, 128.0, "A", "minor", 4);
        assertThat(CamelotKey.fromAudioFeatures(af)).hasValueSatisfying(k -> assertThat(k.code()).isEqualTo("8A"));
        assertThat(CamelotKey.fromAudioFeatures(null)).isEmpty();
    }

    @Test
    void compatible_includesSelfNeighborsAndModeSwap() {
        var key8a = new CamelotKey(8, 'A');
        assertThat(key8a.compatible())
                .extracting(CamelotKey::code)
                .containsExactlyInAnyOrder("8A", "9A", "7A", "8B");
    }

    @Test
    void compatible_wrapsAroundTheWheel() {
        // 12 +1 → 1
        assertThat(new CamelotKey(12, 'B').compatible())
                .extracting(CamelotKey::code)
                .contains("1B", "11B", "12A");
        // 1 -1 → 12
        assertThat(new CamelotKey(1, 'A').compatible())
                .extracting(CamelotKey::code)
                .contains("2A", "12A", "1B");
    }

    @Test
    void isCompatibleWith_respectsRules() {
        var key8a = new CamelotKey(8, 'A');
        assertThat(key8a.isCompatibleWith(new CamelotKey(8, 'A'))).isTrue();  // same
        assertThat(key8a.isCompatibleWith(new CamelotKey(9, 'A'))).isTrue();  // +1
        assertThat(key8a.isCompatibleWith(new CamelotKey(7, 'A'))).isTrue();  // -1
        assertThat(key8a.isCompatibleWith(new CamelotKey(8, 'B'))).isTrue();  // mode swap
        assertThat(key8a.isCompatibleWith(new CamelotKey(10, 'A'))).isFalse(); // +2
        assertThat(key8a.isCompatibleWith(new CamelotKey(9, 'B'))).isFalse();  // not relative
        assertThat(key8a.isCompatibleWith(null)).isFalse();
    }

    @Test
    void constructor_rejectsOutOfRange() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new CamelotKey(0, 'A'))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new CamelotKey(13, 'A'))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new CamelotKey(8, 'C'))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
