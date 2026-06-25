package com.github.vagnerlg.inventory;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class MockConfiguration {

    // Declare beans mockados aqui com @Bean + Mockito.mock() quando necessário.
    // Exemplo:
    // @Bean
    // SomeService someService() {
    //     return Mockito.mock(SomeService.class);
    // }

}
