package egovframework.admin.web.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import egovframework.admin.auth.AuthTypes.ClientInfo;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청에서 접속 IP와 브라우저 정보를 꺼낸다.
 * 프록시 뒤에 둘 때의 X-Forwarded-For 처리는 배포 구성을 정할 때 추가한다.
 */
public final class ClientInfos {

    private ClientInfos() {
    }

    public static ClientInfo of(HttpServletRequest request) {
        return new ClientInfo(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public static ClientInfo current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return of(attrs.getRequest());
        }
        return new ClientInfo("unknown", null);
    }
}
