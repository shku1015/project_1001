package egovframework.admin.company;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.common.ApiResponse.FieldError;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.common.PageQuery;
import egovframework.admin.common.PageResult;
import egovframework.admin.common.excel.ExcelWriter;
import egovframework.admin.common.masking.Masking.Field;
import egovframework.admin.common.masking.MaskingService;
import egovframework.admin.company.CompanyMapper.DetailRow;
import egovframework.admin.company.CompanyMapper.ListRow;
import egovframework.admin.company.CompanyMapper.Search;
import egovframework.admin.company.CompanyMapper.UserRow;

/**
 * 기업정보관리 (docs/04-features/01-company.md). 승인 절차 없이 관리자가 바로 등록한다.
 * 사업자등록번호는 숫자 10자리, 중복 불가(삭제된 기업 포함), 등록 후 바꿀 수 없다 (BR-01, 02).
 * 삭제는 삭제 표시이고 소속 회원이 있으면 할 수 없다 (BR-03, 04). 상태 변경은 사유가 필요하다 (BR-06).
 */
@Service
public class CompanyAdminService {

    private static final String COMPANY = "COMPANY";
    private static final Map<String, String> SORTABLE = Map.of(
            "companyNm", "c.company_nm", "memberCnt", "member_cnt", "regDt", "c.reg_dt");
    private static final String DEFAULT_ORDER = "c.reg_dt DESC";
    private static final String TIE_BREAKER = "c.company_id DESC";

    /** 등록·수정 입력 (등록이면 bizRegNo도 받는다) */
    public record CompanyCommand(String companyNm, String ceoNm, String bizType, String bizItem, String telNo,
                                 String zipCd, String addr, String addrDtl) {
    }

    public record CompanyUsers(List<UserRow> items, long totalCount) {
    }

    private final CompanyMapper mapper;
    private final AuditLogService auditLogService;
    private final MaskingService maskingService;

