/** 예산 입력 화면 — 토큰 기반 슬라이더 (1토큰 = 1원) */
(() => {
  const MIN_BUDGET = 50000;
  const STEP = 10000;

  const state = { days: 2, people: 2, budget: 300000, balance: 0, maxBudget: 300000 };

  const $ = (id) => document.getElementById(id);
  const slider = $('budgetSlider');
  const daysValue = $('daysValue');
  const peopleValue = $('peopleValue');
  const createBtn = $('createBtn');
  const loading = $('loading');
  const loadingMsg = $('loadingMsg');
  const errorBox = $('errorBox');

  const fmt = (n) => Number(n).toLocaleString('ko-KR');

  function renderBudget() {
    const el = $('budgetValue');
    if (el) el.textContent = fmt(state.budget);
    slider.value = state.budget;
    // 트랙 채움 비율 갱신 (최대값이면 정확히 100%)
    const min = Number(slider.min), max = Number(slider.max);
    const pct = max > min ? ((state.budget - min) / (max - min)) * 100 : 100;
    slider.style.setProperty('--fill', Math.max(0, Math.min(100, pct)) + '%');
  }

  function clampBudget(v) {
    return Math.max(MIN_BUDGET, Math.min(state.maxBudget, v));
  }

  /** 지갑 조회 → 슬라이더 최대치 = 보유 토큰 (1만 단위 내림 정렬해 썸이 끝까지 닿게) */
  async function loadWallet() {
    try {
      const res = await fetch(`${API_BASE}/api/wallet`);
      state.balance = (await res.json()).balance;
    } catch {
      state.balance = 0;
      showError('지갑 조회 실패 — Spring 서버(8080)가 켜져 있는지 확인해 주세요.');
    }
    const rawMax = Math.max(MIN_BUDGET, state.balance);
    // step(1만) 격자에 맞춰 내림: max가 어중간하면 슬라이더가 끝까지 못 가 빈 칸이 생긴다
    state.maxBudget = MIN_BUDGET + Math.floor((rawMax - MIN_BUDGET) / STEP) * STEP;
    slider.max = state.maxBudget;
    $('walletBalance').textContent = fmt(state.balance) + ' 토큰';
    $('maxLabel').textContent = fmt(state.maxBudget);
    state.budget = clampBudget(state.budget);
    renderBudget();
  }

  // 슬라이더 드래그
  slider.addEventListener('input', () => {
    state.budget = Number(slider.value);
    renderBudget();
  });

  // 숫자 클릭 → 직접 입력
  $('budgetValue').addEventListener('click', () => {
    const numEl = $('budgetValue');
    const input = document.createElement('input');
    input.type = 'text';
    input.inputMode = 'numeric';
    input.className = 'budget-edit';
    input.id = 'budgetValueEdit';
    input.value = state.budget;
    numEl.replaceWith(input);
    input.focus();
    input.select();

    input.addEventListener('input', () => {
      const digits = input.value.replace(/[^\d]/g, '');
      input.value = digits ? fmt(Number(digits)) : '';
    });
    let committed = false;
    const commit = () => {
      if (committed) return;
      committed = true;
      const v = Number(String(input.value).replace(/[^\d]/g, ''));
      if (v) state.budget = clampBudget(Math.round(v / STEP) * STEP || MIN_BUDGET);
      input.replaceWith(numEl);
      renderBudget();
    };
    input.addEventListener('blur', commit);
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') commit(); });
  });

  // 테스트용 토큰 충전 (500,000으로 초기화)
  $('chargeBtn').addEventListener('click', async () => {
    try {
      await fetch(`${API_BASE}/api/wallet/reset`, { method: 'POST' });
      await loadWallet();
    } catch {
      showError('충전 실패 — 서버 연결을 확인해 주세요.');
    }
  });

  // 일수/인원 스테퍼
  document.querySelectorAll('[data-step]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const key = btn.dataset.step;
      const delta = Number(btn.dataset.delta);
      const limits = key === 'days' ? [1, 5] : [1, 10];
      state[key] = Math.min(limits[1], Math.max(limits[0], state[key] + delta));
      daysValue.textContent = state.days;
      peopleValue.textContent = state.people;
    });
  });

  function showError(msg) {
    errorBox.textContent = msg;
    errorBox.classList.add('show');
  }

  /** 브라우저 geolocation — 거부/실패 시 null (숙소 출발 폴백) */
  function getLocation() {
    return new Promise((resolve) => {
      if (!navigator.geolocation) return resolve(null);
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => resolve(null),
        { enableHighAccuracy: true, timeout: 7000, maximumAge: 60000 }
      );
    });
  }

  const LOADING_MESSAGES = [
    'AI가 예산을 숙박·관광·식비·교통에 배분하고 있어요',
    'AI가 평점 높은 숙소를 고르는 중이에요 (AI 응답에 2~3분 걸릴 수 있어요)',
    'AI가 예산을 알뜰하게 다 쓰는 조합을 찾고 있어요',
    '버스 노선과 도보 경로를 살피는 중…',
    '마지막으로 경로를 꽁꽁 숨기는 중 🤫',
  ];

  createBtn.addEventListener('click', async () => {
    errorBox.classList.remove('show');
    const budget = state.budget;
    if (!budget || budget < MIN_BUDGET) {
      showError(`총 예산을 ${fmt(MIN_BUDGET)}토큰 이상으로 설정해 주세요.`);
      return;
    }
    if (budget > state.balance) {
      showError(`보유 토큰(${fmt(state.balance)})이 부족합니다. 충전 버튼을 눌러 주세요.`);
      return;
    }

    loading.classList.add('show');
    let msgIdx = 0;
    const ticker = setInterval(() => {
      msgIdx = (msgIdx + 1) % LOADING_MESSAGES.length;
      loadingMsg.textContent = LOADING_MESSAGES[msgIdx];
    }, 2600);

    try {
      const loc = await getLocation();
      const res = await fetch(`${API_BASE}/api/plan`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          budget,
          days: state.days,
          people: state.people,
          startLatitude: loc ? loc.lat : null,
          startLongitude: loc ? loc.lng : null,
        }),
      });
      if (!res.ok) {
        let msg = `서버 오류 (${res.status})`;
        try { msg = (await res.json()).message || msg; } catch { /* 그대로 */ }
        throw new Error(msg);
      }
      const plan = await res.json();
      savePlan(plan);
      sessionStorage.removeItem('mysteryDoneDay'); // 새 플랜 → 일차 진행 초기화
      location.href = 'plan.html';
    } catch (e) {
      showError(`플랜 생성 실패: ${e.message}`);
      loadWallet(); // 잔액이 바뀌었을 수 있으니 갱신
    } finally {
      clearInterval(ticker);
      loading.classList.remove('show');
    }
  });

  loadWallet();
})();
