package es.vargontoc.storyteller.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class ChatController {
    
    @GetMapping("/health")
    public String getHealth(@RequestParam String param) {
        return "ok";
    }
    
}
