package egovframework.admin.web.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import egovframework.admin.auth.AdminAuthInfoService;
import egovframework.admin.web.security.AdminPrincipal;
import egovframework.admin.web.security.ApiErrorWriter;
import egovframework.admin.web.security.JwtAuthenticationFilter;
import egovframework.admin.web.security.JwtProvider;
import egovframework.admin.web.security.SessionAuthenticationProvider;

/**
 * 경로별 보안 설정 (docs/08-architecture.md 1절, ADR-0005).
 * 1. /api/**  : JWT(Bearer), 세션 없음, CSRF 없음 (①②)
 * 2. /ssr/**  : 세션 + CSRF + 로그인 폼 (③)
 * 3. 그 밖    : 공개 (안내 페이지, 헬스 체크, React·JSP 정적 화면, 본문 이미지)
 * 메서드 단위 권한은 PermissionInterceptor가 확인한다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http, JwtProvider jwtProvider, AdminAuthInfoService authInfoService,
                                 ApiErrorWriter errorWriter) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/token", "/api/v1/auth/token/refresh").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, authInfoService),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(errorWriter).accessDeniedHandler(errorWriter));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain ssrChain(HttpSecurity http, SessionAuthenticationProvider provider,
                                 AdminAuthInfoService authInfoService) throws Exception {
        http.securityMatcher("/ssr/**")
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ssr/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/ssr/login")
                        .loginProcessingUrl("/ssr/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
                            boolean pwdTemp = authInfoService.load(principal.adminId()).pwdTemp();
                            response.sendRedirect(request.getContextPath() + (pwdTemp ? "/ssr/password" : "/ssr/"));
                        })
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler("/ssr/login?error")))
                .logout(logout -> logout
                        .logoutUrl("/ssr/logout")
                        .logoutSuccessUrl("/ssr/login?logout"));
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
