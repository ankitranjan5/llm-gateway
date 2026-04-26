package com.llm.gateway.llm_gateway.api;

import com.llm.gateway.llm_gateway.service.RouterService;
import com.llm.gateway.llm_gateway.service.TokenService;
import com.llm.gateway.llm_gateway.domain.model.ExecutionMetadata;
import com.llm.gateway.llm_gateway.domain.model.GatewayRequest;
import com.llm.gateway.llm_gateway.infrastructure.persistence.RequestLogService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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
    @RateLimiter(name="gateway-api", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<SseEmitter> chat(@RequestBody GatewayRequest requestDto,
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
                requestLogService.log(requestDto, metadata.provider(), metadata.modelUsed(), metadata.modelRequested(),
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
                        "unknown",
                        inputTokens,
                        0, // Incomplete output
                        System.currentTimeMillis() - startTime,
                        false
                );
                emitter.completeWithError(e);
            }
        });

        return ResponseEntity.status(200).body(emitter);
    }

    private void logUsage(String model, int promptTokens, int completionTokens, long ms) {
        // Later, we will write this to Postgres!
        log.info("USAGE: Model={} | Input={} | Output={} | Total={} | Time={}ms",
                model, promptTokens, completionTokens, promptTokens + completionTokens, ms);
    }

    public ResponseEntity<SseEmitter> rateLimitFallback(GatewayRequest request, String authHeader, Throwable t) {
        return ResponseEntity.status(429).body(null);
    }
}
