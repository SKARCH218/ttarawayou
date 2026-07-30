/**
 * 공통 유틸.
 * API 주소는 접속한 호스트 기준으로 만들어 localhost든 IP 접속이든
 * 같은 호스트의 8080 포트(Spring)로 자동 연결한다.
 */
// 개발용 Express(3000/3030)에서는 8080의 Spring으로, 그 외(단일 jar 배포)에서는 같은 오리진으로
const API_BASE = ['3000', '3030'].includes(location.port)
  ? `${location.protocol}//${location.hostname}:8080`
  : '';

function formatWon(n) {
  return `${Math.round(n).toLocaleString('ko-KR')}토큰`;
}

function savePlan(plan) {
  sessionStorage.setItem('trevitPlan', JSON.stringify(plan));
}

function loadPlan() {
  const raw = sessionStorage.getItem('trevitPlan');
  return raw ? JSON.parse(raw) : null;
}

/** 하버사인 거리(미터) */
function distanceMeters(lat1, lng1, lat2, lng2) {
  const R = 6371000;
  const toRad = (d) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const TYPE_INFO = {
  LODGING: { emoji: '🏨', label: '숙소' },
  RESTAURANT: { emoji: '🍜', label: '식당' },
  ATTRACTION: { emoji: '🏛️', label: '관광지' },
  START: { emoji: '📍', label: '출발지' },
};
