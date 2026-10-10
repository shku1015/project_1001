package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;
import egovframework.admin.web.support.OpenApiContract;

/**
 * 사용자관리 API (docs/06-api/02-user.md, docs/04-features/02-user.md BR-01~12, 10-masking.md).
 * t_member(회원운영자)는 USER의 READ·CREATE·UPDATE·EXCEL·PRIVACY를 갖고 DELETE는 없다. t_viewer는 READ만.
 * 테스트 데이터 회원은 바꾸지 않고, 바꾸는 시나리오는 itusr* 회원을 등록해 쓰고 끝나면 지운다
 * (회원 수·상태 이력 건수를 확인하는 TestDataTest에 영향을 주지 않게).
 */
@IntegrationTestWithData
class UserApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String member;

    @BeforeEach
    void login() throws Exception {
        member = AuthTestSupport.login(mockMvc, "t_member").bearer();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_user_status_hist WHERE user_id IN (SELECT user_id FROM tb_user WHERE login_id LIKE 'itusr%')");
        jdbc.update("DELETE FROM tb_user WHERE login_id LIKE 'itusr%'");
        jdbc.update("UPDATE tb_masking_policy SET screen_mask_yn = 'Y', excel_mask_yn = 'Y'");
    }

    // ------------------------------------------------------------------ 도우미

    private MvcResult call(String token, MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(req.header("Authorization", token)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        OpenApiContract.assertValid(result);
        return result;
    }

    private MvcResult call(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        return call(member, req, expectedStatus);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static <T> T read(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    private static String error(MvcResult r) throws Exception {
        return read(r, "$.error.code");
    }

    private long userId(String loginId) {
        return jdbc.queryForObject("SELECT user_id FROM tb_user WHERE login_id = ?", Long.class, loginId);
    }

    private long companyId(String bizRegNo) {
        return jdbc.queryForObject("SELECT company_id FROM tb_company WHERE biz_reg_no = ?", Long.class, bizRegNo);
    }

    private long create(String body) throws Exception {
        MvcResult r = call(json(post("/api/v1/users"), body), 201);
        return ((Number) read(r, "$.data.userId")).longValue();
    }

    private long createPersonal() throws Exception {
        String loginId = "itusr" + SEQ.incrementAndGet() + System.nanoTime() % 100000;
        return create("""
                {"userTypeCd": "PERSONAL", "loginId": "%s", "userNm": "홍길동", "email": "%s@example.com",
                 "mobileNo": "01012345678", "birthDate": "1990-05-01"}""".formatted(loginId, loginId));
    }

    private String modDt(long userId) throws Exception {
        return read(call(get("/api/v1/users/" + userId), 200), "$.data.modDt");
    }

    // ------------------------------------------------------------------ 조회·마스킹

    @Test
    void 목록은_개인정보를_가려서_주지만_원문으로_검색한다() throws Exception {
        MvcResult r = call(get("/api/v1/users?userNm={n}", "세나"), 200);
        assertThat(read(r, "$.data.items[*].loginId").toString()).isEqualTo("[\"p_user03\"]");
        assertThat(read(r, "$.data.items[0].userNm").toString()).isEqualTo("박**라");
        assertThat(read(r, "$.data.items[0].mobileNo").toString()).isEqualTo("010-****-0003");
        assertThat(read(r, "$.data.items[0].email").toString()).isEqualTo("p_***@example.com");

        assertThat(read(call(get("/api/v1/users?mobileNo=01000000003"), 200), "$.data.items[*].loginId").toString())
                .isEqualTo("[\"p_user03\"]");
        assertThat(((Number) read(call(get("/api/v1/users?companyId=" + companyId("1000000001")), 200),
                "$.data.totalCount")).longValue()).isEqualTo(4);
        assertThat(read(call(get("/api/v1/users?joinPath=ADMIN&size=50"), 200), "$.data.items[*].loginId").toString())
                .contains("p_user09", "c_user07").doesNotContain("p_user01");
        // 삭제된 회원(p_user10)은 나오지 않는다
        assertThat(((Number) read(call(get("/api/v1/users?loginId=p_user10"), 200), "$.data.totalCount")).longValue())
                .isZero();
    }

    @Test
    void 화면_마스킹을_끄면_원문으로_준다() throws Exception {
        jdbc.update("UPDATE tb_masking_policy SET screen_mask_yn = 'N' WHERE field_cd = 'USER_NM'");
        MvcResult r = call(get("/api/v1/users/" + userId("p_user03")), 200);
        assertThat(read(r, "$.data.userNm").toString()).isEqualTo("박세나라");
        assertThat(read(r, "$.data.maskedFields").toString()).doesNotContain("USER_NM").contains("EMAIL");
    }

    @Test
    void 상세는_가린_항목을_알려_주고_원문_보기는_사유와_감사로그가_필요하다() throws Exception {
        long id = userId("p_user01");
        MvcResult detail = call(get("/api/v1/users/" + id), 200);
        assertThat(read(detail, "$.data.maskedFields").toString())
                .isEqualTo("[\"USER_NM\",\"EMAIL\",\"MOBILE_NO\",\"BIRTH_DATE\"]");
        assertThat(read(detail, "$.data.birthDate").toString()).isEqualTo("1990-**-**");
        assertThat(read(detail, "$.data.joinPath").toString()).isEqualTo("USER_SERVICE");

        MvcResult raw = call(json(post("/api/v1/users/" + id + "/privacy"), "{\"reasonCd\": \"CS_INQUIRY\"}"), 200);
        assertThat(read(raw, "$.data.userNm").toString()).isEqualTo("김하나");
        assertThat(read(raw, "$.data.birthDate").toString()).isEqualTo("1990-01-01");
        assertThat(raw.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(jdbc.queryForObject("""
                SELECT reason FROM tb_audit_log WHERE menu_cd = 'USER' AND action_cd = 'PRIVACY' AND target_id = ?
                ORDER BY log_id DESC LIMIT 1""", String.class, String.valueOf(id))).isEqualTo("고객 문의 응대");

        // 기타는 직접 입력이 필요하다, 없는 사유 코드
        assertThat(read(call(json(post("/api/v1/users/" + id + "/privacy"), "{\"reasonCd\": \"ETC\"}"), 400),
                "$.error.fieldErrors[0].field").toString()).isEqualTo("reasonEtc");
        call(json(post("/api/v1/users/" + id + "/privacy"), "{\"reasonCd\": \"NOPE\"}"), 400);
        // PRIVACY 권한이 없으면 403
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        assertThat(mockMvc.perform(json(post("/api/v1/users/" + id + "/privacy"), "{\"reasonCd\": \"CS_INQUIRY\"}")
                .header("Authorization", viewer)).andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void 소속_기업_선택은_정상_기업만() throws Exception {
        MvcResult r = call(get("/api/v1/users/company-options?keyword={k}", "테스트"), 200);
        assertThat(read(r, "$.data[*].companyNm").toString()).contains("(주)테스트상사").doesNotContain("테스트유통(주)");
    }

    // ------------------------------------------------------------------ 등록·수정

    @Test
    void 등록하면_정상_상태와_임시_비밀번호로_시작한다() throws Exception {
        String loginId = "itusr" + SEQ.incrementAndGet() + System.nanoTime() % 100000;
        MvcResult r = call(json(post("/api/v1/users"), """
                {"userTypeCd": "CORPORATE", "loginId": "%s", "userNm": "기업회원", "email": "%s@example.com",
                 "companyId": %d, "deptNm": "영업팀", "positionNm": "대리"}""".formatted(loginId, loginId,
                companyId("1000000001"))), 201);
        assertThat(read(r, "$.data.tempPassword").toString()).hasSize(10);
        long id = ((Number) read(r, "$.data.userId")).longValue();
        MvcResult detail = call(get("/api/v1/users/" + id), 200);
        assertThat(read(detail, "$.data.statusCd").toString()).isEqualTo("ACTIVE");
        assertThat(read(detail, "$.data.joinPath").toString()).isEqualTo("ADMIN");
        assertThat(read(detail, "$.data.pwdTempYn").toString()).isEqualTo("Y");
        assertThat(read(detail, "$.data.companyNm").toString()).isEqualTo("(주)테스트상사");

        // 아이디 중복, 기업 회원은 소속 기업 필수, 정지 기업은 고를 수 없다 (BR-01, 04)
        assertThat(error(call(json(post("/api/v1/users"), """
                {"userTypeCd": "PERSONAL", "loginId": "%s", "userNm": "중복", "email": "d@example.com"}"""
                .formatted(loginId)), 409))).isEqualTo("DUPLICATE");
        assertThat(error(call(json(post("/api/v1/users"), """
                {"userTypeCd": "CORPORATE", "loginId": "itusrnocomp", "userNm": "기업", "email": "n@example.com"}"""), 409)))
                .isEqualTo("COMPANY_REQUIRED");
        assertThat(error(call(json(post("/api/v1/users"), """
                {"userTypeCd": "CORPORATE", "loginId": "itusrsusp", "userNm": "기업", "email": "s@example.com",
                 "companyId": %d}""".formatted(companyId("1000000004"))), 409))).isEqualTo("COMPANY_NOT_ACTIVE");
        // 감사로그에는 개인정보를 가린 값으로 남긴다
        assertThat(jdbc.queryForObject("""
                SELECT after_data->>'userNm' FROM tb_audit_log WHERE menu_cd = 'USER' AND action_cd = 'CREATE'
                AND target_id = ?""", String.class, String.valueOf(id))).isEqualTo("기**원");
    }

    @Test
    void 등록과_수정은_PRIVACY_권한도_필요하다() throws Exception {
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        assertThat(mockMvc.perform(json(post("/api/v1/users"), "{}").header("Authorization", viewer)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(get("/api/v1/users/" + userId("p_user01") + "/form").header("Authorization", viewer))
                .andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void 수정_화면은_원문을_주고_감사로그를_남긴다() throws Exception {
        long id = createPersonal();
        MvcResult form = call(get("/api/v1/users/" + id + "/form"), 200);
        assertThat(read(form, "$.data.userNm").toString()).isEqualTo("홍길동");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_audit_log WHERE menu_cd = 'USER' AND action_cd = 'PRIVACY' AND target_id = ?""",
                Integer.class, String.valueOf(id))).isEqualTo(1);

        call(json(put("/api/v1/users/" + id), """
                {"userNm": "김바뀜", "email": "changed@example.com", "mobileNo": null, "birthDate": null,
                 "modDt": "%s"}""".formatted(modDt(id))), 200);
        assertThat(jdbc.queryForObject("SELECT user_nm FROM tb_user WHERE user_id = ?", String.class, id))
                .isEqualTo("김바뀜");
        assertThat(error(call(json(put("/api/v1/users/" + id), """
                {"userNm": "다시", "email": "x@example.com", "modDt": "2000-01-01T00:00:00"}"""), 409)))
                .isEqualTo("CONFLICT_MODIFIED");
        // 생년월일은 오늘 이전
        assertThat(read(call(json(put("/api/v1/users/" + id), """
                {"userNm": "미래", "email": "f@example.com", "birthDate": "2999-01-01", "modDt": "%s"}"""
                .formatted(modDt(id))), 400), "$.error.fieldErrors[0].field").toString()).isEqualTo("birthDate");
    }

    // ------------------------------------------------------------------ 상태·비밀번호·삭제

    @Test
    void 상태는_전이_규칙대로만_바뀌고_이력이_남는다() throws Exception {
        long id = createPersonal();
        // 정상에서 정상(정지 해제)은 허용하지 않는다
        assertThat(error(call(json(patch("/api/v1/users/" + id + "/status"), """
                {"statusCd": "ACTIVE", "reason": "해제", "modDt": "%s"}""".formatted(modDt(id))), 409)))
                .isEqualTo("INVALID_STATUS_CHANGE");
        call(json(patch("/api/v1/users/" + id + "/status"), """
                {"statusCd": "SUSPENDED", "reason": "약관 위반", "modDt": "%s"}""".formatted(modDt(id))), 200);
        call(json(patch("/api/v1/users/" + id + "/status"), """
                {"statusCd": "ACTIVE", "reason": "소명 확인", "modDt": "%s"}""".formatted(modDt(id))), 200);
        call(json(patch("/api/v1/users/" + id + "/status"), """
                {"statusCd": "WITHDRAWN", "reason": "강제 탈퇴", "modDt": "%s"}""".formatted(modDt(id))), 200);

        MvcResult hist = call(get("/api/v1/users/" + id + "/status-histories"), 200);
        assertThat(read(hist, "$.data[*].afterStatusCd").toString()).isEqualTo("[\"WITHDRAWN\",\"ACTIVE\",\"SUSPENDED\"]");
        assertThat(read(hist, "$.data[0].regNm").toString()).isEqualTo("테스트회원운영");
        MvcResult detail = call(get("/api/v1/users/" + id), 200);
        assertThat((Object) read(detail, "$.data.withdrawDt")).isNotNull();

        // 탈퇴 회원은 상태·정보·비밀번호를 바꿀 수 없다 (BR-08)
        assertThat(error(call(json(patch("/api/v1/users/" + id + "/status"), """
                {"statusCd": "ACTIVE", "reason": "복구", "modDt": "%s"}""".formatted(modDt(id))), 409)))
                .isEqualTo("USER_WITHDRAWN");
        assertThat(error(call(get("/api/v1/users/" + id + "/form"), 409))).isEqualTo("USER_WITHDRAWN");
        assertThat(error(call(post("/api/v1/users/" + id + "/password-reset"), 409))).isEqualTo("USER_WITHDRAWN");
    }

    @Test
    void 비밀번호를_초기화하면_임시_비밀번호_상태가_된다() throws Exception {
        long id = createPersonal();
        jdbc.update("UPDATE tb_user SET pwd_temp_yn = 'N' WHERE user_id = ?", id);
        MvcResult r = call(post("/api/v1/users/" + id + "/password-reset"), 200);
        assertThat(read(r, "$.data.tempPassword").toString()).hasSize(10);
        assertThat(jdbc.queryForObject("SELECT pwd_temp_yn FROM tb_user WHERE user_id = ?", String.class, id))
                .isEqualTo("Y");
    }

    @Test
    void 삭제는_사유가_필요하고_삭제한_아이디는_다시_쓸_수_없다() throws Exception {
        long id = createPersonal();
        String loginId = jdbc.queryForObject("SELECT login_id FROM tb_user WHERE user_id = ?", String.class, id);
        // 회원운영자는 삭제 권한이 없다
        assertThat(mockMvc.perform(json(delete("/api/v1/users/" + id), "{\"reason\": \"정리\"}")
                .header("Authorization", member)).andReturn().getResponse().getStatus()).isEqualTo(403);

        String superAdmin = AuthTestSupport.login(mockMvc, "t_super").bearer();
        call(superAdmin, json(delete("/api/v1/users/" + id), "{\"reason\": \"중복 등록 정리\"}"), 200);
        call(get("/api/v1/users/" + id), 404);
        assertThat((Boolean) read(call(get("/api/v1/users/check-login-id?loginId=" + loginId), 200),
                "$.data.available")).isFalse();
    }

    // ------------------------------------------------------------------ 엑셀

    @Test
    void 엑셀은_엑셀_마스킹_설정과_PRIVACY_권한을_따른다() throws Exception {
        assertThat(excelNames("p_user03")).containsExactly("박**라");
        // 엑셀 마스킹을 끄면 PRIVACY 권한이 있는 회원운영자에게는 원문
        jdbc.update("UPDATE tb_masking_policy SET excel_mask_yn = 'N' WHERE field_cd = 'USER_NM'");
        assertThat(excelNames("p_user03")).containsExactly("박세나라");
    }

    private List<String> excelNames(String loginId) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/users/excel?loginId=" + loginId).header("Authorization", member))
                .andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(r.getResponse().getContentAsByteArray()))) {
            var sheet = book.getSheetAt(0);
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                names.add(sheet.getRow(i).getCell(2).getStringCellValue());
            }
            return names;
        }
    }
}
