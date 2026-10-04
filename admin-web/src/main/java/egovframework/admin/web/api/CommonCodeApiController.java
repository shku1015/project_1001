package egovframework.admin.web.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import egovframework.admin.code.CodeItem;
import egovframework.admin.code.CodeService;
import egovframework.admin.common.ApiResponse;

/**
 * 공통 코드 조회 API (docs/06-api-spec.md 9절, api/openapi.yaml).
 * TODO(M1 인증): 로그인한 관리자만 호출할 수 있게 보안 설정을 붙인다.
 */
@RestController
@RequestMapping("/api/v1/common/codes")
public class CommonCodeApiController {

    private final CodeService codeService;

    public CommonCodeApiController(CodeService codeService) {
        this.codeService = codeService;
    }

    @GetMapping("/{groupCd}")
    public ApiResponse<List<CodeItem>> getCodes(@PathVariable String groupCd,
                                                @RequestParam(defaultValue = "N") String includeUnused) {
        return ApiResponse.ok(codeService.getCodes(groupCd, "Y".equals(includeUnused)));
    }

    @GetMapping
    public ApiResponse<Map<String, List<CodeItem>>> getCodesByGroups(@RequestParam List<String> groups) {
        return ApiResponse.ok(codeService.getCodes(groups));
    }
}
