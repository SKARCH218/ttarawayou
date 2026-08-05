/**
 * 공통 유틸.
 * API 주소는 접속한 호스트 기준으로 만들어 localhost든 IP 접속이든
 * 같은 호스트의 8080 포트(Spring)로 자동 연결한다.
 */
// 개발용 Express(3000/3030)에서는 8080의 Spring으로, 그 외(단일 jar 배포)에서는 같은 오리진으로
const API_BASE = ['3000', '3030'].includes(location.port)
  ? `${location.protocol}//${location.hostname}:8080`
  : '';

/* ============ 로그인 상태 ============
   토큰은 localStorage에, 회원 정보는 화면 표시용 캐시로 함께 둔다.
   서버 검증은 /api/auth/me 가 담당하므로 캐시는 신뢰 대상이 아니다. */
const AUTH_KEY = 'trevitAuth';

function saveAuth(auth) {
  localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
}

function loadAuth() {
  try {
    return JSON.parse(localStorage.getItem(AUTH_KEY) || 'null');
  } catch {
    return null;
  }
}

function clearAuth() {
  localStorage.removeItem(AUTH_KEY);
}

/** fetch 옵션에 Authorization 헤더를 얹어 준다 */
function authHeaders(extra = {}) {
  const auth = loadAuth();
  return auth?.token ? { ...extra, Authorization: `Bearer ${auth.token}` } : { ...extra };
}

/** 저장된 토큰이 아직 살아 있는지 서버에 확인. 죽었으면 지우고 null */
async function fetchMe() {
  const auth = loadAuth();
  if (!auth?.token) return null;
  try {
    const res = await fetch(`${API_BASE}/api/auth/me`, { headers: authHeaders() });
    if (res.status === 401) {
      clearAuth();
      return null;
    }
    if (!res.ok) return auth.user || null; // 서버 장애 시엔 캐시로 버틴다
    const user = await res.json();
    saveAuth({ token: auth.token, user });
    return user;
  } catch {
    return auth.user || null;
  }
}

async function logout() {
  try {
    await fetch(`${API_BASE}/api/auth/logout`, { method: 'POST', headers: authHeaders() });
  } catch {
    /* 서버에 못 닿아도 로컬 토큰은 지운다 */
  }
  clearAuth();
  location.href = 'login.html';
}

/**
 * <body data-auth="required"> 인 화면은 로그인해야 열린다.
 * #authChip 이 있으면 로그인한 닉네임과 로그아웃 버튼을 그려 준다.
 */
document.addEventListener('DOMContentLoaded', async () => {
  const needsAuth = document.body.dataset.auth === 'required';
  if (needsAuth && !loadAuth()) {
    location.replace('login.html');
    return;
  }

  const chip = document.getElementById('authChip');
  if (!chip) return;

  const user = await fetchMe();
  if (!user) {
    // 토큰이 만료됐거나 폐기된 경우 — 다시 로그인시킨다
    if (needsAuth) location.replace('login.html');
    return;
  }

  const name = document.createElement('span');
  name.className = 'auth-chip-name';
  name.textContent = `${user.nickname}님`; // 닉네임은 마크업이 아니라 텍스트로만 넣는다
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'auth-chip-btn';
  btn.textContent = '로그아웃';
  btn.addEventListener('click', logout);
  chip.replaceChildren(name, btn);
});

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

/** 여행 설정(지역·예산·기간·인원) — index.html → ask.html 로 전달 */
function saveSetup(setup) {
  sessionStorage.setItem('trevitSetup', JSON.stringify(setup));
}

function loadSetup() {
  const raw = sessionStorage.getItem('trevitSetup');
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
