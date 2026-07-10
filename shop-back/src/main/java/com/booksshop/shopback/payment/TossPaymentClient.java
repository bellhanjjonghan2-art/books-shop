package com.booksshop.shopback.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@EnableConfigurationProperties(TossProperties.class)
public class TossPaymentClient {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentClient.class);
    private static final int SECRET_KEY_PREFIX_LENGTH = 12;

    private final RestClient restClient;

    public TossPaymentClient(TossProperties tossProperties) {
        String secretKey = tossProperties.getSecretKey();
        // TossProperties 바인딩이 실제로 값을 받았는지(시작 시점) 확인용. 시크릿 키 전체는 절대 로그로 남기지 않는다.
        log.info("Toss 시크릿 키 로딩 확인: prefix={}, length={}, apiBaseUrl={}",
                maskSecretKey(secretKey), secretKey == null ? 0 : secretKey.length(), tossProperties.getApiBaseUrl());

        // 토스 시크릿 키 인코딩 규칙: "Basic " + base64(secretKey + ":")
        String encodedSecretKey = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(tossProperties.getApiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public TossConfirmResult confirm(String paymentKey, String orderId, Integer amount) {
        Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );
        return restClient.post()
                .uri("/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange((request, response) -> {
                    String rawBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    int statusCode = response.getStatusCode().value();
                    return new TossConfirmResult(response.getStatusCode().is2xxSuccessful(), statusCode, rawBody);
                });
    }

    // 재고 부족으로 인한 자동 환불 시 호출. 취소 응답 원문을 그대로 반환해 PaymentCancel 필드(transactionKey 등) 추출에 쓴다.
    public String cancel(String paymentKey, String cancelReason) {
        Map<String, Object> requestBody = Map.of("cancelReason", cancelReason);
        return restClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    private String maskSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return "(empty)";
        }
        return secretKey.substring(0, Math.min(SECRET_KEY_PREFIX_LENGTH, secretKey.length())) + "...";
    }
}
