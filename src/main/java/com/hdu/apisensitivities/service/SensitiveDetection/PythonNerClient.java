package com.hdu.apisensitivities.service.SensitiveDetection;
// 新建类 com.hdu.apisensitivities.service.SensitiveDetection.PythonNerClient
import com.hdu.apisensitivities.entity.SensitiveEntity;
import com.hdu.apisensitivities.entity.SensitiveType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PythonNerClient {

    private final WebClient webClient;
    private final int timeout;

    public PythonNerClient(@Value("${ner.python.url:http://localhost:8000}") String baseUrl,
            @Value("${ner.python.timeout:2000}") int timeout) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    // 请求 DTO
    public static class NerRequest {
        public String text;

        public NerRequest(String text) {
            this.text = text;
        }
    }

    public static class NerResponse {
        public List<EntityDto> entities;
    }

    public static class EntityDto {
        public String text;
        public String label;
        public int start;
        public int end;
        public double confidence;
    }

    public List<SensitiveEntity> detect(String text) {
        try {
            NerResponse response = webClient.post()
                    .uri("/ner")
                    .bodyValue(new NerRequest(text))
                    .retrieve()
                    .bodyToMono(NerResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .onErrorResume(e -> {
                        log.warn("Python NER 服务调用失败，返回空列表", e);
                        return Mono.just(new NerResponse()); // fallback 空列表
                    })
                    .block();

            if (response == null || response.entities == null) {
                return Collections.emptyList();
            }

            // 映射到你的 SensitiveEntity
            return response.entities.stream()
                    .filter(e -> e.label != null && e.text != null)
                    .map(e -> SensitiveEntity.builder()
                            .type(SensitiveType.valueOf(e.label)) // 注意 label 要与 SensitiveType 枚举一致
                            .originalText(e.text)
                            .start(e.start)
                            .end(e.end)
                            .confidence(e.confidence)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("调用 Python NER 异常", e);
            return Collections.emptyList();
        }
    }
}
