/**
 * 디버깅 웹(3030) 전용 스크립트 — map.js가 노출한 훅(window.__mt)을 사용한다.
 * - 폰 프레임 오른쪽 사이드 패널에 전체 여정·이동 정보·현재 상태 표시
 * - 페이지가 열리면 경로를 따라 초당 10m 자동 이동 (도착 팝업도 자동 진행)
 * - 지도에 전체 경로를 주황 점선으로 상시 표시
 */
(() => {
  const mt = window.__mt;
  const $ = (id) => document.getElementById(id);
  if (!mt) {
    $('dbgBody').textContent = '디버그 훅(window.__mt)을 찾지 못했습니다. map.js 버전을 확인하세요.';
    return;
  }

  const SPEED_M_PER_S = 10;   // 초당 이동 거리
  const TICK_MS = 250;        // 부드럽게 4틱/초 → 틱당 2.5m
  const STEP_M = SPEED_M_PER_S * (TICK_MS / 1000);

  const fmtDist = (m) => (m >= 1000 ? `${(m / 1000).toFixed(1)}km` : `${Math.round(m)}m`);
  const fmtMin = (min) => (min >= 60 ? `${Math.floor(min / 60)}시간 ${min % 60}분` : `${min}분`);
  const won = (n) => Number(n).toLocaleString('ko-KR') + '원';
  const esc = (t) => String(t ?? '').replace(/</g, '&lt;');

  // ---------- 전체 경로 상시 표시 ----------
  L.polyline(mt.legs.flatMap((l) => l.path), {
    color: '#ffb86b', weight: 4, opacity: .8, dashArray: '6 8',
  }).addTo(mt.map);

  // ---------- 시뮬레이션: 초당 10m 자동 이동 ----------
  let simLeg = -1;
  let segIdx = 0, segT = 0;
  let pos = null;
  let arrivalTimer = null;

  function resetToLeg(legIdx) {
    simLeg = legIdx;
    segIdx = 0;
    segT = 0;
    pos = [...mt.legs[legIdx].path[0]];
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
  }

  let stallTicks = 0;
  let lastPosKey = '';

  setInterval(() => {
    if (mt.finished) {
      $('dbgSim').textContent = '■ 여정 완료';
      return;
    }
    // 도착 팝업이 떠 있으면 1초 후 자동으로 다음 구간 진행
    if (mt.overlayOpen) {
      if (!arrivalTimer) {
        arrivalTimer = setTimeout(() => {
          document.getElementById('revealNext').click();
          arrivalTimer = null;
        }, 1000);
      }
      return;
    }
    if (simLeg !== mt.currentLeg) resetToLeg(mt.currentLeg);
    advance(STEP_M);

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
