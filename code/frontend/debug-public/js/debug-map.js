/**
 * 디버깅 웹(3030) 전용 스크립트 — map.js가 노출한 훅(window.__mt)을 사용한다.
 * - 폰 프레임 오른쪽 사이드 패널에 전체 여정·이동 정보·현재 상태 표시
 * - 페이지가 열리면 경로를 따라 초당 10m 자동 이동 (도착 팝업도 자동 진행)
 * - 지도에 전체 경로를 주황 점선으로 상시 표시
 */
(function boot() {
  const $ = (id) => document.getElementById(id);
  // map.js는 지도 SDK·플랜 준비가 끝난 뒤에야 훅(window.__mt)을 노출하므로 기다린다
  if (!window.__mt) {
    if (!window.__mtWaitStart) window.__mtWaitStart = Date.now();
    if (Date.now() - window.__mtWaitStart > 20000) {
      $('dbgBody').textContent = '디버그 훅(window.__mt)을 20초 내에 찾지 못했습니다. '
        + '백엔드(8080) 실행 여부와 TMAP_APP_KEY 설정을 확인하세요.';
      return;
    }
    setTimeout(boot, 200);
    return;
  }
  const mt = window.__mt;

  const WALK_SPEED_M_PER_S = 10;     // 도보 구간: 초당 10m
  const TRANSIT_SPEED_M_PER_S = 100; // 대중교통 구간: 초당 100m
  const TICK_MS = 250;               // 4틱/초

  const fmtDist = (m) => (m >= 1000 ? `${(m / 1000).toFixed(1)}km` : `${Math.round(m)}m`);
  const fmtMin = (min) => (min >= 60 ? `${Math.floor(min / 60)}시간 ${min % 60}분` : `${min}분`);
  const won = (n) => Number(n).toLocaleString('ko-KR') + '토큰';
  const esc = (t) => String(t ?? '').replace(/</g, '&lt;');

  // ---------- 전체 경로 상시 표시 ----------
  mt.addLine(mt.legs.flatMap((l) => l.path), { color: '#ffb86b', weight: 4, opacity: .8, dash: true });

  // ---------- 시뮬레이션: 초당 10m 자동 이동 ----------
  let simLeg = -1;
  let segIdx = 0, segT = 0;
  let pos = null;
  let legLen = 0;      // 현재 구간 전체 길이(m)
  let traveled = 0;    // 현재 구간에서 이동한 거리(m) — 가상 시계 보간용

  // 대중교통 구간 내 위상 경계: 승차 지점까지의 거리 / 하차 지점까지의 거리
  let boardDistM = -1;
  let alightDistM = Infinity;

  function resetToLeg(legIdx) {
    simLeg = legIdx;
    segIdx = 0;
    segT = 0;
    traveled = 0;
    const leg = mt.legs[legIdx];
    const pts = leg.path;
    pos = [...pts[0]];
    // 누적 거리 계산 + 승차/하차 지점의 경로상 위치 찾기
    legLen = 0;
    boardDistM = -1;
    alightDistM = Infinity;
    const board = leg.boardLat != null ? [leg.boardLat, leg.boardLng]
      : (Array.isArray(leg.stations) && leg.stations.length ? leg.stations[0] : null);
    const alight = leg.alightLat != null ? [leg.alightLat, leg.alightLng]
      : (Array.isArray(leg.stations) && leg.stations.length ? leg.stations[leg.stations.length - 1] : null);
    let bBest = Infinity, aBest = Infinity;
    let cum = 0;
    for (let i = 0; i < pts.length; i++) {
      if (i > 0) cum += distanceMeters(pts[i - 1][0], pts[i - 1][1], pts[i][0], pts[i][1]);
      if (leg.mode === 'TRANSIT') {
        if (board) {
          const db = distanceMeters(pts[i][0], pts[i][1], board[0], board[1]);
          if (db < bBest) { bBest = db; boardDistM = cum; }
        }
        if (alight) {
          const da = distanceMeters(pts[i][0], pts[i][1], alight[0], alight[1]);
          if (da < aBest) { aBest = da; alightDistM = cum; }
        }
      }
    }
    legLen = cum;
    // 승차/하차 좌표가 없는 대중교통 구간은 전체를 탑승으로 간주
    if (leg.mode === 'TRANSIT' && !board) boardDistM = 0;
  }

  // ---------- 가상 시계: 일정표 시각 + 구간 진행률 보간 ----------
  const toMin = (s) => {
    const m = /^(\d{1,2}):(\d{2})$/.exec(s || '');
    return m ? Number(m[1]) * 60 + Number(m[2]) : null;
  };
  const fmtClock = (min) =>
    `${String(Math.floor(min / 60) % 24).padStart(2, '0')}:${String(Math.floor(min % 60)).padStart(2, '0')}`;

  function updateClock() {
    const el = $('dbgClock');
    if (!el) return;
    let text = '--:--';
    if (mt.finished) {
      text = mt.legs[mt.legs.length - 1]?.arriveAt || '--:--';
    } else {
      const l = mt.legs[simLeg >= 0 ? simLeg : mt.currentLeg];
      const dep = toMin(l?.departAt);
      const arr = toMin(l?.arriveAt);
      if (dep != null && arr != null) {
        if (mt.overlayOpen) {
          text = l.arriveAt; // 도착해서 머무는 중
        } else {
          const frac = legLen > 0 ? Math.min(1, traveled / legLen) : 0;
          text = fmtClock(dep + frac * Math.max(0, arr - dep));
        }
      }
    }
    el.textContent = '🕐 ' + text;
  }

  function advance(meters) {
    const pts = mt.legs[simLeg].path;
    let remain = meters;
    while (remain > 0 && segIdx < pts.length - 1) {
      const [aLat, aLng] = pts[segIdx];
      const [bLat, bLng] = pts[segIdx + 1];
      const segLen = distanceMeters(aLat, aLng, bLat, bLng); // common.js 전역
      const left = segLen * (1 - segT);
      if (left > remain && segLen > 0) {
        segT += remain / segLen;
        remain = 0;
      } else {
        remain -= left;
        segIdx += 1;
        segT = 0;
      }
    }
    const [aLat, aLng] = pts[Math.min(segIdx, pts.length - 1)];
    const [bLat, bLng] = pts[Math.min(segIdx + 1, pts.length - 1)];
    pos = [aLat + (bLat - aLat) * segT, aLng + (bLng - aLng) * segT];
    traveled += meters - remain; // 실제 이동량 누적 (가상 시계 보간용)
  }

  // 배속 (×1 / ×2 / ×4 / ×8)
  let speedMult = 1;
  document.querySelectorAll('#dbgSpeed button').forEach((b) => {
    b.addEventListener('click', () => {
      speedMult = Number(b.dataset.mult) || 1;
      document.querySelectorAll('#dbgSpeed button').forEach((x) =>
        x.classList.toggle('on', x === b));
    });
  });

  let stallTicks = 0;
  let lastPosKey = '';
  let lastTickAt = performance.now();

  setInterval(() => {
    // 탭 스로틀링과 무관하게 정확한 속도를 내도록 경과 시간 기반으로 이동
    const now = performance.now();
    const dt = Math.min(2, (now - lastTickAt) / 1000);
    lastTickAt = now;
    if (mt.finished) {
      $('dbgSim').textContent = '■ 여정 완료';
      updateClock();
      return;
    }
    // 도착 팝업이 떠 있는 동안은 이동을 멈춘다 (다음 구간은 직접 버튼을 눌러 진행)
    if (mt.overlayOpen) {
      updateClock();
      $('dbgSim').textContent = '⏸ 도착 — 팝업에서 다음 진행';
      return;
    }
    if (simLeg !== mt.currentLeg) resetToLeg(mt.currentLeg);
    // 속도: 기본 10m/s, 승차 지점~하차 지점 사이(탑승 중)에만 100m/s
    let speed = WALK_SPEED_M_PER_S;
    let label = '▶ 🚶 10m/s 이동';
    if (mt.legs[simLeg]?.mode === 'TRANSIT') {
      if (traveled < boardDistM) {
        label = '▶ 🚶 10m/s (승차 지점으로)';
      } else if (traveled < alightDistM) {
        speed = TRANSIT_SPEED_M_PER_S;
        label = '▶ 🚌 100m/s (탑승 중)';
      } else {
        label = '▶ 🚶 10m/s (하차 후 도보)';
      }
    }
    $('dbgSim').textContent = label + (speedMult > 1 ? ` ×${speedMult}` : '');
    advance(speed * speedMult * dt);

    // 안전장치: 경로 끝에 도달했는데 도착 판정이 안 나서 2초 이상 제자리면
    // 목적지 좌표로 직접 스냅해 도착을 강제한다
    const key = pos[0].toFixed(7) + ',' + pos[1].toFixed(7);
    stallTicks = key === lastPosKey ? stallTicks + 1 : 0;
    lastPosKey = key;
    if (stallTicks >= 8) {
      const target = mt.stops[mt.currentLeg + 1];
      if (target) pos = [target.latitude, target.longitude];
      stallTicks = 0;
    }

    updateClock();
    mt.onPosition(pos[0], pos[1]);
  }, TICK_MS);

  // ---------- 사이드 패널 ----------
  $('dbgDay').textContent = mt.day;

  function render() {
    const legs = mt.legs;
    const stops = mt.stops;
    const totalDist = legs.reduce((s, l) => s + l.distanceMeters, 0);
    const totalMin = legs.reduce((s, l) => s + l.durationMinutes, 0);
    const totalFare = legs.reduce((s, l) => s + l.fare, 0);
    const spotCost = stops.reduce((s, x) => s + (x.cost || 0), 0);

    let h = `<div class="dbg-section"><div class="dbg-title">요약</div>`
      + `총 이동거리 ${fmtDist(totalDist)} · 총 이동시간 ${fmtMin(totalMin)}<br>`
      + `교통비 ${won(totalFare)} · 장소 비용 ${won(spotCost)} · 정지점 ${stops.length}개 / 구간 ${legs.length}개<br>`
      + `설계: ${esc(mt.plan.plannedBy || '?')} · 인원 ${mt.plan.people}명`
      + ` · 남은 토큰 ${mt.plan.tokenBalance != null ? won(mt.plan.tokenBalance) : '—'}</div>`;

    h += `<div class="dbg-section"><div class="dbg-title">현재 상태 (10m/s 자동 이동)</div>`
      + `<span class="dbg-now">구간 ${Math.min(mt.currentLeg + 1, legs.length)}/${legs.length}`
      + ` → 다음: ${esc(stops[mt.currentLeg + 1]?.name ?? '완료')}</span><br>`
      + `현재 좌표: ${pos ? pos[0].toFixed(6) + ', ' + pos[1].toFixed(6) : '준비 중'}<br>`
      + `이탈연속 ${mt.offStreak}회 · 연결선 ${mt.connector.path ? mt.connector.path.length + '점' : '없음'}`
      + ` · ${mt.finished ? '여정 완료' : '진행 중'}</div>`;

    h += `<div class="dbg-section"><div class="dbg-title">전체 여정</div>`;
    stops.forEach((s, i) => {
      const info = mt.TYPE_INFO[s.type] || mt.TYPE_INFO.ATTRACTION;
      const cls = i <= mt.currentLeg ? (i === mt.currentLeg ? 'now' : 'done') : '';
      h += `<div class="dbg-stop ${cls}">${i + 1}. ${info.emoji} [${esc(s.type)}] ${esc(s.name)}`
        + `${s.cost > 0 ? ' · ' + won(s.cost) : ''}${s.rating ? ' · ⭐' + s.rating.toFixed(1) : ''}`
        + `<br>&nbsp;&nbsp;&nbsp;${esc(s.address)} (${s.latitude.toFixed(5)}, ${s.longitude.toFixed(5)})</div>`;
      const l = legs[i];
      if (l) {
        h += `<div class="dbg-leg">${l.mode === 'TRANSIT' ? '🚌' : '🚶'} `
          + `[${l.departAt || '?'}~${l.arriveAt || '?'}] ${esc(l.summary)}`
          + ` · ${fmtDist(l.distanceMeters)} · ${fmtMin(l.durationMinutes)}`
          + `${l.fare > 0 ? ' · ' + won(l.fare) : ''}`
          + `${l.boardStop ? `<br>&nbsp;&nbsp;&nbsp;승차: ${esc(l.boardStop)} → 하차: ${esc(l.alightStop)}` : ''}`
          + `${Array.isArray(l.steps) && l.steps.length
              ? l.steps.map((st) => `<br>&nbsp;&nbsp;&nbsp;&nbsp;${st.kind === 'BUS' ? '🚌' : '🚶'} ${esc(st.description)}`).join('')
              : ''}`
          + ` · 경로점 ${l.path.length}개</div>`;
      }
    });
    h += `</div>`;
    $('dbgBody').innerHTML = h;
  }

  render();
  setInterval(render, 1000);
})();
