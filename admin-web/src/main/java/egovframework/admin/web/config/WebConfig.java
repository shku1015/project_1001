package egovframework.admin.web.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import egovframework.admin.web.security.PermissionInterceptor;

/**
 * MVC 공통 설정: 권한 인터셉터, 캐시, JSON 일시 형식.
 */
@Configuration
@EnableCaching
public class WebConfig implements WebMvcConfigurer {

    /** API 일시 형식: 한국 시간, 초 단위, 시간대 표기 없음 (docs/06-api-spec.md 1절, openapi.yaml DateTime) */
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PermissionInterceptor permissionInterceptor;

    public WebConfig(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**", "/ssr/**");
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer dateTimeFormat() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATE_TIME))
                .deserializers(new LocalDateTimeDeserializer(DATE_TIME));
    }
}
