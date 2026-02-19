---
name: android-architecture-standards
description: Jetpack Compose 기반 Android 앱의 아키텍처/상태 관리/네비게이션/DI 규칙을 정의하는 Skill. MVVM + 단방향 데이터 흐름을 기본으로 하고, ViewModel/UseCase/Repository 계층을 명확히 분리하여 UI와 비즈니스 로직을 분리한다.
allowed-tools:
  - Read
  - Grep
  - Glob
---
# Android Architecture Standards (Compose)

Jetpack Compose 앱의 아키텍처 표준.

## 1. 계층 구조

- UI (Composable + ViewModel)
- Domain (UseCase/Interactor)
- Data (Repository, DataSource, API/DB)

원칙:
- UI는 상태 렌더링 + 이벤트 전파만 담당
- 비즈니스 로직은 UseCase/Repository 에 위치

## 2. 상태 관리

- ViewModel:
    - UI 상태: `StateFlow`/`Immutable data class`
    - 단방향 데이터 흐름 (Intent → ViewModel → State → UI)
- Compose:
    - `collectAsStateWithLifecycle` 사용
    - State hoisting: 컴포넌트는 상태를 받기만 하고, 변경은 콜백으로 위에 알림

## 3. 네비게이션

- Navigation Compose 기준으로:
    - route 상수 정의
    - NavHost 상단 하나, 컴포저블 간에는 “이벤트 콜백”으로만 네비게이션 요청
    - 개별 화면은 NavController를 직접 알지 못하고 상위에서 주입된 콜백만 사용

## 4. DI

- Hilt 또는 Koin 등 DI 프레임워크 사용
- ViewModel은 DI로 의존성 주입, UI에서 직접 생성하지 않음
- Repository/DataSource 도 DI 컨테이너에서 관리

## 5. 테스트 관점

- UseCase/Repository 는 유닛 테스트 가능하도록 인터페이스 기반 설계
- ViewModel 은 상태 변화를 검증하는 테스트를 작성할 수 있도록 순수 Kotlin 코드 유지

## 6. 체크리스트

- [ ] UI/Domain/Data 계층 분리
- [ ] ViewModel 이 StateFlow/단방향 데이터 흐름 사용
- [ ] Navigation 은 상위에서 콜백으로 제어
- [ ] DI 로 의존성 주입 (ViewModel/Repository)
- [ ] 비즈니스 로직이 Composable 내부에 존재하지 않음