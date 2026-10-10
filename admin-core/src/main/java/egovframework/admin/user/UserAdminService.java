package egovframework.admin.user;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.audit.AuditLogService;
import egovframework.admin.audit.AuditLogService.AuditEntry;
import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;
import egovframework.admin.common.PageQuery;
import egovframework.admin.common.PageResult;
import egovframework.admin.common.TempPassword;
import egovframework.admin.common.excel.ExcelWriter;
import egovframework.admin.common.masking.Masking;
import egovframework.admin.common.masking.Masking.Field;
import egovframework.admin.common.masking.MaskingService;
import egovframework.admin.user.UserAdminMapper.CompanyOption;
import egovframework.admin.user.UserAdminMapper.DetailRow;
import egovframework.admin.user.UserAdminMapper.HistoryRow;
import egovframework.admin.user.UserAdminMapper.ListRow;
import egovframework.admin.user.UserAdminMapper.Search;
import egovframework.admin.user.UserAdminMapper.UserValues;

/**
 * 사용자관리 (docs/04-features/02-user.md). 개인정보(이름·이메일·휴대폰·생년월일)는 마스킹 설정에 따라 가려서 주고,
 * 원문 보기·수정 화면 진입·엑셀 다운로드는 감사로그를 남긴다 (07-nonfunctional NF-PI-12).
 * 감사로그의 개인정보는 마스킹한 값으로 남긴다 (NF-PI-13).
 */
@Service
public class UserAdminService {

    private static final String USER = "USER";
    private static final Map<String, String> SORTABLE = Map.of(
            "loginId", "u.login_id", "userNm", "u.user_nm", "joinDt", "u.join_dt");
    private static final String DEFAULT_ORDER = "u.join_dt DESC";
    private static final String TIE_BREAKER = "u.user_id DESC";
    private static final int COMPANY_OPTION_LIMIT = 100;

    public record UserListItem(long userId, String userTypeCd, String userTypeNm, String loginId, String userNm,
                               String email, String mobileNo, Long companyId, String companyNm, String statusCd,
                               String statusNm, LocalDateTime joinDt) {
    }

    public record UserDetail(long userId, String userTypeCd, String userTypeNm, String loginId, String userNm,
                             String email, String mobileNo, Long companyId, String companyNm, String statusCd,
                             String statusNm, LocalDateTime joinDt, String birthDate, String deptNm, String positionNm,
                             String joinPath, String pwdTempYn, LocalDateTime lastLoginDt, LocalDateTime withdrawDt,
                             List<Field> maskedFields, String regNm, LocalDateTime regDt, String modNm,
                             LocalDateTime modDt) {
    }

    public record UserPrivacy(String userNm, String email, String mobileNo, LocalDate birthDate) {
    }

    public record UserForm(long userId, String userTypeCd, String loginId, String userNm, String email,
                           String mobileNo, LocalDate birthDate, Long companyId, String companyNm, String deptNm,
                           String positionNm, LocalDateTime modDt) {
    }

    public record CreateCommand(String userTypeCd, String loginId, UserValues values) {
    }

    public record Created(long userId, String tempPassword) {
    }

    private final UserAdminMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final MaskingService maskingService;

