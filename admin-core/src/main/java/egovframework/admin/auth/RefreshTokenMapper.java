package egovframework.admin.auth;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {

    record RefreshTokenRow(long tokenId, long adminId, LocalDateTime expiresDt, String usedYn, String revokedYn) {
    }

    void insertToken(@Param("adminId") long adminId, @Param("tokenHash") String tokenHash,
                     @Param("expiresDt") LocalDateTime expiresDt, @Param("ipAddr") String ipAddr,
                     @Param("userAgent") String userAgent);

    RefreshTokenRow selectByHash(@Param("tokenHash") String tokenHash);

    /** 사용 처리. 이미 사용·폐기된 토큰이면 0을 돌려준다 (동시 재발급 방지) */
    int markUsed(@Param("tokenId") long tokenId);

    void revokeByHash(@Param("tokenHash") String tokenHash);

    void revokeAll(@Param("adminId") long adminId);
}
