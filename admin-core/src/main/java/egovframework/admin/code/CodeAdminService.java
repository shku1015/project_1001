package egovframework.admin.code;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.code.CodeMapper.CodeDetailRequest;
import egovframework.admin.code.CodeMapper.CodeGroupRequest;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;

/**
 * 코드관리 (docs/04-features/04-code.md). 조회(코드 콤보)는 {@link CodeService}가 맡는다.
 * 코드를 바꾸면 코드 캐시를 비운다 (BR-07).
 */
@Service
public class CodeAdminService {

    private static final String MENU = "CODE";

    private final CodeMapper codeMapper;
    private final AuditLogService auditLogService;

    public CodeAdminService(CodeMapper codeMapper, AuditLogService auditLogService) {
        this.codeMapper = codeMapper;
        this.auditLogService = auditLogService;
    }

    // ================= 그룹코드 =================

    @Transactional(readOnly = true)
    public List<CodeGroupSummary> getGroups(String keyword, String useYn) {
        return codeMapper.selectGroups(blankToNull(keyword), blankToNull(useYn));
    }

    @Transactional(readOnly = true)
    public CodeGroup getGroup(String groupCd) {
        return requireGroup(groupCd);
    }

    @Transactional
    @CacheEvict(cacheNames = CodeService.CACHE, allEntries = true)
    public void createGroup(long adminId, String ipAddr, CodeGroupRequest request) {
        if (codeMapper.existsGroup(request.groupCd())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 그룹코드입니다.");
        }
        codeMapper.insertGroup(request);
        audit(adminId, ipAddr, "CREATE", request.groupCd(), "그룹코드 등록: " + request.groupCd(), null, request);
    }

    @Transactional
    @CacheEvict(cacheNames = CodeService.CACHE, allEntries = true)
    public void updateGroup(long adminId, String ipAddr, String groupCd, String groupNm, String description,
                            String useYn, java.time.LocalDateTime modDt) {
        CodeGroup before = requireGroup(groupCd);
        // 시스템 코드 그룹은 사용 여부를 바꿀 수 없다 (BR-03) → 기존 값 유지
        String appliedUseYn = before.isSystem() ? before.useYn() : useYn;
        int updated = codeMapper.updateGroup(groupCd, groupNm, description, appliedUseYn, modDt);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(adminId, ipAddr, "UPDATE", groupCd, "그룹코드 수정: " + groupCd, before,
                codeMapper.selectGroup(groupCd));
    }

    @Transactional
    @CacheEvict(cacheNames = CodeService.CACHE, allEntries = true)
    public void deleteGroup(long adminId, String ipAddr, String groupCd) {
        CodeGroup before = requireGroup(groupCd);
        if (before.isSystem()) {
            throw new BusinessException(ErrorCode.CODE_SYSTEM_PROTECTED);
        }
        if (codeMapper.countCodes(groupCd) > 0) {
            throw new BusinessException(ErrorCode.CODE_GROUP_HAS_CODES);
        }
        codeMapper.deleteGroup(groupCd);
        audit(adminId, ipAddr, "DELETE", groupCd, "그룹코드 삭제: " + groupCd, before, null);
    }

    // ================= 상세코드 =================

    @Transactional(readOnly = true)
    public List<CodeDetail> getDetails(String groupCd) {
        requireGroup(groupCd);
        return codeMapper.selectDetails(groupCd);
    }

    @Transactional(readOnly = true)
    public int nextSortOrd(String groupCd) {
        Integer max = codeMapper.selectMaxSortOrd(groupCd);
        return max == null ? 1 : max + 1;
    }

    @Transactional
    @CacheEvict(cacheNames = CodeService.CACHE, allEntries = true)
    public void createDetail(long adminId, String ipAddr, String groupCd, CodeDetailRequest request) {
        requireNonSystemGroup(groupCd);    // 시스템 코드 그룹은 상세코드를 추가할 수 없다 (BR-03)
        if (codeMapper.existsDetail(groupCd, request.code())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 코드입니다.");
        }
        codeMapper.insertDetail(groupCd, request);
        audit(adminId, ipAddr, "CREATE", groupCd + "/" + request.code(),
                "상세코드 등록: " + groupCd + "/" + request.code(), null, request);
    }

    @Transactional
    @CacheEvict(cacheNames = CodeService.CACHE, allEntries = true)
    public void updateDetail(long adminId, String ipAddr, String groupCd, String code, String codeNm, int sortOrd,
                             String description, String useYn, java.time.LocalDateTime modDt) {
        CodeGroup group = requireGroup(groupCd);
        CodeDetail before = requireDetail(groupCd, code);
        // 시스템 코드 그룹은 사용 여부를 바꿀 수 없다 (BR-03). 이름·정렬·설명만 반영
        String appliedUseYn = group.isSystem() ? before.useYn() : useYn;
        int updated = codeMapper.updateDetail(groupCd, code, codeNm, sortOrd, description, appliedUseYn, modDt);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(adminId, ipAddr, "UPDATE", groupCd + "/" + code, "상세코드 수정: " + groupCd + "/" + code,
                before, codeMapper.selectDetail(groupCd, code));
    }

    @Transactional
    @CacheEvict(cacheNames = CodeService.CACHE, allEntries = true)
    public void deleteDetail(long adminId, String ipAddr, String groupCd, String code) {
        requireNonSystemGroup(groupCd);    // 시스템 코드 그룹은 상세코드를 삭제할 수 없다 (BR-03)
        CodeDetail before = requireDetail(groupCd, code);
        codeMapper.deleteDetail(groupCd, code);
        audit(adminId, ipAddr, "DELETE", groupCd + "/" + code, "상세코드 삭제: " + groupCd + "/" + code, before, null);
    }

    // ================= 공통 =================

    private CodeGroup requireGroup(String groupCd) {
        CodeGroup group = codeMapper.selectGroup(groupCd);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "코드 그룹이 없습니다: " + groupCd);
        }
        return group;
    }

    private void requireNonSystemGroup(String groupCd) {
        if (requireGroup(groupCd).isSystem()) {
            throw new BusinessException(ErrorCode.CODE_SYSTEM_PROTECTED);
        }
    }

    private CodeDetail requireDetail(String groupCd, String code) {
        CodeDetail detail = codeMapper.selectDetail(groupCd, code);
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "코드가 없습니다: " + groupCd + "/" + code);
        }
        return detail;
    }

    private void audit(long adminId, String ipAddr, String action, String targetId, String summary,
                       Object before, Object after) {
        auditLogService.record(new AuditEntry(adminId, MENU, action, "CODE", targetId, summary, before, after, null,
                ipAddr));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
