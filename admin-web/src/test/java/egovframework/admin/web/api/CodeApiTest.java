package egovframework.admin.web.api;

import static egovframework.admin.web.support.AuthTestSupport.TEST_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * 코드관리 API (docs/06-api/04-code.md). 응답은 api/openapi.yaml과 대조한다.
 * 데이터를 바꾸므로 t_system 대신 전용 관리자를 만들어 쓰고, 테스트마다 전용 그룹코드를 쓴다.
 */
@IntegrationTestWithData
class CodeApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String system;    // CODE 전 권한
    private String viewer;    // READ만

    @BeforeEach
    void login() throws Exception {
        system = AuthTestSupport.login(mockMvc, "t_system").bearer();
        viewer = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
    }

    private MvcResult ok(MockHttpServletRequestBuilder req) throws Exception {
        MvcResult result = mockMvc.perform(req).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString())
                .isBetween(200, 299);
        OpenApiContract.assertValid(result);
        return result;
    }

    @Test
    void 그룹코드_목록과_상세코드_목록을_준다() throws Exception {
        MvcResult groups = ok(get("/api/v1/code-groups").header("Authorization", system));
        OpenApiContract.assertValid(groups);
        // 초기 그룹코드가 보인다
        assertThat(JsonPath.<java.util.List<String>>read(groups.getResponse().getContentAsString(), "$.data[*].groupCd"))
                .contains("USER_STATUS", "ACTION");

        MvcResult codes = ok(get("/api/v1/code-groups/USER_STATUS/codes").header("Authorization", system));
        assertThat(JsonPath.<java.util.List<String>>read(codes.getResponse().getContentAsString(), "$.data[*].code"))
                .containsExactly("ACTIVE", "DORMANT", "SUSPENDED", "WITHDRAWN");
    }

    @Test
    void 그룹코드를_등록_수정_삭제한다() throws Exception {
        String g = "IT_COD_G1";
        ok(post("/api/v1/code-groups").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content(("{\"groupCd\":\"" + g + "\",\"groupNm\":\"테스트그룹\",\"useYn\":\"Y\"}")));
        assertThat(jdbc.queryForObject("SELECT group_nm FROM tb_code_group WHERE group_cd = ?", String.class, g))
                .isEqualTo("테스트그룹");

        // 그룹 상세 응답도 명세와 대조한다 (isSystem() 같은 편의 메서드가 JSON에 섞이지 않는지)
        String modDt = JsonPath.read(ok(get("/api/v1/code-groups/" + g).header("Authorization", system))
                .getResponse().getContentAsString(), "$.data.modDt");
        ok(put("/api/v1/code-groups/" + g).header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content(("{\"groupNm\":\"바뀐그룹\",\"useYn\":\"N\",\"modDt\":\"" + modDt + "\"}")));
        assertThat(jdbc.queryForObject("SELECT group_nm FROM tb_code_group WHERE group_cd = ?", String.class, g))
                .isEqualTo("바뀐그룹");

        ok(delete("/api/v1/code-groups/" + g).header("Authorization", system));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tb_code_group WHERE group_cd = ?", Integer.class, g)).isZero();
    }

    @Test
    void 상세코드를_등록하고_정렬순서대로_나온다() throws Exception {
        String g = "IT_COD_G2";
        ok(post("/api/v1/code-groups").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content(("{\"groupCd\":\"" + g + "\",\"groupNm\":\"분류\",\"useYn\":\"Y\"}")));
        ok(post("/api/v1/code-groups/" + g + "/codes").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content(("{\"code\":\"B\",\"codeNm\":\"비\",\"sortOrd\":2,\"useYn\":\"Y\"}")));
        ok(post("/api/v1/code-groups/" + g + "/codes").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content(("{\"code\":\"A\",\"codeNm\":\"에이\",\"sortOrd\":1,\"useYn\":\"Y\"}")));

        MvcResult codes = ok(get("/api/v1/code-groups/" + g + "/codes").header("Authorization", system));
        assertThat(JsonPath.<java.util.List<String>>read(codes.getResponse().getContentAsString(), "$.data[*].code"))
                .containsExactly("A", "B");
    }

    @Test
    void 중복_그룹코드는_409_DUPLICATE() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/code-groups").header("Authorization", system)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupCd\":\"USER_STATUS\",\"groupNm\":\"중복\",\"useYn\":\"Y\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE"))
                .andReturn();
        OpenApiContract.assertValid(result);
    }

    @Test
    void 시스템_코드_그룹은_삭제_상세추가_불가_CODE_SYSTEM_PROTECTED() throws Exception {
        mockMvc.perform(delete("/api/v1/code-groups/USER_STATUS").header("Authorization", system))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CODE_SYSTEM_PROTECTED"));
        mockMvc.perform(post("/api/v1/code-groups/USER_STATUS/codes").header("Authorization", system)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NEW\",\"codeNm\":\"새값\",\"sortOrd\":9,\"useYn\":\"Y\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CODE_SYSTEM_PROTECTED"));
    }

    @Test
    void 시스템_코드_그룹은_사용여부를_바꿔도_무시한다() throws Exception {
        // BR-03: 이름·설명은 바뀌지만 use_yn은 Y로 유지된다
        String modDt = JsonPath.read(mockMvc.perform(get("/api/v1/code-groups/USER_STATUS").header("Authorization", system))
                .andReturn().getResponse().getContentAsString(), "$.data.modDt");
        ok(put("/api/v1/code-groups/USER_STATUS").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupNm\":\"회원 상태(수정)\",\"useYn\":\"N\",\"modDt\":\"" + modDt + "\"}"));
        assertThat(jdbc.queryForObject("SELECT use_yn FROM tb_code_group WHERE group_cd = 'USER_STATUS'", String.class))
                .isEqualTo("Y");
        assertThat(jdbc.queryForObject("SELECT group_nm FROM tb_code_group WHERE group_cd = 'USER_STATUS'", String.class))
                .isEqualTo("회원 상태(수정)");
        // 되돌린다 (다른 테스트에 영향 없게)
        String modDt2 = JsonPath.read(mockMvc.perform(get("/api/v1/code-groups/USER_STATUS").header("Authorization", system))
                .andReturn().getResponse().getContentAsString(), "$.data.modDt");
        mockMvc.perform(put("/api/v1/code-groups/USER_STATUS").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupNm\":\"회원 상태\",\"useYn\":\"Y\",\"modDt\":\"" + modDt2 + "\"}"));
    }

    @Test
    void 상세코드가_있는_그룹은_삭제_불가_CODE_GROUP_HAS_CODES() throws Exception {
        String g = "IT_COD_G3";
        ok(post("/api/v1/code-groups").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupCd\":\"" + g + "\",\"groupNm\":\"그룹\",\"useYn\":\"Y\"}"));
        ok(post("/api/v1/code-groups/" + g + "/codes").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"X\",\"codeNm\":\"엑스\",\"sortOrd\":1,\"useYn\":\"Y\"}"));
        mockMvc.perform(delete("/api/v1/code-groups/" + g).header("Authorization", system))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CODE_GROUP_HAS_CODES"));
    }

    @Test
    void 동시_수정이면_409_CONFLICT_MODIFIED() throws Exception {
        String g = "IT_COD_G4";
        ok(post("/api/v1/code-groups").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupCd\":\"" + g + "\",\"groupNm\":\"그룹\",\"useYn\":\"Y\"}"));
        mockMvc.perform(put("/api/v1/code-groups/" + g).header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupNm\":\"바뀜\",\"useYn\":\"Y\",\"modDt\":\"2000-01-01T00:00:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT_MODIFIED"));
    }

    @Test
    void 잘못된_그룹코드_형식은_400() throws Exception {
        // 일부러 형식에 맞지 않는 요청이라 계약 검증(요청 스키마)은 하지 않는다. 서버가 400으로 막는지만 본다
        mockMvc.perform(post("/api/v1/code-groups").header("Authorization", system)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupCd\":\"bad code\",\"groupNm\":\"x\",\"useYn\":\"Y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 코드관리_READ만_있으면_조회만_되고_등록은_403() throws Exception {
        mockMvc.perform(get("/api/v1/code-groups").header("Authorization", viewer))
                .andExpect(status().isForbidden());    // 조회전용은 CODE 권한이 없다
        mockMvc.perform(post("/api/v1/code-groups").header("Authorization", viewer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupCd\":\"X\",\"groupNm\":\"x\",\"useYn\":\"Y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 코드를_바꾸면_공통코드_조회에_바로_반영된다() throws Exception {
        // 캐시(BR-07): 새 그룹·코드를 만들면 /common/codes 가 바로 보여 준다
        String g = "IT_COD_CACHE";
        ok(post("/api/v1/code-groups").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupCd\":\"" + g + "\",\"groupNm\":\"캐시\",\"useYn\":\"Y\"}"));
        ok(post("/api/v1/code-groups/" + g + "/codes").header("Authorization", system).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ONE\",\"codeNm\":\"하나\",\"sortOrd\":1,\"useYn\":\"Y\"}"));
        MvcResult common = mockMvc.perform(get("/api/v1/common/codes/" + g).header("Authorization", system))
                .andExpect(status().isOk()).andReturn();
        assertThat(JsonPath.<java.util.List<String>>read(common.getResponse().getContentAsString(), "$.data[*].code"))
                .containsExactly("ONE");
    }
}
