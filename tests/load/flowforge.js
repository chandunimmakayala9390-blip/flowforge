import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '30s', target: 25 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function () {
  const response = http.get(baseUrl + '/proxy/backend-test');

  check(response, {
    'proxy returns HTTP 200': (result) => result.status === 200,
    'response body is present': (result) => result.body !== null && result.body.length > 0,
  });
}
