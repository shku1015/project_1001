package egovframework.admin.code;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.admin.common.BusinessException;
import egovframework.admin.common.ErrorCode;

/**
 * 공통 코드 조회 (docs/06-api-spec.md 9절). 코드 콤보와 코드명 표시에 쓴다.
 */
@Service
@Transactional(readOnly = true)
public class CodeService {

    private final CodeMapper codeMapper;

    public CodeService(CodeMapper codeMapper) {
        this.codeMapper = codeMapper;
    }

    public List<CodeItem> getCodes(String groupCd, boolean includeUnused) {
        if (!codeMapper.existsGroup(groupCd)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "코드 그룹이 없습니다: " + groupCd);
        }
        return codeMapper.selectCodes(groupCd, includeUnused);
    }

    public Map<String, List<CodeItem>> getCodes(List<String> groupCds) {
        Map<String, List<CodeItem>> result = new LinkedHashMap<>();
        for (String groupCd : groupCds) {
            result.put(groupCd, getCodes(groupCd, false));
        }
        return result;
    }
}
