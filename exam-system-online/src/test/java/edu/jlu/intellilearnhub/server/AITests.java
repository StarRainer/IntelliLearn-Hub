package edu.jlu.intellilearnhub.server;

import edu.jlu.intellilearnhub.server.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class AITests {
    @Autowired
    private AIService aiService;

    @Test
    void test() {
        String result = aiService.chat("你是谁？");
        log.debug(result);
    }
}
