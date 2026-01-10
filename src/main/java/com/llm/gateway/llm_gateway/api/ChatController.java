package com.llm.gateway.llm_gateway.api;

import com.llm.gateway.llm_gateway.application.service.RouterService;
import com.llm.gateway.llm_gateway.application.service.TokenService;
import com.llm.gateway.llm_gateway.domain.model.ChatRequest;
import com.llm.gateway.llm_gateway.domain.port.LLMProvider;
import com.llm.gateway.llm_gateway.dto.ExecutionMetadata;
import com.llm.gateway.llm_gateway.dto.GatewayRequest;
import com.llm.gateway.llm_gateway.infrastructure.persistence.RequestLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RouterService routerService;
    private final TokenService tokenService;
    private RequestLogService requestLogService;

    public ChatController(RouterService routerService, TokenService tokenService, RequestLogService requestLogService) {
        this.routerService = routerService;
        this.tokenService = tokenService;
        this.requestLogService = requestLogService;
    }

    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody GatewayRequest requestDto,
                           @RequestHeader("Authorization") String authHeader) {

        int inputTokens = tokenService.estimateInputTokens(requestDto);
        long startTime = System.currentTimeMillis();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Thread.ofVirtual().start(() -> {
            StringBuilder responseBuffer = new StringBuilder();
            try {
                // 1. Route based on the generic string in the DTO
                ExecutionMetadata metadata = routerService.routeAndExecute(requestDto, chunk->{
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                        responseBuffer.append(chunk);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                String fullResponse = responseBuffer.toString();
                int outputTokens = tokenService.count(fullResponse);
                long duration = System.currentTimeMillis() - startTime;
                logUsage(metadata.modelUsed(), inputTokens, outputTokens, duration);
                requestLogService.log(requestDto, metadata.provider(), metadata.modelUsed(),
                        inputTokens, outputTokens, duration, true);

                emitter.complete();
            } catch (Exception e) {
                log.error("Stream failed", e);
                // Even on error, log what we consumed so far
                int partialOutput = tokenService.count(responseBuffer.toString());
                logUsage(requestDto.model(), inputTokens, partialOutput, 0);

                requestLogService.log(
                        requestDto,
                        "unknown",
                        "unknown",
                        inputTokens,
                        0, // Incomplete output
                        System.currentTimeMillis() - startTime,
                        false
                );
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void logUsage(String model, int promptTokens, int completionTokens, long ms) {
        // Later, we will write this to Postgres!
        log.info("USAGE: Model={} | Input={} | Output={} | Total={} | Time={}ms",
                model, promptTokens, completionTokens, promptTokens + completionTokens, ms);
    }
}
