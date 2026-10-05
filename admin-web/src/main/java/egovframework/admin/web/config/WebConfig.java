package egovframework.admin.web.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import egovframework.admin.web.security.PermissionInterceptor;

/**
 * MVC 공통 설정: 권한 인터셉터, 캐시, JSON 일시 형식, ① React 정적 파일.
 */
@Configuration
@EnableCaching
public class WebConfig implements WebMvcConfigurer {

    /** API 일시 형식: 한국 시간, 초 단위, 시간대 표기 없음 (docs/06-api-spec.md 1절, openapi.yaml DateTime) */
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PermissionInterceptor permissionInterceptor;
    private final String reactLocation;

    public WebConfig(PermissionInterceptor permissionInterceptor, @Value("${app.react.location}") String reactLocation) {
        this.permissionInterceptor = permissionInterceptor;
        this.reactLocation = reactLocation.endsWith("/") ? reactLocation : reactLocation + "/";
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**", "/ssr/**");
    }

    /**
     * ① React: 빌드 결과를 /react/** 로 제공한다. 파일이 없는 경로(/react/me 등 화면 주소)는 index.html을 준다
     * (클라이언트 라우팅, docs/08-architecture.md 1.1).
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/react/**")
                .addResourceLocations(reactLocation)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return resourcePath.contains(".") ? null : location.createRelative("index.html");
                    }
                });
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/react", "/react/");
        // 경로가 빈 /react/ 는 정적 파일 처리기가 파일을 찾지 않으므로 index.html로 직접 넘긴다
        registry.addViewController("/react/").setViewName("forward:/react/index.html");
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer dateTimeFormat() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATE_TIME))
                .deserializers(new LocalDateTimeDeserializer(DATE_TIME));
    }
}
