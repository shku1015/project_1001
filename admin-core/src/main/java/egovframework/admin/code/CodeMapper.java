package egovframework.admin.code;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CodeMapper {

    boolean existsGroup(@Param("groupCd") String groupCd);

    List<CodeItem> selectCodes(@Param("groupCd") String groupCd, @Param("includeUnused") boolean includeUnused);
}
