---
name: compose-official-docs
description: Jetpack Compose, Material 3, Navigation, 커스텀 디자인 시스템과 관련된 작업에서 항상 docs/compose-official-docs.md에 정리된 공식 Android 문서를 1순위 기준으로 삼도록 하는 Skill. Compose API, Material 3 컴포넌트, Navigation 패턴을 사용할 때는 먼저 이 파일을 읽고 기준을 정리할 때 사용한다.
---
# Compose Official Docs Alignment

이 Skill이 활성화된 상태에서 Jetpack Compose 작업을 할 때는 항상 다음을 따른다.

## 1. 공식 문서 단일 소스

- 모든 공식 URL은 `docs/compose-official-docs/SKILL.md` 에만 모아둔다.
- 다른 곳에서는 URL을 직접 나열하지 않고 이 파일만 참조한다.
- 작업 전 `Read docs/compose-official-docs/SKILL.md` 로 관련 섹션을 파악한다.

## 2. 작업 전 기준 잡기 절차

1. `Read` 로 `docs/compose-official-docs.md` 읽기
2. 현재 작업(컴포넌트/레이아웃/네비게이션/디자인 시스템 등)과 관련된 항목 식별
3. 핵심 개념을 3–5줄로 요약해 응답 상단에 적기
4. 이후 설계/코드는 이 기준에 맞춰 작성

## 3. API/컴포넌트 사용 검증

- Button, Card, NavigationBar, Scaffold 등 M3 컴포넌트를 사용할 때:
    - 먼저 어떤 공식 문서 항목이 해당 컴포넌트를 다루는지 확인
    - 응답에서 “어떤 공식 문서를 기준으로 했는지” 한 줄로 언급

## 4. 비공식 정보 필터링

- 블로그/QA/예제는 참고용이며, 공식 문서와 충돌하면 공식 문서를 우선한다.
- 공식 문서에 없는 내용은 “버전 의존적인 참고사항”으로 명시한다.

## 5. 체크리스트

- [ ] `docs/compose-official-docs/SKILL.md` 를 읽고 관련 섹션을 식별했다
- [ ] 제안한 패턴/코드가 공식 기준과 충돌하지 않는다
- [ ] 공식 기준을 응답 상단에 요약했다