package com.booksshop.shopback.payment;

// 토스 승인 API 응답을 성공 여부·HTTP 상태코드·원본 바디(JSON 문자열)로 감싼다.
// 실패(4xx/5xx) 응답도 raw_response·fail_code/fail_message 기록을 위해 본문이 필요해 예외 대신 이 타입으로 반환한다.
// statusCode는 실패 원인 추정(401=키 문제, 400=요청값 문제, 5xx=토스 장애 등)에 로그로 쓴다.
public record TossConfirmResult(boolean success, int statusCode, String rawBody) {
}
