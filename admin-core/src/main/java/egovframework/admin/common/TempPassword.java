package egovframework.admin.common;

import java.security.SecureRandom;

/**
 * 임시 비밀번호 (관리자 BR-02, 회원 BR-02). 화면에 한 번만 보여 주고 해시로만 저장한다.
 * 헷갈리기 쉬운 글자(0/O, 1/l/I)는 쓰지 않는다.
 */
public final class TempPassword {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TempPassword() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
