---
name: compose-component-standards
description: Jetpack Compose에서 재사용 가능한 디자인 시스템 컴포넌트(버튼, 카드, 리스트 아이템 등)를 설계·구현할 때, 중복 컴포넌트를 만들지 않고, Material 3 및 팀 표준에 맞는 일관된 API/스타일을 강제하는 Skill.
allowed-tools:
  - Read
  - Grep
  - Glob
---
# Compose Component Standards

이 Skill은 Jetpack Compose 디자인 시스템/공통 컴포넌트 설계를 위한 규칙을 정의한다.

## 1. 디자인 시스템 계층

- Atoms: 버튼, 아이콘, 텍스트 등 단일 요소
- Molecules: 아이콘+텍스트, 버튼+배지 등 작은 조합
- Organisms: 리스트 아이템, 카드 목록, 툴바, 섹션 등

새 컴포넌트 설계 시:
1. 어느 계층인지 결정
2. 해당 계층에 유사 컴포넌트가 있는지 검색
3. 있으면 확장을 우선 고려

## 2. 중복 컴포넌트 탐지

1. 레포 구조 파악
    - `Read`/`Glob` 로 `ui/`, `design/`, `components/`, `common/` 확인
2. 유사 컴포넌트 검색
    - `Grep` + `@Composable` + `Button/Card/ListItem/Dialog/...`
3. 결과 정리
    - 유사 항목 목록 + 새 컴포넌트 필요 여부 근거 작성

## 3. 네이밍 & API

- PascalCase 사용 (`AppPrimaryButton`)
- 접두사: 프로젝트/도메인/디자인 시스템 (`App`, `Flow`)
- 접미사: 역할 (`Button`, `Card`, `ListItem`, `Badge` 등)
- 파라미터 순서:
    1. 필수 데이터
    2. 콜백
    3. 옵션(Modifier, enabled 등)
    4. 슬롯(Composable 람다)

## 4. Material 3 & 테마

- 색상: `MaterialTheme.colorScheme`만 사용
- 타이포: `MaterialTheme.typography` 기반, 필요 시 `copy()`
- 쉐이프: `MaterialTheme.shapes` 또는 M3 radius 스케일

## 5. Stateless & 비즈니스 로직 분리

- 코어 컴포넌트는 항상 stateless
- 상태는 상위에서 주입, 필요 시 별도 stateful 래퍼 제공
- 네트워크/DB/도메인 로직은 컴포넌트 밖(ViewModel/UseCase/Repository)에서 처리

## 6. Preview 필수

- 최소 1개 이상 `@Preview`
- 상태/variant 많으면 여러 Preview
- 디자인 시스템용은 Gallery 스타일 Preview 권장

## 7. 응답 패턴 & 체크리스트

- 중복 탐지 결과 요약
- 계층(Atom/Molecule/Organism) 명시
- 네이밍/파라미터/테마 적용 설명
- `@Composable` + `@Preview` 코드 예시
- 체크리스트:
    - [ ] 유사 컴포넌트 검색 완료
    - [ ] 네이밍/파라미터 규칙 준수
    - [ ] MaterialTheme 기반 스타일 사용
    - [ ] Stateless 설계 & 비즈니스 로직 분리
    - [ ] Preview 제공