package egovframework.admin.web.support;

import static org.assertj.core.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;

/**
 * 계약 테스트 도우미 (ADR-0020): MockMvc로 실행한 요청·응답이 api/openapi.yaml과 맞는지 검증한다.
 *
 * <pre>
 * MvcResult result = mockMvc.perform(get("/api/v1/...").header("Authorization", "Bearer test")).andReturn();
 * OpenApiContract.assertValid(result);
 * </pre>
 */
public final class OpenApiContract {

    /** 저장소 루트의 명세 파일. 테스트는 admin-web 모듈 폴더에서 실행된다. */
    public static final Path SPEC = Path.of("..", "api", "openapi.yaml").toAbsolutePath().normalize();

    private static final OpenApiInteractionValidator VALIDATOR = OpenApiInteractionValidator
            .createForSpecificationUrl(SPEC.toUri().toString())
            .withBasePathOverride("/api/v1")
            .build();

    private OpenApiContract() {
    }

    public static void assertValid(MvcResult result) {
        ValidationReport report = VALIDATOR.validate(toRequest(result.getRequest()), toResponse(result.getResponse()));
        if (report.hasErrors()) {
            fail("api/openapi.yaml과 맞지 않습니다: %s %s%n%s",
                    result.getRequest().getMethod(), result.getRequest().getRequestURI(), report);
        }
    }

    private static com.atlassian.oai.validator.model.Request toRequest(MockHttpServletRequest req) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(Request.Method.valueOf(req.getMethod()), req.getRequestURI());
        req.getParameterMap().forEach((name, values) -> {
            if (req.getQueryString() != null && req.getQueryString().contains(name + "=")) {
                builder.withQueryParam(name, values);
            }
        });
        for (String name : java.util.Collections.list(req.getHeaderNames())) {
            builder.withHeader(name, java.util.Collections.list(req.getHeaders(name)));
        }
        byte[] body = req.getContentAsByteArray();
        if (body != null && body.length > 0) {
            builder.withBody(new String(body, StandardCharsets.UTF_8));
        }
        return builder.build();
    }

    private static com.atlassian.oai.validator.model.Response toResponse(MockHttpServletResponse res) {
        SimpleResponse.Builder builder = SimpleResponse.Builder.status(res.getStatus());
        for (String name : res.getHeaderNames()) {
            builder.withHeader(name, res.getHeaders(name));
        }
        try {
            String body = res.getContentAsString(StandardCharsets.UTF_8);
            if (!body.isEmpty()) {
                builder.withBody(body);
            }
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        return builder.build();
    }
}
