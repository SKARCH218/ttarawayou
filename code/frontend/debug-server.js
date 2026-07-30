// 디버깅 전용 웹 서버 (3030 포트).
// debug-public/ 의 파일이 우선 서빙되고, 없으면 일반 앱(public/)의 파일을 그대로 쓴다.
// → map.html만 디버그 버전으로 교체되고 나머지(예산/플랜 화면, css, js)는 공유된다.
const express = require('express');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3030;

app.use(express.static(path.join(__dirname, 'debug-public')));
app.use(express.static(path.join(__dirname, 'public')));

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`미스터리 여행 디버깅 웹: http://localhost:${PORT}`);
});
