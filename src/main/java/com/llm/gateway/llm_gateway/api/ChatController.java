package com.llm.gateway.llm_gateway.api;

import com.llm.gateway.llm_gateway.application.service.RouterService;
import com.llm.gateway.llm_gateway.domain.model.ChatRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final RouterService routerService;

    public ChatController(RouterService routerService) {
        this.routerService = routerService;
    }

    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody GatewayRequest requestDto,
                           @RequestHeader("Authorization") String authHeader) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Thread.ofVirtual().start(() -> {
            try {
                // 1. Route based on the generic string in the DTO
                routerService.routeAndExecute(requestDto, chunk->{
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                // 2. Pass the DTO directly!
                // The Controller doesn't care if it's OpenAI or Anthropic underneath.
//                provider.streamChat(requestDto, chunk -> {
//                    try {
//                        emitter.send(SseEmitter.event().data(chunk));
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
