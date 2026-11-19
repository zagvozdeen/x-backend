package ru.urfu.xbackend.messages;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.urfu.xbackend.api.ApiErrorController;
import ru.urfu.xbackend.api.ApiErrors;
import ru.urfu.xbackend.api.ApiSecurity;

@SpringBootApplication
@Import({ApiErrors.class, ApiErrorController.class, ApiSecurity.class})
public class MessagesApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagesApplication.class, args);
    }
}
