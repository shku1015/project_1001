package egovframework.admin.web.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import egovframework.admin.common.masking.Masking;

/**
 * 마스킹 방식 (docs/04-features/10-masking.md 4절의 예).
 */
class MaskingTest {

    @Test
    void 이름은_첫_글자와_끝_글자만_남긴다() {
        assertThat(Masking.name("홍길동")).isEqualTo("홍*동");
        assertThat(Masking.name("김철")).isEqualTo("김*");
        assertThat(Masking.name("박세나라")).isEqualTo("박**라");
        assertThat(Masking.name("이")).isEqualTo("*");
    }

    @Test
    void 이메일은_아이디_앞_두_글자만_남긴다() {
        assertThat(Masking.email("hong@example.com")).isEqualTo("ho***@example.com");
        assertThat(Masking.email("ab@example.com")).isEqualTo("a***@example.com");
    }

    @Test
    void 휴대폰_번호는_가운데를_가린다() {
        assertThat(Masking.mobile("01012345678")).isEqualTo("010-****-5678");
        assertThat(Masking.mobile("0101234567")).isEqualTo("010-****-4567");
    }

    @Test
    void 생년월일은_연도만_남긴다() {
        assertThat(Masking.birthDate("1990-01-01")).isEqualTo("1990-**-**");
    }
}
