/** 미스터리 플랜 화면 — 장소 이름은 절대 노출하지 않는다 */
(() => {
  const plan = loadPlan();
  if (!plan) {
    location.replace('index.html');
    return;
  }

  document.getElementById('plannerBadge').textContent =
    plan.plannedBy === 'AI' ? '🤖 로컬 AI(LM Studio)가 설계한 플랜'
                            : '⚙️ 알고리즘이 설계한 플랜';

  document.getElementById('budgetTotal').textContent = formatWon(plan.budget);
  document.getElementById('totalCost').textContent = formatWon(plan.totalCost);
  document.getElementById('remaining').textContent = formatWon(plan.remainingBudget);
  document.getElementById('tokenBalance').textContent =
    plan.tokenBalance != null ? Number(plan.tokenBalance).toLocaleString('ko-KR') + ' 토큰' : '—';

  // 예산 배분 사용률 바
  const b = plan.breakdown;
  const bars = [
    { label: '숙박', spent: b.lodgingSpent, budget: b.lodgingBudget, color: '#7f6bff' },
    { label: '관광', spent: b.attractionSpent, budget: b.attractionBudget, color: '#4ecdc4' },
    { label: '식비', spent: b.foodSpent, budget: b.foodBudget, color: '#ffb86b' },
    { label: '교통', spent: b.transportSpent, budget: b.transportBudget, color: '#ff7ab8' },
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

  // Day 버튼 — 장소 이름 대신 개수·소요 정보만 노출
  const dayList = document.getElementById('dayList');
  plan.dayPlans.forEach((day) => {
    const mysterySpots = day.stops.filter(
      (s, i) => i > 0 && !(i === day.stops.length - 1 && s.type === 'LODGING')
    ).length;
    const totalMinutes = day.legs.reduce((sum, l) => sum + l.durationMinutes, 0);
    const btn = document.createElement('button');
    btn.className = 'day-btn';
    btn.innerHTML = `
      <span class="day-num">D${day.day}</span>
      <span class="day-info">
        <b>Day ${day.day} 여정 따라가기</b>
        <span>비밀 장소 ${mysterySpots}곳 · 이동 약 ${totalMinutes}분 · ${formatWon(day.dayCost)}</span>
      </span>
      <span class="arrow">›</span>`;
    btn.addEventListener('click', () => (location.href = `map.html?day=${day.day}`));
    dayList.appendChild(btn);
  });

  document.getElementById('resetBtn').addEventListener('click', () => {
    sessionStorage.removeItem('mysteryPlan');
    location.href = 'index.html';
  });
})();
