/**
 * 여행 설정 화면 — 지역·예산·기간·인원만 받는다.
 * 취향(목적·성별·연령·MBTI·음식·장소·걷기·메모)은 ask.html에서 한 질문씩 묻는다.
 */
(() => {
  const MIN_BUDGET = 50000;
  const STEP = 10000;

  const state = loadSetup() || { days: 2, people: 1, budget: 300000, region: null };
  state.balance = 0;
  state.maxBudget = 300000;

  const $ = (id) => document.getElementById(id);
  const slider = $('budgetSlider');
  const fmt = (n) => Number(n).toLocaleString('ko-KR');

  // ---------- 인트로: 1.6초 후 사라짐, 탭하면 즉시 스킵 ----------
  // ?intro=hold 로 열면 자동으로 사라지지 않는다 (피치 영상·스크린샷 촬영용)
  const intro = $('intro');
  const hideIntro = () => intro.classList.add('hide');
  if (new URLSearchParams(location.search).get('intro') !== 'hold') {
    setTimeout(hideIntro, 1600);
  }
  intro.addEventListener('click', hideIntro);

  // ---------- 예산 ----------
  function renderBudget() {
    $('budgetValue').textContent = fmt(state.budget);
    slider.value = state.budget;
    const min = Number(slider.min), max = Number(slider.max);
    const pct = max > min ? ((state.budget - min) / (max - min)) * 100 : 100;
    slider.style.setProperty('--fill', Math.max(0, Math.min(100, pct)) + '%');
  }

  const clampBudget = (v) => Math.max(MIN_BUDGET, Math.min(state.maxBudget, v));

  /** 지갑 조회 → 슬라이더 최대치 = 보유 토큰 */
  async function loadWallet() {
    try {
      const res = await fetch(`${API_BASE}/api/wallet`);
      state.balance = (await res.json()).balance;
    } catch {
      state.balance = 0;
      showError('서버에 연결할 수 없어요 — 잠시 후 다시 시도해 주세요.');
    }
    const rawMax = Math.max(MIN_BUDGET, state.balance);
    state.maxBudget = MIN_BUDGET + Math.floor((rawMax - MIN_BUDGET) / STEP) * STEP;
    slider.max = state.maxBudget;
    $('walletBalance').textContent = fmt(state.balance) + ' 토큰';
    state.budget = clampBudget(state.budget);
    renderBudget();
  }

  slider.addEventListener('input', () => {
    state.budget = Number(slider.value);
    renderBudget();
  });

  // 숫자를 눌러 직접 입력
  $('budgetValue').addEventListener('click', () => {
    const numEl = $('budgetValue');
    const input = document.createElement('input');
    input.type = 'text';
    input.inputMode = 'numeric';
    input.className = 'budget-edit';
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

  // 테스트용 토큰 충전
  $('chargeBtn').addEventListener('click', async () => {
    try {
      await fetch(`${API_BASE}/api/wallet/reset`, { method: 'POST' });
      await loadWallet();
    } catch {
      showError('충전에 실패했어요 — 서버 연결을 확인해 주세요.');
    }
  });

  // ---------- 기간·인원 스테퍼 ----------
  document.querySelectorAll('[data-step]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const key = btn.dataset.step;
      const limits = key === 'days' ? [1, 3] : [1, 4];
      state[key] = Math.min(limits[1], Math.max(limits[0], state[key] + Number(btn.dataset.delta)));
      $('daysValue').textContent = state.days;
      $('peopleValue').textContent = state.people;
    });
  });

  function showError(msg) {
    $('errorBox').textContent = msg;
    $('errorBox').classList.add('show');
  }

  // ---------- 다음: 취향 질문으로 ----------
  $('nextBtn').addEventListener('click', () => {
    $('errorBox').classList.remove('show');
    if (state.budget > state.balance) {
      showError(`보유 토큰(${fmt(state.balance)})이 부족해요. 충전 버튼을 눌러 주세요.`);
      return;
    }
    saveSetup({
      region: $('regionSelect').value,
      budget: state.budget,
      days: state.days,
      people: state.people,
    });
    location.href = 'ask.html';
  });

  // 이전에 고른 값 복원
  if (state.region) $('regionSelect').value = state.region;
  $('daysValue').textContent = state.days;
  $('peopleValue').textContent = state.people;
  loadWallet();
})();
