import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 테스트 설정: 10명의 가상 사용자가 30초 동안 반복
  vus: 10,
  duration: '10s',

  // 성공 기준(Thresholds) 추가 예시
//  thresholds: {
//    http_req_duration: ['p(95)<500'], // 95%의 요청이 0.5초 이내 완료될 것
//    http_req_failed: ['rate<0.01'],   // 에러율 1% 미만 유지
//  },
};

export default function () {
  const url = 'http://localhost:8080/api/v1/orders';

  const payload = JSON.stringify({
    memberId: 1,      // @NotNull
    productId: 1,   // @NotNull
    quantity: 1       // @NotNull, @Min(1)
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // POST 요청 전송
  const res = http.post(url, payload, params);

  // 결과 검증
  check(res, {
    'is status 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response body has id': (r) => r.body.includes('id'), // 응답에 id가 포함되는지 예시
  });

  // 1초 대기 후 다음 반복
  sleep(1);
}
