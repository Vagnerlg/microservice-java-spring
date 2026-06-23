package com.github.vagnerlg.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MockConfiguration.class})
class MainApplicationIT {

    @Test
    void contextLoads() {
    }
}
