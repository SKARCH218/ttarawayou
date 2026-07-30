/** 미스터리 플랜 화면 — 장소 이름은 절대 노출하지 않는다 */
(() => {
  const plan = loadPlan();
  if (!plan) {
    location.replace('index.html');
    return;
  }

  document.getElementById('plannerBadge').textContent =
    plan.plannedBy === 'AI' ? 'AI가 설계한 플랜' : '알고리즘이 설계한 플랜';

  // AI 추천 이유 (v2 백엔드)
  if (plan.aiReason) {
    const reason = document.createElement('div');
    reason.className = 'card';
    reason.style.cssText = 'font-size:13px; line-height:1.6; color:var(--ogq-color-mono-800);';
    reason.innerHTML = `<b>AI의 한마디</b><br />${plan.aiReason}`;
    document.querySelector('.card').before(reason);
  }

  document.getElementById('budgetTotal').textContent = formatWon(plan.budget);
  document.getElementById('totalCost').textContent = formatWon(plan.totalCost);
  document.getElementById('remaining').textContent = formatWon(plan.remainingBudget);
  document.getElementById('tokenBalance').textContent =
    plan.tokenBalance != null ? Number(plan.tokenBalance).toLocaleString('ko-KR') + ' 토큰' : '—';

  // 예산 배분 사용률 바
  const b = plan.breakdown;
  const bars = [
    // OGQ 팔레트: 숙박=secondary(퍼플), 관광=primary(틸), 식비=warning(오렌지), 교통=accent-blue
    { label: '숙박', spent: b.lodgingSpent, budget: b.lodgingBudget, color: '#703fe4' },
    { label: '관광', spent: b.attractionSpent, budget: b.attractionBudget, color: '#00c389' },
    { label: '식비', spent: b.foodSpent, budget: b.foodBudget, color: '#ff9800' },
    { label: '교통', spent: b.transportSpent, budget: b.transportBudget, color: '#1490eb' },
  ];
  const barGroup = document.getElementById('barGroup');
  bars.forEach(({ label, spent, budget, color }) => {
    const pct = budget > 0 ? Math.min(100, Math.round((spent / budget) * 100)) : 0;
    const el = document.createElement('div');
    el.className = 'bar-item';
    el.innerHTML = `
      <div class="bar-label"><span>${label}</span><span>${formatWon(spent)} / ${formatWon(budget)}</span></div>
      <div class="bar-track"><div class="bar-fill" style="width:0; background:${color};"></div></div>`;
    barGroup.appendChild(el);
    requestAnimationFrame(() =>
      requestAnimationFrame(() => (el.querySelector('.bar-fill').style.width = pct + '%'))
    );
  });

  // Day 버튼 — 장소 이름 대신 개수·소요 정보만 노출.
  // 순차 진행: 이전 일차를 완료해야 다음 일차가 열린다
  const doneDay = Number(sessionStorage.getItem('trevitDoneDay') || 0);
  const dayList = document.getElementById('dayList');
  plan.dayPlans.forEach((day) => {
    const mysterySpots = day.stops.filter(
      (s, i) => i > 0 && !(i === day.stops.length - 1 && s.type === 'LODGING')
    ).length;
    const totalMinutes = day.legs.reduce((sum, l) => sum + l.durationMinutes, 0);
    const startAt = day.legs[0]?.departAt || '09:00';
    const completed = day.day <= doneDay;
    const locked = day.day > doneDay + 1;

    const btn = document.createElement('button');
    btn.className = 'day-btn' + (locked ? ' locked' : '');
    let infoLine;
    if (completed) {
      infoLine = `완료한 여정 · 다시 보기`;
    } else if (locked) {
      infoLine = `Day ${day.day - 1} 완료 후 열려요 · ${startAt} 시작 예정`;
    } else {
      infoLine = `${startAt} 시작 · 비밀 장소 ${mysterySpots}곳 · 이동 약 ${totalMinutes}분 · ${formatWon(day.dayCost)}`;
    }
    btn.innerHTML = `
      <span class="day-num">${locked ? '잠김' : completed ? '완료' : 'D' + day.day}</span>
      <span class="day-info">
        <b>Day ${day.day} 여정 ${completed ? '(완료)' : '따라가기'}</b>
        <span>${infoLine}</span>
      </span>
      <span class="arrow"><img class="ds-icon" src="icons/Chevron_Right.png" alt="›" /></span>`;
    if (!locked) {
      btn.addEventListener('click', () => {
        const demo = document.getElementById('demoCheck').checked ? '&demo=1' : '';
        location.href = `map.html?day=${day.day}${demo}`;
      });
    }
    dayList.appendChild(btn);
  });

  // 데모 모드 기본값: 모바일(실사용)은 OFF, 데스크톱(시연·심사)은 ON. 선택은 세션에 기억
  const demoCheck = document.getElementById('demoCheck');
  const savedDemo = sessionStorage.getItem('trevitDemoMode');
  demoCheck.checked = savedDemo != null
    ? savedDemo === '1'
    : !/Android|iPhone|iPad|Mobile/i.test(navigator.userAgent);
  demoCheck.addEventListener('change', () =>
    sessionStorage.setItem('trevitDemoMode', demoCheck.checked ? '1' : '0'));

  document.getElementById('resetBtn').addEventListener('click', () => {
    sessionStorage.removeItem('trevitPlan');
    location.href = 'index.html';
  });
})();
