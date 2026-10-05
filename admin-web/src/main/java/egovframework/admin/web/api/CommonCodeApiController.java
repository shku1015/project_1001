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
import egovframework.admin.web.security.LoginOnly;

/**
 * 공통 코드 조회 API (docs/06-api-spec.md 9절, api/openapi.yaml). 로그인만 필요하다.
 */
@RestController
@RequestMapping("/api/v1/common/codes")
public class CommonCodeApiController {

    private final CodeService codeService;

    public CommonCodeApiController(CodeService codeService) {
        this.codeService = codeService;
    }

    @GetMapping("/{groupCd}")
    @LoginOnly
    public ApiResponse<List<CodeItem>> getCodes(@PathVariable String groupCd,
                                                @RequestParam(defaultValue = "N") String includeUnused) {
        return ApiResponse.ok(codeService.getCodes(groupCd, "Y".equals(includeUnused)));
    }

    @GetMapping
    @LoginOnly
    public ApiResponse<Map<String, List<CodeItem>>> getCodesByGroups(@RequestParam List<String> groups) {
        return ApiResponse.ok(codeService.getCodes(groups));
    }
}
