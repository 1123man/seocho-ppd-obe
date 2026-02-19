---
name: android-material3-ui-guidelines
description: Jetpack Compose 기반 Android 앱을 위한 팀 공통 UI 가이드라인 Skill. Material Design 3를 기준으로 레이아웃/간격/타이포그래피/색상/표면·고도/다크 모드/국제화/접근성 규칙을 요약해, 화면과 컴포넌트를 설계·구현할 때 일관된 UI/UX를 유지한다.
---
# Android Material 3 UI Guidelines

Material 3 기반 팀 공통 UI 규칙 요약.

## 1. 레이아웃 & 간격

- 4dp 그리드, 권장 값: 4/8/12/16/20/24/32/40/48/56/64dp
- 터치 영역: 최소 48×48dp, 타깃 간 8dp
- 기본 구조: Scaffold + TopBar + BottomBar + content

## 2. 타이포그래피

- 역할: display/headline/title/body/label × large/medium/small
- 대표 매핑:
    - 화면/섹션 제목: `headlineSmall` / `titleLarge`
    - 카드/리스트 제목: `titleMedium`
    - 기본 본문: `bodyMedium`
    - 보조 텍스트: `bodySmall`
    - 버튼/칩 라벨: `labelLarge`/`labelMedium`

## 3. 색상 & 다크 모드

- `MaterialTheme.colorScheme` 만 사용
- 역할별 일관성 유지 (primary/onPrimary, surface, error 등)
- Light/Dark `ColorScheme` 모두 대비 확인
- Android 12+ 에서 동적 컬러 사용 시에도 역할 정의 유지

## 4. 표면·고도

- 0–5 레벨 고도 개념 사용
- 기본 표면: Level 0
- 카드/섹션: Level 1–2
- FAB/플로팅 패널: Level 3–4
- 다이얼로그/모달: Level 5
- Compose에서는 `tonalElevation` 우선, shadow/scrim은 강조 상황에만 사용

## 5. 컴포넌트 & 패턴

- 폼: 필드 간 8–16dp, 서포팅 텍스트 영역 고정해 “점프” 방지
- 리스트/카드: 공통 리스트 아이템/카드 컴포넌트 재사용, 간격은 4/8/12/16dp 중 선택

## 6. 접근성 & i18n

- 터치 타깃, 대비, 키보드 탐색, 스크린리더 라벨 고려
- 다국어/긴 문자열 시 줄 바꿈 허용, `maxLines`/`overflow` 설정
- RTL 지원 시 아이콘/정렬/패딩이 자연스럽게 반전되도록 구현
- 폰트 스케일 1.3–1.5배에서도 레이아웃 유지

## 7. 체크리스트

- [ ] 4dp 그리드 간격 사용
- [ ] 터치 타깃/간격 규칙 준수
- [ ] M3 타입 역할 일관 적용
- [ ] ColorScheme 기반 색상만 사용
- [ ] Elevation 레벨 의미에 맞게 사용
- [ ] 공통 패턴은 공통 컴포넌트로 재사용
- [ ] 다국어/폰트 스케일/다크 모드 대응