    public UserAdminService(UserAdminMapper mapper, PasswordEncoder passwordEncoder, AuditLogService auditLogService,
                            MaskingService maskingService) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.maskingService = maskingService;
    }

    // ================= 조회 =================

    /** USR-01 회원 목록. 개인정보로도 검색하지만 결과는 화면 마스킹 설정대로 가린다 */
    @Transactional(readOnly = true)
    public PageResult<UserListItem> getUsers(Search search, Integer page, Integer size, List<String> sort) {
        PageQuery query = PageQuery.of(page, size, sort, SORTABLE, DEFAULT_ORDER, TIE_BREAKER);
        long total = mapper.countUsers(search);
        List<ListRow> rows = total == 0 ? List.of()
                : mapper.selectUsers(search, query.orderBy(), query.size(), query.offset());
        MaskingService.Masker masker = maskingService.forScreen();
        return PageResult.of(rows.stream().map(r -> new UserListItem(r.userId(), r.userTypeCd(), r.userTypeNm(),
                r.loginId(), masker.mask(Field.USER_NM, r.userNm()), masker.mask(Field.EMAIL, r.email()),
                masker.mask(Field.MOBILE_NO, r.mobileNo()), r.companyId(), r.companyNm(), r.statusCd(), r.statusNm(),
                r.joinDt())).toList(), query, total);
    }

    /** USR-02 회원 상세. 가린 항목은 maskedFields로 알려 준다 */
    @Transactional(readOnly = true)
    public UserDetail getUser(long userId) {
        DetailRow u = find(userId);
        MaskingService.Masker masker = maskingService.forScreen();
        List<Field> masked = new ArrayList<>();
        String userNm = maskTracked(masker, Field.USER_NM, u.userNm(), masked);
        String email = maskTracked(masker, Field.EMAIL, u.email(), masked);
        String mobileNo = maskTracked(masker, Field.MOBILE_NO, u.mobileNo(), masked);
        String birthDate = maskTracked(masker, Field.BIRTH_DATE,
                u.birthDate() == null ? null : u.birthDate().toString(), masked);
        return new UserDetail(u.userId(), u.userTypeCd(), u.userTypeNm(), u.loginId(), userNm, email, mobileNo,
                u.companyId(), u.companyNm(), u.statusCd(), u.statusNm(), u.joinDt(), birthDate, u.deptNm(),
                u.positionNm(), u.joinPath(), u.pwdTempYn(), u.lastLoginDt(), u.withdrawDt(), masked, u.regNm(),
                u.regDt(), u.modNm(), u.modDt());
    }

    private static String maskTracked(MaskingService.Masker masker, Field field, String value, List<Field> masked) {
        if (value == null || !masker.on(field)) {
            return value;
        }
        masked.add(field);
        return Masking.apply(field, value);
    }

    /** USR-03 개인정보 원문 보기. 사유 코드(PRIVACY_REASON), 기타면 직접 입력. 부를 때마다 감사로그 */
    @Transactional
    public UserPrivacy viewPrivacy(long adminId, String ipAddr, long userId, String reasonCd, String reasonEtc) {
        DetailRow u = find(userId);
        String reasonNm = reasonCd == null ? null : mapper.selectPrivacyReasonNm(reasonCd);
        if (reasonNm == null) {
            throw BusinessException.field("reasonCd", "열람 사유를 고르세요.");
        }
        if ("ETC".equals(reasonCd) && (reasonEtc == null || reasonEtc.isBlank())) {
            throw BusinessException.field("reasonEtc", "기타 사유를 입력하세요.");
        }
        String reason = "ETC".equals(reasonCd) ? reasonNm + ": " + reasonEtc.trim() : reasonNm;
        privacyAudit(adminId, ipAddr, u, reason);
        return new UserPrivacy(u.userNm(), u.email(), u.mobileNo(), u.birthDate());
    }

    /** 수정 화면용 원문. 탈퇴 회원은 수정할 수 없다. 원문을 다루므로 감사로그(정보 정정)를 남긴다 */
    @Transactional
    public UserForm getForm(long adminId, String ipAddr, long userId) {
        DetailRow u = find(userId);
        if ("WITHDRAWN".equals(u.statusCd())) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
        privacyAudit(adminId, ipAddr, u, "정보 정정 (수정 화면)");
        return new UserForm(u.userId(), u.userTypeCd(), u.loginId(), u.userNm(), u.email(), u.mobileNo(),
                u.birthDate(), u.companyId(), u.companyNm(), u.deptNm(), u.positionNm(), u.modDt());
    }

    @Transactional(readOnly = true)
    public boolean isLoginIdAvailable(String loginId) {
        return !mapper.existsLoginId(loginId);
    }

    /** 소속 기업 선택 목록: 정상 기업만 (BR-04) */
    @Transactional(readOnly = true)
    public List<CompanyOption> getCompanyOptions(String keyword) {
        return mapper.selectCompanyOptions(keyword == null ? null : keyword.trim(), COMPANY_OPTION_LIMIT);
    }

    /** USR-10 상태 변경 이력 (최근순) */
    @Transactional(readOnly = true)
    public List<HistoryRow> getStatusHistories(long userId) {
        find(userId);
        return mapper.selectStatusHistories(userId);
    }

    /**
     * USR-11 엑셀. 엑셀 마스킹이 Y면 가리고, N이면 PRIVACY 권한이 있을 때만 원문 (마스킹 BR-03, 04).
     * 검색 조건·건수·마스킹 여부를 감사로그에 남긴다.
     */
    @Transactional
    public void writeExcel(long adminId, String ipAddr, boolean privacy, Search search, List<String> sort,
                           ExcelWriter.Target target) throws IOException {
        PageQuery query = PageQuery.of(1, 10, sort, SORTABLE, DEFAULT_ORDER, TIE_BREAKER);
        long total = mapper.countUsers(search);
        ExcelWriter.checkLimit(total);
        MaskingService.Masker masker = maskingService.forExcel(privacy);
        List<List<Object>> rows = mapper.selectUsers(search, query.orderBy(), null, 0).stream()
                .map(u -> List.<Object>of(nvl(u.userTypeNm()), u.loginId(), masker.mask(Field.USER_NM, u.userNm()),
                        masker.mask(Field.EMAIL, u.email()), nvl(masker.mask(Field.MOBILE_NO, u.mobileNo())),
                        nvl(u.companyNm()), nvl(u.statusNm()), u.joinDt()))
                .toList();
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("search", search);
        after.put("count", total);
        after.put("maskedFields", List.of(Field.values()).stream().filter(masker::on).toList());
        auditLogService.record(new AuditEntry(adminId, USER, "EXCEL", "USER", null,
                "회원 엑셀 다운로드 (" + total + "건)", null, after, null, ipAddr));
        ExcelWriter.write(target.open(), "회원",
                List.of("회원 구분", "로그인 아이디", "이름", "이메일", "휴대폰 번호", "소속 기업", "상태", "가입일시"), rows);
    }

    // ================= 등록·수정 =================

    /** USR-04 회원 등록. 정상 상태로 시작하고 가입 일시는 등록 일시 (BR-06). 임시 비밀번호를 한 번만 돌려준다 */
    @Transactional
    public Created create(long adminId, String ipAddr, CreateCommand cmd) {
        if (!"PERSONAL".equals(cmd.userTypeCd()) && !"CORPORATE".equals(cmd.userTypeCd())) {
            throw BusinessException.field("userTypeCd", "회원 구분을 고르세요.");
        }
        if (cmd.loginId() == null || !cmd.loginId().matches("^[a-z0-9]{4,50}$")) {
            throw BusinessException.field("loginId", "로그인 아이디는 영문 소문자·숫자 4~50자입니다.");
        }
        UserValues v = normalize(cmd.userTypeCd(), cmd.values(), null);
        if (mapper.existsLoginId(cmd.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "이미 사용 중인 로그인 아이디입니다.");
        }
        String tempPassword = TempPassword.generate();
        long userId = mapper.insertUser(cmd.userTypeCd(), cmd.loginId(), passwordEncoder.encode(tempPassword), v,
                adminId);
        Map<String, Object> after = masked(v);
        after.put("userTypeCd", cmd.userTypeCd());
        after.put("loginId", cmd.loginId());
        audit(adminId, ipAddr, "CREATE", userId, "회원 등록: " + cmd.loginId(), null, after, null);
        return new Created(userId, tempPassword);
    }

    /** USR-05, 06 정보·소속 기업 수정. 회원 구분·아이디는 바꾸지 않는다 (BR-01, 03). 탈퇴 회원은 불가 (BR-08) */
    @Transactional
    public void update(long adminId, String ipAddr, long userId, UserValues values, LocalDateTime modDt) {
        DetailRow before = find(userId);
        if ("WITHDRAWN".equals(before.statusCd())) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
        UserValues v = normalize(before.userTypeCd(), values, before.companyId());
        if (modDt == null || mapper.updateUser(userId, v, adminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        audit(adminId, ipAddr, "UPDATE", userId, "회원 수정: " + before.loginId(),
                masked(new UserValues(before.userNm(), before.email(), before.mobileNo(), before.birthDate(),
                        before.companyId(), before.deptNm(), before.positionNm())), masked(v), null);
    }

    // ================= 상태·비밀번호·삭제 =================

    /**
     * USR-07 상태 변경 (5절 상태 전이). 정지(정상→정지), 정지 해제(정지→정상), 휴면 해제(휴면→정상),
     * 강제 탈퇴(탈퇴가 아닌 상태→탈퇴). 탈퇴 회원은 바꿀 수 없다. 사유 필수, 이력을 남긴다 (BR-07, 08).
     */
    @Transactional
    public void changeStatus(long adminId, String ipAddr, long userId, String statusCd, String reason,
                             LocalDateTime modDt) {
        DetailRow u = find(userId);
        String from = u.statusCd();
        if ("WITHDRAWN".equals(from)) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
        boolean allowed = switch (statusCd == null ? "" : statusCd) {
            case "SUSPENDED" -> "ACTIVE".equals(from);
            case "ACTIVE" -> "SUSPENDED".equals(from) || "DORMANT".equals(from);
            case "WITHDRAWN" -> true;
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_CHANGE);
        }
        String r = requireReason(reason);
        if (mapper.updateStatus(userId, statusCd, adminId, modDt) == 0) {
            throw new BusinessException(ErrorCode.CONFLICT_MODIFIED);
        }
        mapper.insertStatusHist(userId, from, statusCd, r, adminId);
        audit(adminId, ipAddr, "UPDATE", userId, "회원 상태 변경: " + u.loginId() + " " + from + " → " + statusCd,
                Map.of("statusCd", from), Map.of("statusCd", statusCd), r);
    }

    /** USR-08 비밀번호 초기화. 탈퇴 회원은 불가. 회원은 사용자 서비스에서 로그인할 때 바꿔야 한다 */
    @Transactional
    public String resetPassword(long adminId, String ipAddr, long userId) {
        DetailRow u = find(userId);
        if ("WITHDRAWN".equals(u.statusCd())) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
        String tempPassword = TempPassword.generate();
        mapper.updateTempPassword(userId, passwordEncoder.encode(tempPassword), adminId);
        audit(adminId, ipAddr, "UPDATE", userId, "회원 비밀번호 초기화: " + u.loginId(), null, null, null);
        return tempPassword;
    }

    /** USR-09 삭제. 어느 상태에서든 삭제 표시로, 사유 필수 (BR-10, 11) */
    @Transactional
    public void delete(long adminId, String ipAddr, long userId, String reason) {
        DetailRow u = find(userId);
        String r = requireReason(reason);
        mapper.deleteUser(userId, adminId);
        audit(adminId, ipAddr, "DELETE", userId, "회원 삭제: " + u.loginId(), Map.of("loginId", u.loginId()), null, r);
    }

    // ================= 공통 =================

    private DetailRow find(long userId) {
        DetailRow u = mapper.selectUser(userId);
        if (u == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "회원이 없습니다: " + userId);
        }
        return u;
    }

    /** 입력값 확인. 기업 회원은 정상 기업이 필요하고(BR-04), 개인 회원은 소속 정보를 비운다 */
    private UserValues normalize(String userTypeCd, UserValues v, Long currentCompanyId) {
        String userNm = trim(v.userNm());
        if (userNm == null || userNm.length() > 50) {
            throw BusinessException.field("userNm", "이름은 1~50자로 입력하세요.");
        }
        String email = trim(v.email());
        if (email == null || email.length() > 100 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw BusinessException.field("email", "이메일 형식이 올바르지 않습니다.");
        }
        String mobileNo = trim(v.mobileNo());
        if (mobileNo != null && !mobileNo.matches("^[0-9]{10,11}$")) {
            throw BusinessException.field("mobileNo", "휴대폰 번호는 숫자 10~11자리입니다.");
        }
        if (v.birthDate() != null && !v.birthDate().isBefore(LocalDate.now())) {
            throw BusinessException.field("birthDate", "생년월일은 오늘 이전이어야 합니다.");
        }
        if (!"CORPORATE".equals(userTypeCd)) {
            return new UserValues(userNm, email, mobileNo, v.birthDate(), null, null, null);
        }
        if (v.companyId() == null) {
            throw new BusinessException(ErrorCode.COMPANY_REQUIRED);
        }
        // 소속 기업을 바꿀 때만 정상 기업인지 본다 (지금 소속 기업이 정지돼도 다른 정보는 고칠 수 있다)
        if (!v.companyId().equals(currentCompanyId) && !"ACTIVE".equals(mapper.selectCompanyStatus(v.companyId()))) {
            throw new BusinessException(ErrorCode.COMPANY_NOT_ACTIVE);
        }
        String deptNm = trim(v.deptNm());
        String positionNm = trim(v.positionNm());
        if (deptNm != null && deptNm.length() > 100 || positionNm != null && positionNm.length() > 50) {
            throw BusinessException.field(deptNm != null && deptNm.length() > 100 ? "deptNm" : "positionNm",
                    "부서는 100자, 직위는 50자 이내로 입력하세요.");
        }
        return new UserValues(userNm, email, mobileNo, v.birthDate(), v.companyId(), deptNm, positionNm);
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw BusinessException.field("reason", "사유를 500자 이내로 입력하세요.");
        }
        return reason.trim();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Object nvl(String value) {
        return value == null ? "" : value;
    }

    /** 감사로그용: 개인정보는 마스킹한 값 (NF-PI-13) */
    private static Map<String, Object> masked(UserValues v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userNm", Masking.apply(Field.USER_NM, v.userNm()));
        m.put("email", Masking.apply(Field.EMAIL, v.email()));
        m.put("mobileNo", Masking.apply(Field.MOBILE_NO, v.mobileNo()));
        m.put("birthDate", v.birthDate() == null ? null : Masking.apply(Field.BIRTH_DATE, v.birthDate().toString()));
        m.put("companyId", v.companyId());
        m.put("deptNm", v.deptNm());
        m.put("positionNm", v.positionNm());
        return m;
    }

    private void privacyAudit(long adminId, String ipAddr, DetailRow u, String reason) {
        audit(adminId, ipAddr, "PRIVACY", u.userId(), "개인정보 원문 보기: " + u.loginId(), null, null, reason);
    }

    private void audit(long adminId, String ipAddr, String action, long userId, String summary, Object before,
                       Object after, String reason) {
        auditLogService.record(new AuditEntry(adminId, USER, action, "USER", String.valueOf(userId), summary, before,
                after, reason, ipAddr));
    }
}
