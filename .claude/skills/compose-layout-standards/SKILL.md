---
name: compose-layout-standards
description: Jetpack Compose 화면 레이아웃을 설계·구현할 때, Material 3와 Compose 공식 레이아웃 가이드를 따르고, 화면 간 패딩/간격/섹션 구조를 일관되게 유지하도록 하는 Skill.
allowed-tools:
  - Read
  - Grep
  - Glob
---
# Compose Layout & Screen Standards

Jetpack Compose 화면 레이아웃을 위한 일관성 규칙.

## 1. 공식 기준

- 레이아웃/스크롤/리스트/애니메이션은 `docs/compose-official-docs/SKILL.md` 에 정의된 Compose/M3 문서 기준을 따른다.

## 2. 기본 화면 구조

- `Scaffold` + TopAppBar + BottomBar + FAB(필요시) + content(PaddingValues)
- 콘텐츠:
    - 상단 `Column` 또는 `LazyColumn`
    - 섹션 간 16–24dp
    - 좌우 패딩: 모바일 16dp, 태블릿 이상 24dp+

## 3. 간격/패딩

- 4/8/12/16/24dp 세트 중심
- 동일 섹션 유형 동일 패딩
- `Spacing`/`Dimens` 상수로 관리

## 4. 스크롤/리스트

- 단순 스크롤: `Column` + `verticalScroll`
- 리스트: `LazyColumn`/`LazyRow`
- 리스트 아이템은 공통 컴포넌트 재사용 (`AppListItem` 등)

## 5. 상태별 UI

- 로딩/에러/Empty UI는 공통 컴포넌트 재사용
- 화면마다 새로 만들지 말 것

## 6. 반응형 & 텍스트 스케일

- `BoxWithConstraints`/윈도우 사이즈 클래스로 레이아웃 분기
- 정보 구조는 유지, 열/간격만 조정
- 폰트 스케일 1.3–1.5배, 다국어 길이 증가에도 깨지지 않도록 설계

## 7. 응답 패턴 & 체크리스트

- 화면 유형/레이아웃 패턴 분류
- 공식 문서 참조 섹션 언급
- 섹션/컴포넌트/패딩/간격 구체화
- Scaffold + Composable 코드 예시
- 체크리스트:
    - [ ] 공식 문서 기준 확인
    - [ ] 섹션 간 패딩/간격 일관성
    - [ ] 공통 상태 UI 재사용
    - [ ] 공통 컴포넌트 재사용
    - [ ] 텍스트 스케일/다국어 대응