/**
 * 취향 질문 화면 — 한 화면에 질문 하나.
 * 레이아웃: 이모지(정가운데) → 질문 → 선택지.
 * 단일 선택은 고르면 자동으로 다음 질문, 다중 선택·서술형은 [다음]으로 넘어간다.
 * 마지막 질문을 마치면 플랜을 생성하고 plan.html로 이동한다.
 */
(() => {
  const setup = loadSetup();
  if (!setup) {
    location.replace('index.html');
    return;
  }

  // 답변 저장소 (전부 선택 사항 — 건너뛰면 null)
  const answers = {
    purpose: null,
    gender: null,
    ageGroup: null,
    mbti: { ei: null, sn: null, tf: null, jp: null },
    foodPreference: null,
    keywords: [],
    avoidWalking: null,
    preferenceNote: '',
  };

  const QUESTIONS = [
    {
      emoji: '🧭',
      q: '어떤 여행을 원하세요?',
      type: 'single',
      key: 'purpose',
      options: ['휴양', '관광', '미식', '액티비티'],
    },
    {
      emoji: '🙂',
      q: '성별을 알려주세요',
      hint: '취향이 비슷한 여행자의 코스를 참고해요',
      type: 'single',
      key: 'gender',
      options: ['남', '여', '선택 안 함'],
      wide: true,
    },
    {
      emoji: '🎂',
      q: '연령대는 어떻게 되세요?',
      type: 'single',
      key: 'ageGroup',
      options: ['10대', '20대', '30대', '40대', '50대+'],
    },
    {
      emoji: '🧠',
      q: 'MBTI를 알려주세요',
      hint: '모르면 건너뛰어도 괜찮아요',
      type: 'mbti',
    },
    {
      emoji: '🍚',
      q: '어떤 음식을 좋아하세요?',
      type: 'single',
      key: 'foodPreference',
      options: ['한식', '양식', '일식', '중식', '상관없음'],
    },
    {
      emoji: '🏞️',
      q: '어떤 곳에 가고 싶으세요?',
      hint: '여러 개 고를 수 있어요',
      type: 'multi',
      key: 'keywords',
      options: ['산', '바다', '공원', '강'],
    },
    {
      emoji: '🚶',
      q: '많이 걷는 건 괜찮으세요?',
      type: 'single',
      key: 'avoidWalking',
      options: [
        { label: '괜찮아요', value: false },
        { label: '적게 걷고 싶어요', value: true },
      ],
      wide: true,
    },
    {
      emoji: '💬',
      q: '더 알려주실 취향이 있나요?',
      hint: 'AI가 장소를 고를 때 참고해요',
      type: 'note',
    },
  ];

  let idx = 0;

  const $ = (id) => document.getElementById(id);
  const body = $('body');

  // ---------- 진행 표시 ----------
  const progress = $('progress');
  QUESTIONS.forEach(() => progress.appendChild(document.createElement('i')));

  function renderProgress() {
    [...progress.children].forEach((el, i) => el.classList.toggle('on', i <= idx));
    $('count').textContent = `${idx + 1} / ${QUESTIONS.length}`;
  }

  // ---------- 질문 렌더링 ----------
  function render() {
    const item = QUESTIONS[idx];
    renderProgress();
    body.innerHTML = '';

    const emoji = document.createElement('div');
    emoji.className = 'ask-emoji';
    emoji.textContent = item.emoji;
    body.appendChild(emoji);

    const q = document.createElement('h1');
    q.className = 'ask-question';
    q.textContent = item.q;
    body.appendChild(q);

    if (item.hint) {
      const hint = document.createElement('p');
      hint.className = 'ask-hint';
      hint.textContent = item.hint;
      body.appendChild(hint);
    }

    if (item.type === 'single') renderSingle(item);
    else if (item.type === 'multi') renderMulti(item);
    else if (item.type === 'mbti') renderMbti();
    else if (item.type === 'note') renderNote();

    // 단일 선택은 고르면 자동 진행하므로 [다음]을 숨긴다
    $('nextBtn').style.display = item.type === 'single' ? 'none' : '';
    $('skipBtn').style.display = item.type === 'single' ? 'none' : '';
    $('backBtn').textContent = idx === 0 ? '설정' : '이전';
    $('nextBtn').textContent = idx === QUESTIONS.length - 1 ? '플랜 만들기' : '다음';
  }

  function optionButton(label, selected, onClick, wide) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'ask-opt' + (selected ? ' on' : '') + (wide ? ' wide' : '');
    btn.textContent = label;
    btn.addEventListener('click', onClick);
    return btn;
  }

  function renderSingle(item) {
    const wrap = document.createElement('div');
    wrap.className = 'ask-options';
    item.options.forEach((opt) => {
      const label = typeof opt === 'object' ? opt.label : opt;
      const value = typeof opt === 'object' ? opt.value : opt;
      const selected = answers[item.key] === value;
      wrap.appendChild(optionButton(label, selected, () => {
        answers[item.key] = value;
        // 고른 걸 잠깐 보여준 뒤 다음 질문으로
        [...wrap.children].forEach((c) => c.classList.remove('on'));
        wrap.children[item.options.indexOf(opt)].classList.add('on');
        setTimeout(next, 180);
      }, item.wide));
    });
    body.appendChild(wrap);
  }

  function renderMulti(item) {
    const wrap = document.createElement('div');
    wrap.className = 'ask-options';
    item.options.forEach((opt) => {
      const btn = optionButton(opt, answers[item.key].includes(opt), () => {
        const list = answers[item.key];
        const at = list.indexOf(opt);
        if (at >= 0) list.splice(at, 1); else list.push(opt);
        btn.classList.toggle('on');
      });
      wrap.appendChild(btn);
    });
    body.appendChild(wrap);
  }

  function renderMbti() {
    const AXES = [
      ['ei', 'E', 'I'],
      ['sn', 'S', 'N'],
      ['tf', 'T', 'F'],
      ['jp', 'J', 'P'],
    ];
    const wrap = document.createElement('div');
    wrap.className = 'ask-mbti';
    AXES.forEach(([axis, a, b]) => {
      const row = document.createElement('div');
      row.className = 'ask-mbti-row';
      [a, b].forEach((ch) => {
        const btn = optionButton(ch, answers.mbti[axis] === ch, () => {
          answers.mbti[axis] = answers.mbti[axis] === ch ? null : ch;
          [...row.children].forEach((c) => c.classList.remove('on'));
          if (answers.mbti[axis]) btn.classList.add('on');
        });
        row.appendChild(btn);
      });
      wrap.appendChild(row);
    });
    body.appendChild(wrap);
  }

  function renderNote() {
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'ask-note';
    input.maxLength = 120;
    input.placeholder = '예: 매운 음식 좋아요, 조용한 카페 위주로';
    input.value = answers.preferenceNote;
    input.addEventListener('input', () => (answers.preferenceNote = input.value));
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') next(); });
    body.appendChild(input);
  }

  // ---------- 이동 ----------
  function next() {
    if (idx < QUESTIONS.length - 1) {
      idx += 1;
      render();
    } else {
      createPlan();
    }
  }

  $('nextBtn').addEventListener('click', next);
  $('skipBtn').addEventListener('click', next);
  $('backBtn').addEventListener('click', () => {
    if (idx === 0) {
      location.href = 'index.html';
      return;
    }
    idx -= 1;
    render();
  });

  // ---------- 플랜 생성 ----------
  const LOADING_MESSAGES = [
    'AI가 예산을 숙박·관광·식비·교통에 배분하고 있어요',
    '취향에 맞는 장소를 고르는 중이에요',
    '예산을 알뜰하게 쓰는 조합을 찾고 있어요',
    '버스 노선과 도보 경로를 살피는 중이에요',
    '경로를 숨기는 중',
  ];

  function mbtiOrNull() {
    const { ei, sn, tf, jp } = answers.mbti;
    return ei && sn && tf && jp ? ei + sn + tf + jp : null;
  }

  function requestBody() {
    return {
      budget: setup.budget,
      days: setup.days,
      people: setup.people,
      region: setup.region,
      startLatitude: null,
      startLongitude: null,
      purpose: answers.purpose,
      gender: answers.gender === '남' ? 'MALE' : answers.gender === '여' ? 'FEMALE' : null,
      ageGroup: answers.ageGroup,
      mbti: mbtiOrNull(),
      foodPreference: answers.foodPreference === '상관없음' ? null : answers.foodPreference,
      keywords: answers.keywords.length ? answers.keywords : null,
      avoidWalking: answers.avoidWalking === true,
      preferenceNote: answers.preferenceNote.trim() || null,
    };
  }

  async function postPlan() {
    const res = await fetch(`${API_BASE}/api/plan`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody()),
    });
    if (!res.ok) {
      let msg = `서버 오류 (${res.status})`;
      try { msg = (await res.json()).message || msg; } catch { /* 그대로 */ }
      const err = new Error(msg);
      err.status = res.status;
      throw err;
    }
    return res.json();
  }

  async function createPlan() {
    $('errorBox').classList.remove('show');
    $('loading').classList.add('show');
    let msgIdx = 0;
    const ticker = setInterval(() => {
      msgIdx = (msgIdx + 1) % LOADING_MESSAGES.length;
      $('loadingMsg').textContent = LOADING_MESSAGES[msgIdx];
    }, 2600);

    try {
      let plan;
      try {
        plan = await postPlan();
      } catch (e) {
        // 토큰 선결제는 시뮬레이션이라 플랜을 만들 때마다 잔액이 줄어든다.
        // 잔액이 예산에 못 미치면 테스트 지갑을 충전해 한 번 다시 시도한다
        // (실제 서비스라면 이 자리에 결제가 들어간다).
        if (!/토큰/.test(e.message)) throw e;
        await fetch(`${API_BASE}/api/wallet/reset`, { method: 'POST' });
        plan = await postPlan();
      }
      savePlan(plan);
      sessionStorage.removeItem('trevitDoneDay');
      location.href = 'plan.html';
    } catch (e) {
      $('errorBox').textContent = `플랜 생성 실패: ${e.message}`;
      $('errorBox').classList.add('show');
    } finally {
      clearInterval(ticker);
      $('loading').classList.remove('show');
    }
  }

  render();
})();
