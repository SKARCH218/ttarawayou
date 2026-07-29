/**
 * 미스터리 내비게이션 (GPS 전용).
 * - 지도 확대/축소·드래그 완전 잠금, 항상 내 위치 중심(반경 약 10~20m)
 * - 여정 시작 지점 = 지도를 연 순간의 현재 위치.
 *   계획된 출발점과 30m 이상 떨어져 있으면 첫 구간을 현재 위치 기준으로 재계산(OSRM)
 * - 경로는 현재 위치 앞부분만 조금씩 공개, 목적지 마커 없음
 * - 반경 20m 도착 순간에만 장소 이름 공개
 * - 경로 이탈(40m 초과) 시 경고 배너
 */
(function init() {
  // TMAP SDK는 map.html의 동기 로더가 백엔드(/api/config) 앱키로 심어준다.
  // 여기서는 준비될 때까지 대기하고, 실패 시 안내만 한다.
  if (!(window.Tmapv2 && window.Tmapv2.Map)) {
    const el = document.getElementById('statusSub');
    if (window.__tmapSdkFailed) {
      if (el) el.textContent = '지도 키를 불러오지 못했어요 — 백엔드(8080)와 TMAP_APP_KEY를 확인해 주세요';
    } else {
      if (!window.__tmapInitAt) window.__tmapInitAt = Date.now();
      else if (Date.now() - window.__tmapInitAt > 12000 && el) {
        el.textContent = '지도 로딩이 늦어지고 있어요 — 네트워크를 확인해 주세요';
      }
    }
    setTimeout(init, 60);
    return;
  }
  const plan = loadPlan();
  const day = Number(new URLSearchParams(location.search).get('day') || 1);
  if (!plan || !plan.dayPlans[day - 1]) {
    location.replace(plan ? 'plan.html' : 'index.html');
    return;
  }
  // 순차 진행: 이전 일차를 완료해야 이 일차를 열 수 있다
  const doneDayGate = Number(sessionStorage.getItem('mysteryDoneDay') || 0);
  if (day > doneDayGate + 1) {
    location.replace('plan.html');
    return;
  }
  const dayPlan = plan.dayPlans[day - 1];
  const stops = dayPlan.stops;
  const legs = dayPlan.legs;

  const ZOOM = 18;             // TMAP 고배율 줌 (한국 상세 지도)
  const ARRIVE_RADIUS = 20;    // 도착 판정 반경(m)
  const OFFROUTE_DIST = 40;    // 경로 이탈 판정(m)
  const FAR_OFF_DIST = 250;    // 이만큼 크게 벗어나면 구간 종류와 무관하게 재탐색(m)
  const REVEAL_AHEAD = 130;    // 앞으로 공개할 경로 길이(m)
  const REROUTE_DIST = 30;     // 계획된 출발점과 이만큼 떨어져 있으면 재계산(m)

  // ---------- 지도: TMAP SDK (줌 완전 잠금은 #mapLock 오버레이가 입력을 차단) ----------
  const start = stops[0];
  const TL = (lat, lng) => new Tmapv2.LatLng(lat, lng);
  const map = new Tmapv2.Map('map', {
    center: TL(start.latitude, start.longitude),
    width: '100%',
    height: '100%',
    zoom: ZOOM,
    zoomControl: false,
    scrollwheel: false,
  });

  function setCenter(lat, lng) {
    map.setCenter(TL(lat, lng));
  }

  /** HTML 마커 생성 (이모지·배지·사용자 점) */
  function htmlMarker(lat, lng, html, w, h) {
    return new Tmapv2.Marker({
      position: TL(lat, lng),
      iconHTML: html,
      iconSize: new Tmapv2.Size(w, h),
      map,
    });
  }

  /** Leaflet 호환 폴리라인 래퍼 (setLatLngs로 갱신) */
  function makeLine(opts) {
    let line = null;
    return {
      setLatLngs(pts) {
        if (line) { line.setMap(null); line = null; }
        if (!pts || pts.length < 2) return;
        line = new Tmapv2.Polyline({
          path: pts.map((p) => TL(p[0], p[1])),
          strokeColor: opts.color,
          strokeWeight: opts.weight,
          strokeOpacity: opts.opacity ?? 1,
          strokeStyle: opts.dash ? 'dash' : 'solid',
          map,
        });
      },
    };
  }

  const userMarkerObj = htmlMarker(start.latitude, start.longitude,
    '<div class="user-marker"></div>', 22, 22);
  const userMarker = {
    pos: [start.latitude, start.longitude],
    setLatLng(ll) { this.pos = [ll[0], ll[1]]; userMarkerObj.setPosition(TL(ll[0], ll[1])); },
    getLatLng() { return { lat: this.pos[0], lng: this.pos[1] }; },
  };

  const revealedPath = makeLine({ color: '#8d7bff', weight: 7, opacity: 0.95 });
  const walkedPath = makeLine({ color: '#3d3763', weight: 5, opacity: 0.7, dash: true });

  // ---------- 상태 ----------
  let currentLeg = 0;          // 지금 따라가는 구간 인덱스 (목적지는 stops[currentLeg+1])
  let finished = false;
  let overlayOpen = false;
  let rerouteChecked = false;  // 최초 GPS 수신 시 1회만 출발 경로 재계산
  let rerouting = false;
  let offStreak = 0;           // 연속 이탈 감지 횟수 (오탐 방지)
  let lastRerouteAt = 0;       // 마지막 재탐색 시각 (과도한 재호출 방지)
  // 내 위치 → 경로 진입점 연결선 (60m 이상 떨어져 있으면 실제 보행로로 안내)
  let connector = { leg: -1, path: null, from: null, fetching: false };
  // 실시간 버스 도착정보 (TAGO 전국 버스도착정보 — 대중교통 구간에서만 폴링)
  let busTimer = null;
  let ridingBus = false;   // 버스 탑승 중이면 도착정보 대신 하차 카운트다운 표시
  let legMarkers = [];     // 현재 구간의 승차🚏/하차🚏/목적지❓ 마커

  const $ = (id) => document.getElementById(id);
  const fmtDist = (m) => (m >= 1000 ? `${(m / 1000).toFixed(1)}km` : `${Math.round(m)}m`);

  /** 하단 안내 박스: 도보면 미스터리 문구, 대중교통이면 상세 탑승 정보 */
  function updateHintBox() {
    if (finished) return;
    const l = legs[currentLeg];
    if (l && l.mode === 'TRANSIT') {
      // 세부 단계(도보→승차→하차→도보)가 있으면 단계별로, 없으면 요약으로
      let body;
      if (Array.isArray(l.steps) && l.steps.length) {
        body = l.steps
          .map((s) => (s.kind === 'BUS' ? '🚌 ' : '🚶 ') + s.description)
          .join('<br>');
      } else {
        body = `<b>${l.boardStop || '근처 정류장'}</b>에서 승차 → `
          + `<b>${l.alightStop || '도착 정류장'}</b>에서 하차<br>`
          + `<span style="color:#8f8ab8;">${l.summary || ''}</span>`;
      }
      $('mysteryHint').innerHTML =
        '<span style="font-size:20px;">🚌</span><span>'
        + `<b>${l.departAt || ''}~${l.arriveAt || ''}</b><br>${body}</span>`;
    } else {
      $('mysteryHint').innerHTML =
        '<span style="font-size:20px;">🧭</span><span>목적지는 비밀! 발밑에 나타나는 <b>보라색 길</b>을 따라'
        + '가면 어딘가에 도착하게 됩니다…</span>';
    }
  }
  $('statusTitle').textContent = `Day ${day} 미스터리 여정`;
  updateProgressPill();

  $('backBtn').addEventListener('click', () => (location.href = 'plan.html'));

  // ---------- 기하 유틸 ----------
  const path = () => legs[currentLeg].path; // [[lat,lng], ...]

  /** 점을 폴리라인에 사영: {dist, segIdx, t, point} */
  function projectOnPath(lat, lng, pts) {
    let best = { dist: Infinity, segIdx: 0, t: 0, point: pts[0] };
    for (let i = 0; i < pts.length - 1; i++) {
      const [aLat, aLng] = pts[i];
      const [bLat, bLng] = pts[i + 1];
      // 근사 평면 좌표 (경주 규모에서 충분)
      const kx = Math.cos((aLat * Math.PI) / 180) * 111320;
      const ky = 110540;
      const ax = aLng * kx, ay = aLat * ky;
      const bx = bLng * kx, by = bLat * ky;
      const px = lng * kx, py = lat * ky;
      const dx = bx - ax, dy = by - ay;
      const len2 = dx * dx + dy * dy;
      const t = len2 === 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
      const cx = ax + t * dx, cy = ay + t * dy;
      const dist = Math.hypot(px - cx, py - cy);
      if (dist < best.dist) {
        best = { dist, segIdx: i, t, point: [cy / ky, cx / kx] };
      }
    }
    return best;
  }

  /** 사영 지점부터 앞으로 aheadMeters 만큼의 경로 조각 */
  function pathAhead(proj, pts, aheadMeters) {
    const out = [proj.point];
    let acc = 0;
    let prev = proj.point;
    for (let i = proj.segIdx + 1; i < pts.length && acc < aheadMeters; i++) {
      const d = distanceMeters(prev[0], prev[1], pts[i][0], pts[i][1]);
      acc += d;
      out.push(pts[i]);
      prev = pts[i];
    }
    return out;
  }

  /**
   * 현재 구간의 안내 마커 갱신:
   * - 목적지: ❓ (위치만 표시, 이름은 도착 전까지 비밀)
   * - 대중교통 구간: 승차 🚏 / 하차 🚏 정류장
   */
  function updateLegMarkers() {
    legMarkers.forEach((m) => m.setMap(null));
    legMarkers = [];
    if (finished) return;
    const mk = (lat, lng, html) => {
      legMarkers.push(htmlMarker(lat, lng, html, 60, 34));
    };
    const badge = (emoji, label, color) =>
      `<div style="text-align:center; filter:drop-shadow(0 2px 5px rgba(0,0,0,.7));">`
      + `<div style="font-size:24px; line-height:1;">${emoji}</div>`
      + `<div style="font-size:10px; font-weight:800; color:#fff; background:${color};`
      + ` border-radius:99px; padding:1px 7px; display:inline-block; margin-top:2px;">${label}</div></div>`;

    const target = stops[currentLeg + 1];
    if (target) mk(target.latitude, target.longitude, badge('❓', '목적지', '#7f6bff'));

    const l = legs[currentLeg];
    if (l && l.mode === 'TRANSIT') {
      const board = l.boardLat != null ? [l.boardLat, l.boardLng]
        : (Array.isArray(l.stations) && l.stations.length ? l.stations[0] : null);
      const alight = l.alightLat != null ? [l.alightLat, l.alightLng]
        : (Array.isArray(l.stations) && l.stations.length ? l.stations[l.stations.length - 1] : null);
      if (board) mk(board[0], board[1], badge('🚏', '승차', '#2f9e6e'));
      if (alight) mk(alight[0], alight[1], badge('🚏', '하차', '#b8543a'));
    }
  }

  /**
   * GPS 수신 전에도 현재 구간 출발점부터 앞 130m 경로를 미리 보여준다.
   * 경로가 화면 밖에 있어도 찾아갈 수 있도록 항상 내 위치(지도 중심)에서
   * 경로 시작점까지 연결선을 붙인다.
   */
  function drawInitialPath() {
    const pts = path();
    const here = userMarker.getLatLng();
    revealedPath.setLatLngs(
      [[here.lat, here.lng]].concat(pathAhead({ segIdx: 0, point: pts[0] }, pts, REVEAL_AHEAD))
    );
  }

  function legDistanceFrom(proj, pts) {
    let acc = 0;
    let prev = proj.point;
    for (let i = proj.segIdx + 1; i < pts.length; i++) {
      acc += distanceMeters(prev[0], prev[1], pts[i][0], pts[i][1]);
      prev = pts[i];
    }
    return acc;
  }

  // ---------- 경로 계산 (OSRM 도보, 실패 시 직선 폴백) ----------
  async function fetchWalkRoute(fromLat, fromLng, toLat, toLng) {
    let newPath = null;
    try {
      const url = `https://router.project-osrm.org/route/v1/foot/${fromLng},${fromLat};${toLng},${toLat}?overview=full&geometries=geojson`;
      const res = await fetch(url, { signal: AbortSignal.timeout(6000) });
      const json = await res.json();
      if (json.code === 'Ok' && json.routes?.[0]) {
        newPath = json.routes[0].geometry.coordinates.map(([x, y]) => [y, x]);
      }
    } catch (e) { /* 아래 직선 폴백 */ }
    if (!newPath) {
      newPath = [[fromLat, fromLng], [toLat, toLng]];
    }
    return newPath;
  }

  /** 현재 구간을 새 경로로 교체한다 */
  function replaceLegPath(newPath) {
    const dist = newPath.reduce((acc, p, i) =>
      i === 0 ? 0 : acc + distanceMeters(newPath[i - 1][0], newPath[i - 1][1], p[0], p[1]), 0);
    legs[currentLeg] = {
      mode: 'WALK',
      distanceMeters: dist,
      durationMinutes: Math.max(1, Math.round(dist / 67)),
      fare: legs[currentLeg].fare,
      summary: `도보 ${Math.round(dist)}m`,
      path: newPath,
    };
    walkedPath.setLatLngs([]);
    ridingBus = false;
    updateLegMarkers(); // 구간이 도보로 바뀌면 정류장 마커 제거, 목적지 ❓는 유지
  }

  // ---------- 시작 지점 = 현재 위치 ----------
  /**
   * 최초 GPS 수신 시: 계획된 출발점과 30m 이상 떨어져 있으면
   * 현재 위치 → 첫 목적지 도보 경로를 OSRM으로 재계산해 첫 구간을 교체한다.
   */
  async function rerouteFromHere(lat, lng) {
    rerouteChecked = true;
    if (currentLeg !== 0) return;
    // 대중교통 구간은 도보로 바꾸지 않는다 (경로 진입은 연결선이 안내)
    if (legs[0].mode === 'TRANSIT') return;
    const offM = distanceMeters(lat, lng, stops[0].latitude, stops[0].longitude);
    if (offM <= REROUTE_DIST) return;
    // 1km 이상 걷게 만들지 않는다: 너무 멀면 원래 경로 유지(연결선이 안내)
    if (distanceMeters(lat, lng, stops[1].latitude, stops[1].longitude) > 1000) return;

    rerouting = true;
    $('statusSub').textContent = '현재 위치에서 출발 경로를 준비하는 중…';
    const target = stops[1];
    replaceLegPath(await fetchWalkRoute(lat, lng, target.latitude, target.longitude));
    // 시작 지점 정보도 현재 위치로 교체
    stops[0] = { ...stops[0], name: '현재 위치', type: 'START', address: '출발 지점', latitude: lat, longitude: lng, cost: 0, rating: 0, description: '여행의 시작점' };
    rerouting = false;
    drawInitialPath();
  }

  // ---------- 경로 이탈 → 현재 위치 기준 재탐색 ----------
  /**
   * 길에서 벗어난 상태가 이어지면 현재 위치 → 다음 목적지 경로를 새로 계산한다.
   * 대중교통 구간은 버스 이동 중 오탐이 많아 도보 구간만 자동 재탐색한다.
   */
  async function rerouteCurrentLeg(lat, lng) {
    rerouting = true;
    lastRerouteAt = Date.now();
    $('statusSub').textContent = '길을 벗어났어요 — 새 경로를 찾는 중…';
    const target = stops[currentLeg + 1];
    replaceLegPath(await fetchWalkRoute(lat, lng, target.latitude, target.longitude));
    rerouting = false;
    offStreak = 0;
    $('offrouteBanner').classList.remove('show');
    onPosition(lat, lng); // 새 경로 기준으로 즉시 다시 그리기
  }

  /**
   * 경로에서 60m 이상 떨어져 있으면 내 위치 → 경로 진입점까지
   * 실제 보행로(OSRM)를 따라가는 연결선을 만든다. 가까우면 내 위치만 잇는다.
   */
  async function buildConnector(lat, lng, proj) {
    if (proj.dist <= 60) return [[lat, lng]];
    const movedM = connector.from
      ? distanceMeters(lat, lng, connector.from[0], connector.from[1]) : Infinity;
    if (connector.leg === currentLeg && connector.path && movedM < 120) return connector.path;
    if (connector.fetching) {
      return connector.leg === currentLeg && connector.path ? connector.path : [[lat, lng]];
    }
    connector.fetching = true;
    const p = await fetchWalkRoute(lat, lng, proj.point[0], proj.point[1]);
    connector = { leg: currentLeg, path: p, from: [lat, lng], fetching: false };
    return p;
  }

  // ---------- 실시간 버스 도착정보 (TAGO 전국) ----------
  function currentBusNo() {
    if (legs[currentLeg]?.mode !== 'TRANSIT') return null;
    const m = /버스\s*([^번\s·]+)번/.exec(legs[currentLeg].summary || '');
    return m ? m[1] : null;
  }

  /** 대중교통 구간이면 내 주변 정류소의 해당 노선 도착 예정을 배지로 보여준다 */
  async function refreshBusArrivals() {
    const el = $('busArrival');
    if (ridingBus) return; // 탑승 중에는 하차 카운트다운이 배지를 사용
    const busNo = currentBusNo();
    if (!busNo || finished || !lastFix) { el.style.display = 'none'; return; }
    try {
      const res = await fetch(`${API_BASE}/api/bus/arrivals`
        + `?lat=${lastFix[0]}&lng=${lastFix[1]}&busNo=${encodeURIComponent(busNo)}`);
      const data = await res.json();
      const a = (data.arrivals || [])[0];
      if (a && data.station) {
        const min = Math.max(1, Math.round(a.arrTimeSec / 60));
        el.textContent = `🚌 ${a.routeNo}번 · ${min}분 후 도착 (${a.prevStationCount}정거장 전) · ${data.station.name}`;
        el.style.display = 'block';
      } else {
        el.style.display = 'none';
      }
    } catch (e) { /* 다음 주기에 재시도 */ }
  }

  function startBusPolling() {
    if (busTimer) clearInterval(busTimer);
    refreshBusArrivals();
    busTimer = setInterval(refreshBusArrivals, 30000);
  }

  // ---------- 위치 갱신 ----------
  async function onPosition(lat, lng) {
    if (finished || overlayOpen) return;
    userMarker.setLatLng([lat, lng]);
    setCenter(lat, lng);

    if (!rerouteChecked) await rerouteFromHere(lat, lng);
    if (rerouting) return;

    const pts = path();
    const proj = projectOnPath(lat, lng, pts);

    // 경로에서 멀면 실제 보행로를 따라 경로 진입점까지 안내하는 연결선
    const conn = await buildConnector(lat, lng, proj);
    const guiding = conn.length > 2; // 실도로 연결선이 활성화된 상태

    // 경로 이탈 판정 → 연속 2회 이상 벗어나 있으면 현재 위치 기준으로 재탐색
    // (연결선 안내 중에는 이탈 경고 대신 진입 안내를 보여준다)
    const off = proj.dist > OFFROUTE_DIST;
    $('offrouteBanner').classList.toggle('show', off && !guiding);
    if (off) {
      offStreak += 1;
      // 도보 구간은 40m만 벗어나도 재탐색.
      // 대중교통 구간은 버스 이동 중 오탐이 많아, 250m 이상 크게 벗어났을 때만
      // (그리고 목적지가 걸어갈 만한 거리일 때만) 도보 경로로 재탐색한다.
      const targetStop = stops[currentLeg + 1];
      const toTargetM = distanceMeters(lat, lng, targetStop.latitude, targetStop.longitude);
      // 1km 이상 걷게 만들지 않는다: 대중교통 구간의 도보 전환은 목적지 1km 이내일 때만
      const canReroute = legs[currentLeg].mode === 'WALK'
        || (proj.dist > FAR_OFF_DIST && toTargetM <= 1000);
      if (canReroute && offStreak >= 2 && Date.now() - lastRerouteAt > 8000) {
        rerouteCurrentLeg(lat, lng);
        return;
      }
    } else {
      offStreak = 0;
    }

    // 앞부분만 조금씩 공개 — 항상 내 발밑에서 경로까지 이어지도록 연결선을 앞에 붙인다
    revealedPath.setLatLngs(conn.concat(pathAhead(proj, pts, REVEAL_AHEAD)));
    // 지나온 흔적 (은은한 점선)
    walkedPath.setLatLngs(pts.slice(0, proj.segIdx + 1).concat([proj.point]));

    const target = stops[currentLeg + 1];
    const remainLegM = Math.max(0, legDistanceFrom(proj, pts));
    // 오늘 남은 총거리 = 현재 구간 잔여 + 이후 구간 전체
    const remainTotalM = remainLegM + legs.slice(currentLeg + 1)
      .reduce((sum, l) => sum + l.distanceMeters, 0);
    updateProgressPill(remainTotalM);
    $('statusSub').textContent = off && !guiding
      ? '경로에서 벗어났어요'
      : (guiding ? `보라색 길을 따라 경로로 이동하세요 (${fmtDist(proj.dist)}) · ` : '')
        + `다음 비밀 장소까지 ${fmtDist(remainLegM)} ${legs[currentLeg].mode === 'TRANSIT' ? '· 🚌 ' + legs[currentLeg].summary : ''}`;

    // 버스 탑승 중이면 하차까지 남은 정거장 수를 표시
    const curLeg = legs[currentLeg];
    if (curLeg.mode === 'TRANSIT' && Array.isArray(curLeg.stations) && curLeg.stations.length > 1) {
      let ni = 0, nd = Infinity;
      curLeg.stations.forEach((s, i) => {
        const dist = distanceMeters(lat, lng, s[0], s[1]);
        if (dist < nd) { nd = dist; ni = i; }
      });
      const boardD = distanceMeters(lat, lng, curLeg.stations[0][0], curLeg.stations[0][1]);
      const lastSt = curLeg.stations[curLeg.stations.length - 1];
      // 하차 정류장을 80m 이상 지나쳤으면 하차 완료 → 배지 제거
      const pastAlight = ni >= curLeg.stations.length - 1
        && distanceMeters(lat, lng, lastSt[0], lastSt[1]) > 80;
      ridingBus = ni >= 1 && boardD > 120 && !pastAlight;
      if (ridingBus) {
        const remain = curLeg.stations.length - 1 - ni;
        $('busArrival').textContent = remain > 0
          ? `🚌 탑승 중 · 하차까지 ${remain}개 정류장 (하차: ${curLeg.alightStop || '안내 참고'})`
          : `🚌 곧 하차! ${curLeg.alightStop ? curLeg.alightStop + '에서 내리세요' : '다음 정류장에서 내리세요'}`;
        $('busArrival').style.display = 'block';
      } else if (pastAlight) {
        $('busArrival').style.display = 'none';
      }
    } else {
      ridingBus = false;
    }

    // 도착 판정: 반경 20m.
    // 단, OSRM이 경로를 도로 위로 스냅하면서 경로 끝점이 목적지에서 20~60m
    // 떨어질 수 있으므로, 경로를 거의 다 걸었고 목적지가 60m 이내면 도착으로 처리한다.
    const toTarget = distanceMeters(lat, lng, target.latitude, target.longitude);
    if (toTarget <= ARRIVE_RADIUS || (remainLegM < 15 && toTarget <= 60)) {
      reveal(target);
    }
  }

  function updateProgressPill(remainTotalM) {
    $('progressPill').textContent = `비밀 장소 ${currentLeg} / ${legs.length}`
      + (remainTotalM != null ? ` · 남은 거리 ${fmtDist(remainTotalM)}` : '');
  }

  // ---------- 도착 → 이름 공개 ----------
  function reveal(stop) {
    overlayOpen = true;
    const info = TYPE_INFO[stop.type] || TYPE_INFO.ATTRACTION;
    const isLast = currentLeg === legs.length - 1;
    $('revealEmoji').textContent = info.emoji;
    $('revealLabel').textContent = isLast ? '오늘의 여정 완료! 마지막 장소는…' : '도착! 이곳은…';
    $('revealName').textContent = stop.name;
    $('revealAddr').textContent = stop.address;
    $('revealDesc').textContent = stop.description || '';
    $('revealMeta').innerHTML =
      `<span>${info.label}</span>` +
      (stop.rating ? `<span>⭐ ${stop.rating.toFixed(1)}</span>` : '') +
      `<span>${stop.cost > 0 ? formatWon(stop.cost) : '무료'}</span>`;
    $('revealNext').textContent = isLast ? '🎉 여정 마치기' : '다음 비밀 장소로 →';
    $('revealOverlay').classList.add('show');

    // 공개된 장소에는 마커를 남긴다
    htmlMarker(stop.latitude, stop.longitude,
      `<div style="font-size:26px; filter:drop-shadow(0 2px 6px rgba(0,0,0,.6));">${info.emoji}</div>`,
      30, 30);
  }

  $('revealNext').addEventListener('click', () => {
    $('revealOverlay').classList.remove('show');
    overlayOpen = false;
    currentLeg += 1;
    if (currentLeg >= legs.length) {
      finished = true;
      // 일차 완료 기록 → 플랜 화면에서 다음 일차가 열린다
      const doneDay = Math.max(day, Number(sessionStorage.getItem('mysteryDoneDay') || 0));
      sessionStorage.setItem('mysteryDoneDay', String(doneDay));
      const next = plan.dayPlans[day]; // day는 1부터라 인덱스 day = 다음 일차
      $('statusSub').textContent = '오늘 여정을 모두 마쳤어요! 🎉';
      $('mysteryHint').innerHTML = next
        ? `<span style="font-size:20px;">🎉</span><span>Day ${day} 완료! 다음 여정(Day ${day + 1})은 `
          + `<b>${next.legs?.[0]?.departAt || '09:00'}</b>에 시작해요. 푹 쉬고 만나요!</span>`
        : '<span style="font-size:20px;">🏆</span><span>모든 여정을 완료했어요! 미스터리 여행 끝!</span>';
      revealedPath.setLatLngs([]);
      setTimeout(() => (location.href = 'plan.html'), 3200);
      return;
    }
    updateProgressPill();
    connector = { leg: -1, path: null, from: null, fetching: false };
    ridingBus = false;
    drawInitialPath();
    updateLegMarkers();   // 새 구간의 승차/하차/목적지 마커
    updateHintBox();      // 새 구간이 대중교통이면 탑승 정보 표시
    refreshBusArrivals(); // 새 구간이 대중교통이면 도착정보 갱신, 아니면 배지 숨김
    if (lastFix) onPosition(lastFix[0], lastFix[1]);
  });

  // ---------- GPS (단일 위치 소스) ----------
  let lastFix = null;

  function startGps() {
    if (!navigator.geolocation) {
      $('statusSub').textContent = '이 기기는 GPS를 지원하지 않아요';
      return;
    }
    navigator.geolocation.watchPosition(
      (pos) => {
        lastFix = [pos.coords.latitude, pos.coords.longitude];
        onPosition(pos.coords.latitude, pos.coords.longitude);
      },
      (err) => {
        $('statusSub').textContent = err.code === err.PERMISSION_DENIED
          ? '위치 권한이 필요해요 — 브라우저 설정에서 위치 접근을 허용해 주세요'
          : 'GPS 신호를 찾는 중… (실외에서 더 잘 잡혀요)';
      },
      { enableHighAccuracy: true, maximumAge: 1000, timeout: 15000 }
    );
  }

  drawInitialPath();  // GPS 수신 전에도 출발점부터 길이 보이게
  updateLegMarkers(); // 승차/하차/목적지 마커
  updateHintBox();    // 첫 구간이 대중교통이면 탑승 정보 표시
  startBusPolling();  // 대중교통 구간이면 실시간 도착정보 표시
  startGps();

  // 외부 디버그 도구(3030 포트 전용)가 페이지 로드 전에 플래그를 세운 경우에만
  // 내부 상태를 읽기/구동용으로 노출한다. 일반(3000) 페이지에서는 아무 것도 노출되지 않는다.
  if (window.__MT_DEBUG__) {
    window.__mt = {
      map, plan, day, stops, legs, TYPE_INFO,
      /** 디버그용 폴리라인 추가 (pts: [[lat,lng],...]) */
      addLine(pts, opts) {
        const line = makeLine(opts || { color: '#ffb86b', weight: 4, opacity: .8, dash: true });
        line.setLatLngs(pts);
        return line;
      },
      get currentLeg() { return currentLeg; },
      get finished() { return finished; },
      get overlayOpen() { return overlayOpen; },
      get lastFix() { return lastFix; },
      get offStreak() { return offStreak; },
      get connector() { return connector; },
      onPosition,
    };
  }
})();
