package egovframework.admin.common.masking;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import egovframework.admin.common.masking.Masking.Field;

/**
 * 마스킹 설정 적용 (docs/04-features/10-masking.md 5절). 설정은 요청마다 읽어 바꾸면 바로 반영된다 (BR-07).
 * - 화면: 화면 마스킹이 Y면 가린다 (BR-01, 02). 원문 보기는 PRIVACY 권한으로 따로 연다.
 * - 엑셀: 엑셀 마스킹이 Y면 누구에게나 가리고, N이면 PRIVACY 권한이 있을 때만 원문 (BR-03, 04).
 */
@Service
public class MaskingService {

    public record PolicyRow(String fieldCd, String screenMaskYn, String excelMaskYn) {
    }

    @Mapper
    public interface MaskingPolicyMapper {

        @Select("SELECT field_cd, screen_mask_yn, excel_mask_yn FROM tb_masking_policy")
        List<PolicyRow> selectAll();
    }

    /** 한 요청 안에서 쓰는 마스킹 판단 (설정을 한 번만 읽는다) */
    public record Masker(Map<String, PolicyRow> policies, boolean excel, boolean privacy) {

        public String mask(Field field, String value) {
            PolicyRow p = policies.get(field.name());
            boolean on = p == null || (excel ? "Y".equals(p.excelMaskYn()) || !privacy : "Y".equals(p.screenMaskYn()));
            return on ? Masking.apply(field, value) : value;
        }
    }

    private final MaskingPolicyMapper mapper;

    public MaskingService(MaskingPolicyMapper mapper) {
        this.mapper = mapper;
    }

    /** 화면(목록·상세)용 */
    public Masker forScreen() {
        return new Masker(load(), false, false);
    }

    /** 엑셀용. privacy: 내려받는 관리자가 그 메뉴의 PRIVACY 권한을 가졌는지 */
    public Masker forExcel(boolean privacy) {
        return new Masker(load(), true, privacy);
    }

    private Map<String, PolicyRow> load() {
        return mapper.selectAll().stream().collect(Collectors.toMap(PolicyRow::fieldCd, p -> p));
    }
}
