package egovframework.admin.common.masking;

/**
 * 개인정보 마스킹 방식 (docs/04-features/10-masking.md 4절). 방식은 항목별로 고정이며 설정으로 바꾸지 않는다 (BR-05).
 * 어떤 화면·엑셀에서 가릴지는 {@link MaskingService}가 마스킹 설정으로 정한다.
 */
public final class Masking {

    /** 개인정보 항목 (코드 그룹 PRIVACY_FIELD, tb_masking_policy.field_cd) */
    public enum Field { USER_NM, EMAIL, MOBILE_NO, BIRTH_DATE }

    private Masking() {
    }

    public static String apply(Field field, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (field) {
            case USER_NM -> name(value);
            case EMAIL -> email(value);
            case MOBILE_NO -> mobile(value);
            case BIRTH_DATE -> birthDate(value);
        };
    }

    /** 홍길동 → 홍*동, 김철 → 김*, 박세나라 → 박**라 */
    public static String name(String value) {
        if (value.length() <= 1) {
            return "*";
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }

    /** hong@example.com → ho***@example.com (아이디 앞 두 글자만 남긴다) */
    public static String email(String value) {
        int at = value.indexOf('@');
        if (at < 0) {
            return "***";
        }
        String local = value.substring(0, at);
        return local.substring(0, Math.min(2, Math.max(local.length() - 1, 1))) + "***" + value.substring(at);
    }

    /** 01012345678 → 010-****-5678 */
    public static String mobile(String value) {
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return "***";
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    /** 1990-01-01 → 1990-**-** */
    public static String birthDate(String value) {
        return value.length() >= 4 ? value.substring(0, 4) + "-**-**" : "****-**-**";
    }
}
