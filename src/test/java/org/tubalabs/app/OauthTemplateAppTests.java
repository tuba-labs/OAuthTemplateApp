package org.tubalabs.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OauthTemplateAppTests extends AbstractJdbcTestBaseTestClass {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

}
