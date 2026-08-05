/**
 * 로그인 · 회원가입 화면 (login.html / signup.html 공용).
 * 검증 규칙은 백엔드 AuthService·EmailVerificationService 와 같은 기준을 쓴다 —
 * 화면에서 먼저 걸러 주고, 최종 판단은 항상 서버가 한다.
 */
(() => {
  const $ = (id) => document.getElementById(id);
  const EMAIL_RE = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/;
  const RESEND_COOLDOWN = 60; // 백엔드 EmailVerification.RESEND_COOLDOWN_SECONDS 와 동일

  const errorBox = $('errorBox');
  const submitBtn = $('submitBtn');

  function showError(msg) {
    errorBox.textContent = msg;
    errorBox.classList.add('show');
  }
  function clearError() {
    errorBox.classList.remove('show');
  }

  function setHint(id, msg, state) {
    const el = $(id);
    if (!el) return;
    el.textContent = msg;
    el.classList.toggle('error', state === 'error');
    el.classList.toggle('ok', state === 'ok');
  }

  function setLoading(on, label) {
    submitBtn.disabled = on;
    submitBtn.classList.toggle('loading', on);
    submitBtn.textContent = on ? label : submitBtn.dataset.label;
  }

  const mmss = (sec) => `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;

  /** 서버 응답을 공통으로 처리 — 실패 메시지는 백엔드가 준 한글 문구를 그대로 쓴다 */
  async function post(path, body) {
    let res;
    try {
      res = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
    } catch {
      throw new Error('서버에 연결할 수 없어요 — 잠시 후 다시 시도해 주세요.');
    }
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.message || '요청을 처리하지 못했어요.');
    return data;
  }

  // ---------- 비밀번호 표시/숨김 (디자인 시스템 Show·Hide 아이콘) ----------
  document.querySelectorAll('[data-pw-toggle]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const input = $(btn.dataset.pwToggle);
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.querySelector('img').src = show ? 'icons/Hide.png' : 'icons/Show.png';
      btn.setAttribute('aria-label', show ? '비밀번호 숨기기' : '비밀번호 표시');
    });
  });

  // 이미 로그인돼 있으면 홈으로
  if (loadAuth()) location.replace('index.html');

  const loginForm = $('loginForm');
  const signupForm = $('signupForm');
  submitBtn.dataset.label = submitBtn.textContent;

  // ---------- 구글 로그인 ----------
  // 클라이언트 ID가 설정된 경우에만 GIS 스크립트를 불러오고 버튼을 그린다.
  (async () => {
    let config;
    try {
      const res = await fetch(`${API_BASE}/api/auth/config`);
      config = await res.json();
    } catch {
      return; // 설정을 못 읽으면 이메일 로그인만 쓴다
    }
    if (!config?.googleClientId) return;

    await new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = 'https://accounts.google.com/gsi/client';
      s.async = true;
      s.onload = resolve;
      s.onerror = reject;
      document.head.appendChild(s);
    }).catch(() => null);
    if (!window.google?.accounts?.id) return;

    window.google.accounts.id.initialize({
      client_id: config.googleClientId,
      callback: async ({ credential }) => {
        clearError();
        try {
          const auth = await post('/api/auth/google', { credential });
          saveAuth(auth);
          location.href = 'index.html';
        } catch (err) {
          showError(err.message);
        }
      },
    });

    const block = $('socialBlock');
    block.hidden = false;
    window.google.accounts.id.renderButton($('googleBtn'), {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'pill',
      text: 'continue_with',
      locale: 'ko',
      width: Math.min(400, Math.round($('googleBtn').getBoundingClientRect().width)) || 320,
    });
  })();

  // ---------- 로그인 ----------
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearError();

      const email = $('email').value.trim();
      const password = $('password').value;
      if (!email || !password) {
        showError('이메일과 비밀번호를 모두 입력해 주세요.');
        return;
      }

      setLoading(true, '로그인 중…');
      try {
        const auth = await post('/api/auth/login', { email, password });
        saveAuth(auth);
        location.href = 'index.html';
      } catch (err) {
        showError(err.message);
        setLoading(false);
      }
    });
  }

  // ---------- 회원가입 ----------
  if (signupForm) {
    const email = $('email');
    const nickname = $('nickname');
    const password = $('password');
    const passwordConfirm = $('passwordConfirm');
    const code = $('code');
    const sendCodeBtn = $('sendCodeBtn');
    const verifyCodeBtn = $('verifyCodeBtn');

    /** 인증을 마친 이메일 주소. 이메일을 고치면 다시 인증해야 한다 */
    let verifiedEmail = null;
    let expiryTimer = null;
    let cooldownTimer = null;

    const stopTimer = (t) => (t ? clearInterval(t) : null);

    /** 인증번호 유효시간 카운트다운 */
    function startExpiry(seconds) {
      stopTimer(expiryTimer);
      let left = seconds;
      const tick = () => {
        $('codeTimer').textContent = left > 0 ? mmss(left) : '만료';
        if (left <= 0) {
          stopTimer(expiryTimer);
          setHint('codeHint', '인증번호가 만료됐어요. 다시 받아 주세요.', 'error');
        }
        left -= 1;
      };
      tick();
      expiryTimer = setInterval(tick, 1000);
    }

    /** 재발송 쿨다운 — 서버도 60초를 강제하므로 화면에서 미리 막아 준다 */
    function startCooldown() {
      stopTimer(cooldownTimer);
      let left = RESEND_COOLDOWN;
      sendCodeBtn.disabled = true;
      const tick = () => {
        sendCodeBtn.textContent = left > 0 ? `재발송 ${left}` : '재발송';
        if (left <= 0) {
          stopTimer(cooldownTimer);
          sendCodeBtn.disabled = false;
        }
        left -= 1;
      };
      tick();
      cooldownTimer = setInterval(tick, 1000);
    }

    /** 이메일이 바뀌면 인증 상태를 처음으로 되돌린다 */
    function resetVerification() {
      verifiedEmail = null;
      stopTimer(expiryTimer);
      stopTimer(cooldownTimer);
      expiryTimer = cooldownTimer = null;
      $('codeField').hidden = true;
      code.value = '';
      code.disabled = false;
      verifyCodeBtn.disabled = false;
      sendCodeBtn.disabled = false;
      sendCodeBtn.textContent = '인증요청';
      email.readOnly = false;
      email.classList.remove('verified');
      setHint('codeHint', '');
      $('codeTimer').textContent = '';
    }

    sendCodeBtn.addEventListener('click', async () => {
      clearError();
      const value = email.value.trim();
      if (!EMAIL_RE.test(value)) {
        email.classList.add('invalid');
        setHint('emailHint', '이메일 형식이 올바르지 않아요', 'error');
        return;
      }
      email.classList.remove('invalid');

      sendCodeBtn.disabled = true;
      const prev = sendCodeBtn.textContent;
      sendCodeBtn.textContent = '발송 중…';
      try {
        const { expiresInSeconds } = await post('/api/auth/email/send', { email: value });
        $('codeField').hidden = false;
        code.focus();
        setHint('codeHint', '메일로 받은 6자리 숫자를 입력해 주세요', null);
        setHint('emailHint', '인증번호를 보냈어요', 'ok');
        startExpiry(expiresInSeconds);
        startCooldown();
      } catch (err) {
        showError(err.message);
        sendCodeBtn.disabled = false;
        sendCodeBtn.textContent = prev;
      }
    });

    verifyCodeBtn.addEventListener('click', async () => {
      clearError();
      const value = code.value.trim();
      if (value.length !== 6) {
        setHint('codeHint', '인증번호 6자리를 입력해 주세요', 'error');
        return;
      }
      verifyCodeBtn.disabled = true;
      try {
        await post('/api/auth/email/verify', { email: email.value.trim(), code: value });
        verifiedEmail = email.value.trim().toLowerCase();
        stopTimer(expiryTimer);
        stopTimer(cooldownTimer);
        $('codeTimer').textContent = '';
        code.disabled = true;
        sendCodeBtn.disabled = true;
        sendCodeBtn.textContent = '인증완료';
        email.readOnly = true;
        email.classList.add('verified');
        setHint('codeHint', '이메일 인증이 끝났어요', 'ok');
        nickname.focus();
      } catch (err) {
        setHint('codeHint', err.message, 'error');
        verifyCodeBtn.disabled = false;
      }
    });

    code.addEventListener('input', () => {
      code.value = code.value.replace(/\D/g, '').slice(0, 6);
    });

    email.addEventListener('input', () => {
      if (verifiedEmail && email.value.trim().toLowerCase() !== verifiedEmail) resetVerification();
    });

    email.addEventListener('blur', () => {
      const v = email.value.trim();
      if (!v) return setHint('emailHint', '인증번호를 받을 주소예요');
      if (verifiedEmail === v.toLowerCase()) return;
      const ok = EMAIL_RE.test(v);
      email.classList.toggle('invalid', !ok);
      setHint('emailHint', ok ? '인증요청을 눌러 주세요' : '이메일 형식이 올바르지 않아요', ok ? null : 'error');
    });

    const pwRules = (v) => ({
      length: v.length >= 8,
      digit: /\d/.test(v),
      letter: /[A-Za-z]/.test(v),
    });

    nickname.addEventListener('input', () => {
      const len = nickname.value.trim().length;
      if (!len) return setHint('nicknameHint', '2~12자 — 여행 화면에서 이렇게 불러 드려요');
      const ok = len >= 2 && len <= 12;
      nickname.classList.toggle('invalid', !ok);
      setHint('nicknameHint', ok ? `${len}자 — 좋아요` : '닉네임은 2~12자로 입력해 주세요', ok ? 'ok' : 'error');
    });

    password.addEventListener('input', () => {
      const rules = pwRules(password.value);
      const score = Object.values(rules).filter(Boolean).length;
      $('pwMeter').className = `pw-meter lv${password.value ? score : 0}`;

      const missing = [];
      if (!rules.length) missing.push('8자 이상');
      if (!rules.letter) missing.push('영문자');
      if (!rules.digit) missing.push('숫자');
      password.classList.toggle('invalid', Boolean(password.value) && missing.length > 0);
      setHint(
        'passwordHint',
        missing.length ? `${missing.join(' · ')}가 더 필요해요` : '안전한 비밀번호예요',
        password.value ? (missing.length ? 'error' : 'ok') : null,
      );
      if (passwordConfirm.value) passwordConfirm.dispatchEvent(new Event('input'));
    });

    passwordConfirm.addEventListener('input', () => {
      if (!passwordConfirm.value) return setHint('passwordConfirmHint', '');
      const same = passwordConfirm.value === password.value;
      passwordConfirm.classList.toggle('invalid', !same);
      setHint('passwordConfirmHint', same ? '비밀번호가 일치해요' : '비밀번호가 서로 달라요', same ? 'ok' : 'error');
    });

    signupForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearError();

      const values = {
        email: email.value.trim(),
        nickname: nickname.value.trim(),
        password: password.value,
        passwordConfirm: passwordConfirm.value,
      };
      const rules = pwRules(values.password);

      if (!EMAIL_RE.test(values.email)) return showError('이메일 형식이 올바르지 않아요.');
      if (verifiedEmail !== values.email.toLowerCase()) return showError('이메일 인증을 먼저 해주세요.');
      if (values.nickname.length < 2 || values.nickname.length > 12) return showError('닉네임은 2~12자로 입력해 주세요.');
      if (!rules.length || !rules.letter || !rules.digit) return showError('비밀번호는 영문과 숫자를 섞어 8자 이상이어야 해요.');
      if (values.password !== values.passwordConfirm) return showError('비밀번호가 서로 달라요.');
      if (!$('agree').checked) return showError('약관에 동의해야 가입할 수 있어요.');

      setLoading(true, '가입하는 중…');
      try {
        const auth = await post('/api/auth/signup', values);
        saveAuth(auth);
        location.href = 'index.html';
      } catch (err) {
        showError(err.message);
        setLoading(false);
      }
    });
  }
})();
