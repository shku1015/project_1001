package egovframework.admin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.auth.AuthTypes.ClientInfo;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;

/**
 * Refresh Token (docs/04-features/09-auth.md 1.1, ADR-0004).
 * - DB에는 SHA-256 해시만 저장한다.
 * - 한 번 쓰면 새 토큰으로 바꾼다. 만료 시각은 처음 로그인 기준을 넘지 않는다 (NF 1.4).
 * - 이미 쓴 토큰이 다시 오면 그 관리자의 토큰을 모두 폐기한다 (재사용 감지).
 */
@Service
public class RefreshTokenService {

    public record IssuedToken(String rawToken, LocalDateTime expiresDt) {
    }

    public record Rotation(long adminId, IssuedToken newToken) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenMapper mapper;

    public RefreshTokenService(RefreshTokenMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public IssuedToken issue(long adminId, LocalDateTime expiresDt, ClientInfo client) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        mapper.insertToken(adminId, hash(raw), expiresDt, client.ipAddr(), client.userAgent());
        return new IssuedToken(raw, expiresDt);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public Rotation rotate(String rawToken, ClientInfo client) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_FAILED);
        }
        RefreshTokenMapper.RefreshTokenRow row = mapper.selectByHash(hash(rawToken));
        if (row == null || "Y".equals(row.revokedYn()) || row.expiresDt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.REFRESH_FAILED);
        }
        if ("Y".equals(row.usedYn()) || mapper.markUsed(row.tokenId()) == 0) {
            // 재사용 감지: 탈취 가능성이 있으므로 이 관리자의 토큰을 모두 폐기한다
            mapper.revokeAll(row.adminId());
            throw new BusinessException(ErrorCode.REFRESH_FAILED);
        }
        return new Rotation(row.adminId(), issue(row.adminId(), row.expiresDt(), client));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            mapper.revokeByHash(hash(rawToken));
        }
    }

    @Transactional
    public void revokeAll(long adminId) {
        mapper.revokeAll(adminId);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
