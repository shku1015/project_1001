package egovframework.admin.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 감사로그 기록 (docs/04-features/README.md 3.6).
 * 원래 작업과 같은 트랜잭션에서 기록한다. 기록에 실패하면 원래 작업도 취소된다.
 * 개인정보는 마스킹한 값으로 넘겨야 한다 (NF-PI-13). 비밀번호·토큰은 넘기지 않는다.
 */
@Service
public class AuditLogService {

    public record AuditEntry(long adminId, String menuCd, String actionCd, String targetType, String targetId,
                             String summary, Object beforeData, Object afterData, String reason, String ipAddr) {
    }

    @Mapper
    public interface AuditLogMapper {

        @Insert("""
                INSERT INTO tb_audit_log (admin_id, menu_cd, action_cd, target_type, target_id, summary,
                                          before_data, after_data, reason, ip_addr)
                VALUES (#{e.adminId}, #{e.menuCd}, #{e.actionCd}, #{e.targetType}, #{e.targetId}, #{e.summary},
                        #{before}::jsonb, #{after}::jsonb, #{e.reason}, #{e.ipAddr})
                """)
        void insert(@Param("e") AuditEntry entry, @Param("before") String beforeJson, @Param("after") String afterJson);
    }

    private final AuditLogMapper mapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEntry entry) {
        mapper.insert(entry, toJson(entry.beforeData()), toJson(entry.afterData()));
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("감사로그 데이터를 JSON으로 바꿀 수 없습니다", e);
        }
    }
}