    public CompanyAdminService(CompanyMapper mapper, AuditLogService auditLogService, MaskingService maskingService) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.maskingService = maskingService;
    }

    // ================= 조회 =================

    /** COM-01 기업 목록. 기본 정렬은 등록일시 역순 */
    @Transactional(readOnly = true)
    public PageResult<ListRow> getCompanies(Search search, Integer page, Integer size, List<String> sort) {
        PageQuery query = PageQuery.of(page, size, sort, SORTABLE, DEFAULT_ORDER, TIE_BREAKER);
        long total = mapper.countCompanies(search);
        List<ListRow> rows = total == 0 ? List.of()
                : mapper.selectCompanies(search, query.orderBy(), query.size(), query.offset());
        return PageResult.of(rows, query, total);
    }

    /** COM-02 기업 상세 */
    @Transactional(readOnly = true)
    public DetailRow getCompany(long companyId) {
        return find(companyId);
    }

    /** COM-07 소속 회원 (최근 가입순). 이름은 화면 마스킹 설정을 따른다 */
    @Transactional(readOnly = true)
    public CompanyUsers getUsers(long companyId, int size) {
        find(companyId);
        MaskingService.Masker masker = maskingService.forScreen();
        List<UserRow> users = mapper.selectUsers(companyId, size).stream()
                .map(u -> new UserRow(u.userId(), u.loginId(), masker.mask(Field.USER_NM, u.userNm()), u.deptNm(),
                        u.positionNm(), u.statusCd(), u.statusNm(), u.joinDt()))
                .toList();
        return new CompanyUsers(users, mapper.countUsers(companyId));
    }

    @Transactional(readOnly = true)
    public boolean isBizRegNoAvailable(String bizRegNo) {
        return !mapper.existsBizRegNo(bizRegNo);
    }

    /** COM-08 엑셀. 목록과 같은 조건·정렬. 10,000건을 넘으면 거부하고, 내려받은 조건·건수를 감사로그에 남긴다 */
    @Transactional
    public void writeExcel(long adminId, String ipAddr, Search search, List<String> sort, ExcelWriter.Target target)
            throws IOException {
        PageQuery query = PageQuery.of(1, 10, sort, SORTABLE, DEFAULT_ORDER, TIE_BREAKER);
        long total = mapper.countCompanies(search);
        ExcelWriter.checkLimit(total);
        List<List<Object>> rows = mapper.selectCompanies(search, query.orderBy(), null, 0).stream()
                .map(c -> List.<Object>of(c.companyNm(), bizRegNo(c.bizRegNo()), c.ceoNm(), c.memberCnt(),
                        c.statusNm() == null ? c.statusCd() : c.statusNm(), c.regDt()))
                .toList();
        auditLogService.record(new AuditEntry(adminId, COMPANY, "EXCEL", "COMPANY", null,
                "기업 엑셀 다운로드 (" + total + "건)", null, Map.of("search", search, "count", total), null, ipAddr));
        ExcelWriter.write(target.open(), "기업정보", List.of("기업명", "사업자등록번호", "대표자", "소속 회원 수", "상태", "등록일시"), rows);
    }

    // ================= 등록·수정·삭제 =================

    /** COM-03 기업 등록. 상태는 정상으로 시작 */
    @Transactional
    public long create(long adminId, String ipAddr, String bizRegNo, CompanyCommand cmd) {
        if (bizRegNo == null || !bizRegNo.matches("^[0-9]{10}$")) {
            throw BusinessException.field("bizRegNo", "사업자등록번호는 숫자 10자리입니다.");
        }
        CompanyCommand c = normalize(cmd);
        if (mapper.existsBizRegNo(bizRegNo)) {
            throw new BusinessException(ErrorCode.DUPLICATE, ErrorCode.DUPLICATE.message(),
                    List.of(new FieldError("bizRegNo", "이미 등록된 사업자등록번호입니다.")));
        }
        long companyId = mapper.insertCompany(c, bizRegNo, adminId);
        audit(adminId, ipAddr, "CREATE", companyId, "기업 등록: " + c.companyNm(), null, snapshot(bizRegNo, c));
        return companyId;
    }

    /** COM-04 기업 수정. 사업자등록번호는 바꾸지 않는다 */
    @Transactional
    public void update(long adminId, String ipAddr, long companyId, CompanyCommand cmd, LocalDateTime modDt) {
        DetailRow before = find(companyId);
        CompanyCommand c = normalize(cmd);
        if (modDt == null || mapper.updateCompany(companyId, c, adminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(adminId, ipAddr, "UPDATE", companyId, "기업 수정: " + c.companyNm(),
                snapshot(before.bizRegNo(), new CompanyCommand(before.companyNm(), before.ceoNm(), before.bizType(),
                        before.bizItem(), before.telNo(), before.zipCd(), before.addr(), before.addrDtl())),
                snapshot(before.bizRegNo(), c));
    }

    /** COM-05 상태 변경 (정상 ↔ 정지). 사유 필수, 같은 상태로는 바꿀 수 없다. 소속 회원 상태는 바꾸지 않는다 (BR-05) */
    @Transactional
    public void changeStatus(long adminId, String ipAddr, long companyId, String statusCd, String reason,
                             LocalDateTime modDt) {
        DetailRow company = find(companyId);
        if (!"ACTIVE".equals(statusCd) && !"SUSPENDED".equals(statusCd) || statusCd.equals(company.statusCd())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_CHANGE);
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw BusinessException.field("reason", "사유를 500자 이내로 입력하세요.");
        }
        if (mapper.updateStatus(companyId, statusCd, adminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        auditLogService.record(new AuditEntry(adminId, COMPANY, "UPDATE", "COMPANY", String.valueOf(companyId),
                ("SUSPENDED".equals(statusCd) ? "기업 정지: " : "기업 정지 해제: ") + company.companyNm(),
                Map.of("statusCd", company.statusCd()), Map.of("statusCd", statusCd), reason.trim(), ipAddr));
    }

    /** COM-06 기업 삭제. 소속 회원(탈퇴 포함)이 없을 때만, 삭제 표시로 */
    @Transactional
    public void delete(long adminId, String ipAddr, long companyId) {
        DetailRow company = find(companyId);
        if (mapper.countUsers(companyId) > 0) {
            throw new BusinessException(ErrorCode.COMPANY_HAS_MEMBERS);
        }
        mapper.deleteCompany(companyId, adminId);
        audit(adminId, ipAddr, "DELETE", companyId, "기업 삭제: " + company.companyNm(),
                Map.of("companyNm", company.companyNm(), "bizRegNo", company.bizRegNo()), null);
    }

    // ================= 공통 =================

    private DetailRow find(long companyId) {
        DetailRow c = mapper.selectCompany(companyId);
        if (c == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "기업이 없습니다: " + companyId);
        }
        return c;
    }

    private static CompanyCommand normalize(CompanyCommand c) {
        String companyNm = trim(c.companyNm());
        String ceoNm = trim(c.ceoNm());
        if (companyNm == null || companyNm.length() > 100) {
            throw BusinessException.field("companyNm", "기업명은 1~100자로 입력하세요.");
        }
        if (ceoNm == null || ceoNm.length() > 50) {
            throw BusinessException.field("ceoNm", "대표자명은 1~50자로 입력하세요.");
        }
        String telNo = trim(c.telNo());
        if (telNo != null && !telNo.matches("^[0-9]{9,11}$")) {
            throw BusinessException.field("telNo", "대표 전화번호는 숫자 9~11자리입니다.");
        }
        String zipCd = trim(c.zipCd());
        if (zipCd != null && !zipCd.matches("^[0-9]{5}$")) {
            throw BusinessException.field("zipCd", "우편번호는 숫자 5자리입니다.");
        }
        return new CompanyCommand(companyNm, ceoNm, limit(c.bizType(), "bizType", "업태"),
                limit(c.bizItem(), "bizItem", "종목"), telNo, zipCd, limit(c.addr(), "addr", "주소", 200),
                limit(c.addrDtl(), "addrDtl", "상세주소", 200));
    }

    private static String limit(String value, String field, String label) {
        return limit(value, field, label, 100);
    }

    private static String limit(String value, String field, String label, int max) {
        String v = trim(value);
        if (v != null && v.length() > max) {
            throw BusinessException.field(field, label + "은(는) " + max + "자 이내로 입력하세요.");
        }
        return v;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 1234567890 → 123-45-67890 (docs/05-ia-screens.md 4.1) */
    public static String bizRegNo(String value) {
        return value == null || value.length() != 10 ? value
                : value.substring(0, 3) + "-" + value.substring(3, 5) + "-" + value.substring(5);
    }

    private static Map<String, Object> snapshot(String bizRegNo, CompanyCommand c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bizRegNo", bizRegNo);
        m.put("companyNm", c.companyNm());
        m.put("ceoNm", c.ceoNm());
        m.put("bizType", c.bizType());
        m.put("bizItem", c.bizItem());
        m.put("telNo", c.telNo());
        m.put("zipCd", c.zipCd());
        m.put("addr", c.addr());
        m.put("addrDtl", c.addrDtl());
        return m;
    }

    private void audit(long adminId, String ipAddr, String action, long companyId, String summary, Object before,
                       Object after) {
        auditLogService.record(new AuditEntry(adminId, COMPANY, action, "COMPANY", String.valueOf(companyId), summary,
                before, after, null, ipAddr));
    }
}
