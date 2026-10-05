package egovframework.admin.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import egovframework.admin.web.support.AuthTestSupport;
import egovframework.admin.web.support.IntegrationTestWithData;
import egovframework.admin.web.support.OpenApiContract;

/**
 * 공통 코드 API: 동작과 함께 응답이 api/openapi.yaml과 맞는지(계약) 확인한다.
 */
@IntegrationTestWithData
class CommonCodeApiContractTest {

    private String AUTH;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void login() throws Exception {
        AUTH = AuthTestSupport.login(mockMvc, "t_viewer").bearer();
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/common/codes/USER_STATUS"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        OpenApiContract.assertValid(result);
    }

    @Test
    void 그룹코드의_상세코드를_정렬순서대로_준다() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/common/codes/USER_STATUS").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("ACTIVE"))
                .andExpect(jsonPath("$.data[3].codeNm").value("탈퇴"))
                .andReturn();
        OpenApiContract.assertValid(result);
    }

    @Test
    void 여러_그룹을_한번에_준다() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/common/codes")
                        .queryParam("groups", "USER_TYPE,ACTION")
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.USER_TYPE.length()").value(2))
                .andExpect(jsonPath("$.data.ACTION.length()").value(6))
                .andReturn();
        OpenApiContract.assertValid(result);
    }

    @Test
    void 없는_그룹코드는_404_NOT_FOUND() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/common/codes/NO_SUCH_GROUP").header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andReturn();
        OpenApiContract.assertValid(result);
    }
}
