package egovframework.admin.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import egovframework.admin.common.ErrorCode;
import egovframework.admin.web.support.OpenApiContract;

/**
 * Java의 ErrorCode와 api/openapi.yaml의 ErrorCode enum이 같은지 확인한다.
 * 한쪽만 고치면 이 테스트가 실패한다.
 */
class ErrorCodeSpecTest {

    @Test
    @SuppressWarnings("unchecked")
    void 오류_코드_목록이_명세와_같다() throws IOException {
        Map<String, Object> spec;
        try (Reader reader = Files.newBufferedReader(OpenApiContract.SPEC)) {
            spec = new Yaml().load(reader);
        }
        Map<String, Object> schemas = (Map<String, Object>) ((Map<String, Object>) spec.get("components")).get("schemas");
        List<String> specCodes = (List<String>) ((Map<String, Object>) schemas.get("ErrorCode")).get("enum");

        List<String> javaCodes = Arrays.stream(ErrorCode.values()).map(Enum::name).toList();

        assertThat(specCodes).containsExactlyInAnyOrderElementsOf(javaCodes);
    }
}
