package com.safetypin.post;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloControllerTest {

    private final HelloController helloController = new HelloController();

    @Test
    void testSayHelloReturnsHelloWorld() {
        String greeting = helloController.sayHello();
        assertEquals("Hello, World!", greeting);
    }
}
