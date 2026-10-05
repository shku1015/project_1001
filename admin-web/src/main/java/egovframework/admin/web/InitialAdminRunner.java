package egovframework.admin.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import egovframework.admin.auth.InitialAdminService;
import egovframework.admin.web.config.AuthProperties;

/**
 * 시작할 때 최초 관리자 admin이 없으면 만든다 (docs/03-initial-data.md 2절).
 * 비밀번호는 환경 변수 INITIAL_ADMIN_PASSWORD로 받으며, 없으면 만들지 않는다.
 */
@Component
public class InitialAdminRunner implements ApplicationRunner {

    private static final Logger log = LogManager.getLogger(InitialAdminRunner.class);

    private final InitialAdminService initialAdminService;
    private final AuthProperties props;

    public InitialAdminRunner(InitialAdminService initialAdminService, AuthProperties props) {
        this.initialAdminService = initialAdminService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = props.initialAdminPassword();
        if (password == null || password.isBlank()) {
            log.info("INITIAL_ADMIN_PASSWORD가 없어 최초 관리자(admin)를 확인하지 않습니다.");
            return;
        }
        if (initialAdminService.createIfAbsent(password)) {
            log.info("최초 관리자(admin)를 만들었습니다. 첫 로그인 때 비밀번호를 바꿔야 합니다.");
        }
    }
}
