package egovframework.admin.masking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.common.masking.Masking;
import egovframework.admin.common.masking.Masking.Field;
import egovframework.admin.masking.MaskingAdminMapper.PolicyRow;

/**
 * 마스킹 설정 (docs/04-features/10-masking.md). 항목은 코드 그룹 PRIVACY_FIELD로 정해져 있어 바꾸기만 한다 (BR-08).
 * 설정은 요청마다 읽으므로 바꾸면 다음 요청부터 바로 반영된다 (BR-07).
 */
@Service
public class MaskingAdminService {

    private static final String MASKING = "MASKING";

    /** 항목별 마스킹 예에 쓰는 고정 견본 (BR-05 방식은 고정) */
    private static final Map<Field, String> SAMPLES = Map.of(
            Field.USER_NM, "홍길동", Field.EMAIL, "hong@example.com",
            Field.MOBILE_NO, "01012345678", Field.BIRTH_DATE, "1990-01-01");

    public record Policy(String fieldCd, String fieldNm, String example, String screenMaskYn, String excelMaskYn,
                         String modNm, LocalDateTime modDt) {
    }

    public record PolicyChange(String fieldCd, String screenMaskYn, String excelMaskYn, LocalDateTime modDt) {
    }

    private final MaskingAdminMapper mapper;
    private final AuditLogService auditLogService;

    public MaskingAdminService(MaskingAdminMapper mapper, AuditLogService auditLogService) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
    }

    /** MSK-01 설정 조회 (마스킹 예 포함) */
    @Transactional(readOnly = true)
    public List<Policy> getPolicies() {
        return mapper.selectPolicies().stream().map(p -> new Policy(p.fieldCd(), p.fieldNm(), example(p.fieldCd()),
                p.screenMaskYn(), p.excelMaskYn(), p.modNm(), p.modDt())).toList();
    }

    /** MSK-02 설정 변경. 바뀐 항목만 받는다. 하나라도 다른 관리자가 먼저 바꿨으면 전체를 저장하지 않는다 */
    @Transactional
    public void save(long adminId, String ipAddr, List<PolicyChange> changes) {
        if (changes == null || changes.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "바뀐 항목이 없습니다.");
        }
        Map<String, PolicyRow> current = new HashMap<>();
        mapper.selectPolicies().forEach(p -> current.put(p.fieldCd(), p));
        List<Map<String, String>> before = new ArrayList<>();
        List<Map<String, String>> after = new ArrayList<>();
        for (PolicyChange c : changes) {
            PolicyRow row = current.get(c.fieldCd());
            if (row == null || !yn(c.screenMaskYn()) || !yn(c.excelMaskYn())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "잘못된 마스킹 설정입니다: " + c.fieldCd());
            }
            if (mapper.updatePolicy(c.fieldCd(), c.screenMaskYn(), c.excelMaskYn(), adminId, c.modDt()) == 0) {
                throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);    // 트랜잭션이 전체를 되돌린다
            }
            before.add(Map.of("fieldCd", c.fieldCd(), "screenMaskYn", row.screenMaskYn(), "excelMaskYn", row.excelMaskYn()));
            after.add(Map.of("fieldCd", c.fieldCd(), "screenMaskYn", c.screenMaskYn(), "excelMaskYn", c.excelMaskYn()));
        }
        auditLogService.record(new AuditEntry(adminId, MASKING, "UPDATE", "MASKING_POLICY", null,
                "마스킹 설정 변경 (" + changes.size() + "개 항목)", before, after, null, ipAddr));
    }

    private static String example(String fieldCd) {
        try {
            Field field = Field.valueOf(fieldCd);
            return Masking.apply(field, SAMPLES.get(field));
        } catch (IllegalArgumentException e) {
            return "-";
        }
    }

    private static boolean yn(String value) {
        return "Y".equals(value) || "N".equals(value);
    }
}
