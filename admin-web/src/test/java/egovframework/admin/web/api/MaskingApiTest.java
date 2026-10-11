package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
 * 마스킹 설정 API (docs/06-api/10-masking.md, docs/04-features/10-masking.md). 응답은 api/openapi.yaml과 대조한다.
 * t_system(시스템관리자)은 MASKING READ·UPDATE를 갖는다. 바꾼 설정은 테스트가 끝나면 모두 켬(Y)으로 되돌린다.
 */
@IntegrationTestWithData
class MaskingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String system;

    @BeforeEach
    void login() throws Exception {
        system = AuthTestSupport.login(mockMvc, "t_system").bearer();
    }

    @AfterEach
    void restore() {
        jdbc.update("UPDATE tb_masking_policy SET screen_mask_yn = 'Y', excel_mask_yn = 'Y'");
    }

    private MvcResult call(String token, MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(req.header("Authorization", token)).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isEqualTo(expectedStatus);
        OpenApiContract.assertValid(result);
        return result;
    }

    private static <T> T read(MvcResult r, String path) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), path);
    }

    private String modDt(String fieldCd) throws Exception {
        java.util.List<String> values = read(call(system, get("/api/v1/masking-policies"), 200),
                "$.data[?(@.fieldCd == '" + fieldCd + "')].modDt");
        return values.get(0);
    }

    private MockHttpServletRequestBuilder save(String policies) {
        return put("/api/v1/masking-policies").contentType(MediaType.APPLICATION_JSON)
                .content("{\"policies\": [" + policies + "]}");
    }

    @Test
    void 항목_순서대로_마스킹_예와_함께_준다() throws Exception {
        MvcResult r = call(system, get("/api/v1/masking-policies"), 200);
        assertThat(read(r, "$.data[*].fieldCd").toString())
                .isEqualTo("[\"USER_NM\",\"EMAIL\",\"MOBILE_NO\",\"BIRTH_DATE\"]");
        assertThat(read(r, "$.data[*].example").toString())
                .isEqualTo("[\"홍*동\",\"ho***@example.com\",\"010-****-5678\",\"1990-**-**\"]");
        assertThat(read(r, "$.data[0].fieldNm").toString()).isEqualTo("이름");
    }

    @Test
    void 바뀐_항목만_저장하고_감사로그를_남긴다() throws Exception {
        call(system, save("""
                {"fieldCd": "EMAIL", "screenMaskYn": "N", "excelMaskYn": "Y", "modDt": "%s"}""".formatted(modDt("EMAIL"))), 200);
        assertThat(jdbc.queryForObject("SELECT screen_mask_yn FROM tb_masking_policy WHERE field_cd = 'EMAIL'",
                String.class)).isEqualTo("N");
        MvcResult r = call(system, get("/api/v1/masking-policies"), 200);
        assertThat(read(r, "$.data[?(@.fieldCd == 'EMAIL')].modNm").toString()).isEqualTo("[\"테스트시스템\"]");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tb_audit_log WHERE menu_cd = 'MASKING' AND action_cd = 'UPDATE'""", Integer.class))
                .isPositive();
    }

    @Test
    void 하나라도_먼저_바뀌었으면_전체를_저장하지_않는다() throws Exception {
        MvcResult r = call(system, save("""
                {"fieldCd": "USER_NM", "screenMaskYn": "N", "excelMaskYn": "N", "modDt": "%s"},
                {"fieldCd": "EMAIL", "screenMaskYn": "N", "excelMaskYn": "N", "modDt": "2000-01-01T00:00:00"}"""
                .formatted(modDt("USER_NM"))), 409);
        assertThat(read(r, "$.error.code").toString()).isEqualTo("CONFLICT_MODIFIED");
        assertThat(jdbc.queryForObject("SELECT screen_mask_yn FROM tb_masking_policy WHERE field_cd = 'USER_NM'",
                String.class)).isEqualTo("Y");
    }

    @Test
    void 마스킹_권한이_없으면_403() throws Exception {
        String member = AuthTestSupport.login(mockMvc, "t_member").bearer();
        assertThat(mockMvc.perform(get("/api/v1/masking-policies").header("Authorization", member)).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
    }
}
