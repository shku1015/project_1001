package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
 * 기업정보관리 API (docs/06-api/01-company.md, docs/04-features/01-company.md BR-01~06). 응답은 api/openapi.yaml과 대조한다.
 * 테스트 데이터 기업 5곳(1000000001~5)은 바꾸지 않고, 바꾸는 시나리오는 테스트마다 기업을 새로 등록해 쓴다.
 * t_super는 COMPANY 전 권한, t_member(회원운영자)는 삭제 권한이 없다.
 */
@IntegrationTestWithData
class CompanyApiTest {

    /** 테스트가 등록하는 사업자등록번호 (테스트 데이터와 겹치지 않게 9로 시작) */
    private static final AtomicLong BIZ = new AtomicLong(9_000_000_000L + System.nanoTime() % 100_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String superAdmin;

    @BeforeEach
    void login() throws Exception {
        superAdmin = AuthTestSupport.login(mockMvc, "t_super").bearer();
    }

    /** 테스트 데이터 건수를 확인하는 TestDataTest에 영향을 주지 않도록 등록한 기업을 지운다 */
    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tb_company WHERE biz_reg_no LIKE '9%'");
    }

    // ------------------------------------------------------------------ 도우미

    private MvcResult call(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(req.header("Authorization", superAdmin)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        OpenApiContract.assertValid(result);
        return result;
    }

    private int status(String token, MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", token)).andReturn().getResponse().getStatus();
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

    private long companyId(String bizRegNo) {
        return jdbc.queryForObject("SELECT company_id FROM tb_company WHERE biz_reg_no = ?", Long.class, bizRegNo);
    }

    private long create(String bizRegNo) throws Exception {
        MvcResult r = call(json(post("/api/v1/companies"), """
                {"companyNm": "테스트등록", "bizRegNo": "%s", "ceoNm": "등대표", "bizType": null, "bizItem": null,
                 "telNo": "0212345678", "zipCd": "06236", "addr": "서울특별시 강남구 테헤란로 1", "addrDtl": "10층"}
                """.formatted(bizRegNo)), 201);
        return ((Number) read(r, "$.data.companyId")).longValue();
    }

    private String modDt(long companyId) throws Exception {
        return read(call(get("/api/v1/companies/" + companyId), 200), "$.data.modDt");
    }

    // ------------------------------------------------------------------ 조회

    @Test
    void 목록은_검색_정렬_페이징을_한다() throws Exception {
        // 소속 회원 수 내림차순: (주)테스트상사 4명(탈퇴 포함)이 가장 많다
        MvcResult byMembers = call(get("/api/v1/companies?sort=memberCnt,desc&size=10"), 200);
        assertThat(read(byMembers, "$.data.items[0].companyNm").toString()).isEqualTo("(주)테스트상사");
        assertThat((Integer) read(byMembers, "$.data.items[0].memberCnt")).isEqualTo(4);

        MvcResult suspended = call(get("/api/v1/companies?statusCd=SUSPENDED"), 200);
        assertThat(read(suspended, "$.data.items[*].companyNm").toString()).contains("테스트유통(주)")
                .doesNotContain("테스트물산");
        MvcResult byNo = call(get("/api/v1/companies?bizRegNo=1000000003"), 200);
        assertThat(read(byNo, "$.data.items[*].companyNm").toString()).isEqualTo("[\"테스트물산\"]");
        String today = LocalDate.now().toString();
        MvcResult byDate = call(get("/api/v1/companies?regDtFrom=" + today + "&regDtTo=" + today), 200);
        assertThat(((Number) read(byDate, "$.data.totalCount")).longValue()).isGreaterThanOrEqualTo(5);
        MvcResult none = call(get("/api/v1/companies?regDtTo=2000-01-01"), 200);
        assertThat(((Number) read(none, "$.data.totalCount")).longValue()).isZero();
    }

    @Test
    void 상세와_소속_회원은_이름을_마스킹한다() throws Exception {
        long c1 = companyId("1000000001");
        MvcResult detail = call(get("/api/v1/companies/" + c1), 200);
        assertThat(read(detail, "$.data.bizType").toString()).isEqualTo("도소매");
        assertThat((Integer) read(detail, "$.data.memberCnt")).isEqualTo(4);

        MvcResult users = call(get("/api/v1/companies/" + c1 + "/users?size=10"), 200);
        assertThat(((Number) read(users, "$.data.totalCount")).longValue()).isEqualTo(4);
        assertThat(read(users, "$.data.items[*].userNm").toString()).contains("한*업", "서**부")
                .doesNotContain("한기업");
        MvcResult two = call(get("/api/v1/companies/" + c1 + "/users?size=2"), 200);
        assertThat(read(two, "$.data.items.length()").toString()).isEqualTo("2");
    }

    @Test
    void 권한이_없으면_403() throws Exception {
        String member = AuthTestSupport.login(mockMvc, "t_member").bearer();
        String viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
        assertThat(status(member, get("/api/v1/companies"))).isEqualTo(200);
        assertThat(status(member, delete("/api/v1/companies/" + companyId("1000000005")))).isEqualTo(403);
        assertThat(status(viewer, json(post("/api/v1/companies"), "{}"))).isEqualTo(403);
        assertThat(status(viewer, get("/api/v1/companies/excel"))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ 등록·수정·상태·삭제

    @Test
    void 등록하고_사업자등록번호는_중복될_수_없다() throws Exception {
        String bizRegNo = String.valueOf(BIZ.incrementAndGet());
        assertThat((Boolean) read(call(get("/api/v1/companies/check-biz-reg-no?bizRegNo=" + bizRegNo), 200),
                "$.data.available")).isTrue();
        long id = create(bizRegNo);
        MvcResult detail = call(get("/api/v1/companies/" + id), 200);
        assertThat(read(detail, "$.data.statusCd").toString()).isEqualTo("ACTIVE");
        assertThat(read(detail, "$.data.regNm").toString()).isEqualTo("테스트슈퍼");

        MvcResult dup = call(json(post("/api/v1/companies"), """
                {"companyNm": "중복", "bizRegNo": "%s", "ceoNm": "대표"}""".formatted(bizRegNo)), 409);
        assertThat(error(dup)).isEqualTo("DUPLICATE");
        assertThat(read(dup, "$.error.fieldErrors[0].field").toString()).isEqualTo("bizRegNo");
        // 형식 오류 (명세를 어기는 요청이므로 상태만 본다)
        assertThat(status(superAdmin, json(post("/api/v1/companies"), """
                {"companyNm": "형식", "bizRegNo": "123", "ceoNm": "대표"}"""))).isEqualTo(400);
    }

    @Test
    void 수정하면_사업자등록번호는_바뀌지_않는다() throws Exception {
        String bizRegNo = String.valueOf(BIZ.incrementAndGet());
        long id = create(bizRegNo);
        call(json(put("/api/v1/companies/" + id), """
                {"companyNm": "바뀐기업", "bizRegNo": "9999999999", "ceoNm": "새대표", "telNo": null,
                 "zipCd": null, "addr": null, "addrDtl": null, "modDt": "%s"}""".formatted(modDt(id))), 200);
        assertThat(jdbc.queryForObject("SELECT company_nm || '/' || biz_reg_no FROM tb_company WHERE company_id = ?",
                String.class, id)).isEqualTo("바뀐기업/" + bizRegNo);
        assertThat(error(call(json(put("/api/v1/companies/" + id), """
                {"companyNm": "다시", "ceoNm": "대표", "modDt": "2000-01-01T00:00:00"}"""), 409)))
                .isEqualTo("CONFLICT_MODIFIED");
    }

    @Test
    void 상태를_바꾸면_사유가_감사로그에_남는다() throws Exception {
        long id = create(String.valueOf(BIZ.incrementAndGet()));
        call(json(patch("/api/v1/companies/" + id + "/status"), """
                {"statusCd": "SUSPENDED", "reason": "휴업 확인", "modDt": "%s"}""".formatted(modDt(id))), 200);
        assertThat(jdbc.queryForObject("SELECT status_cd FROM tb_company WHERE company_id = ?", String.class, id))
                .isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("""
                SELECT reason FROM tb_audit_log WHERE menu_cd = 'COMPANY' AND target_id = ? ORDER BY log_id DESC LIMIT 1""",
                String.class, String.valueOf(id))).isEqualTo("휴업 확인");
        // 같은 상태로는 바꿀 수 없다
        assertThat(error(call(json(patch("/api/v1/companies/" + id + "/status"), """
                {"statusCd": "SUSPENDED", "reason": "다시", "modDt": "%s"}""".formatted(modDt(id))), 409)))
                .isEqualTo("INVALID_STATUS_CHANGE");
    }

    @Test
    void 소속_회원이_있으면_삭제할_수_없고_삭제한_번호는_다시_쓸_수_없다() throws Exception {
        assertThat(error(call(delete("/api/v1/companies/" + companyId("1000000001")), 409)))
                .isEqualTo("COMPANY_HAS_MEMBERS");

        String bizRegNo = String.valueOf(BIZ.incrementAndGet());
        long id = create(bizRegNo);
        call(delete("/api/v1/companies/" + id), 200);
        call(get("/api/v1/companies/" + id), 404);
        assertThat((Boolean) read(call(get("/api/v1/companies/check-biz-reg-no?bizRegNo=" + bizRegNo), 200),
                "$.data.available")).isFalse();
    }

    // ------------------------------------------------------------------ 엑셀

    @Test
    void 엑셀은_검색_결과를_xlsx로_내려준다() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/companies/excel?statusCd=ACTIVE&sort=companyNm,asc")
                .header("Authorization", superAdmin)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getContentType()).startsWith("application/vnd.openxmlformats");
        assertThat(r.getResponse().getHeader("Content-Disposition"))
                .contains("filename*=UTF-8''%EA%B8%B0%EC%97%85%EC%A0%95%EB%B3%B4%EA%B4%80%EB%A6%AC_");
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(r.getResponse().getContentAsByteArray()))) {
            var sheet = book.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("기업명");
            List<String> names = new java.util.ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                names.add(sheet.getRow(i).getCell(0).getStringCellValue());
                assertThat(sheet.getRow(i).getCell(4).getStringCellValue()).isEqualTo("정상");
            }
            assertThat(names).contains("(주)테스트상사").doesNotContain("테스트유통(주)");
        }
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_audit_log WHERE menu_cd = 'COMPANY' AND action_cd = 'EXCEL'""", Integer.class))
                .isPositive();
    }
}
