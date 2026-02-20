# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Seocho PPD OBE Android 앱. Jetpack Compose 기반 그린필드 프로젝트.
Play Store 배포 없이 **APK 파일을 태블릿 기기에 수동 설치**하여 운용한다.
가로/세로 회전 모두 지원하는 **반응형 레이아웃** 필수.

## 기술 스택 정의

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Navigation**: Jetpack Navigation for Compose (kotlinx-serialization 기반 type-safe route)
- **Design System**: Material 3 기반 커스텀 디자인 시스템
- **Architecture**: Single-Activity, MVVM + 단방향 데이터 흐름
- **DI**: Hilt (KSP 기반 어노테이션 프로세싱)
- **네트워크**: Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx-serialization converter
- **Build**: AGP 8.9.1 + Gradle 8.11.1 + Kotlin 2.1.21
- **JDK**: 17 (빌드 타겟)

## 빌드 & 배포 명령어

```bash
# 디버그 빌드
./gradlew :app:assembleDebug

# 릴리즈 APK 빌드 (수동 설치용)
./gradlew :app:assembleRelease

# 에뮬레이터/기기에 설치 후 실행
./gradlew :app:installDebug && adb shell am start -n com.seocho.ppd.obe/.MainActivity

# 린트
./gradlew :app:lintDebug

# 단위 테스트
./gradlew :app:testDebugUnitTest

# 특정 테스트 클래스 실행
./gradlew :app:testDebugUnitTest --tests "com.seocho.ppd.obe.ClassName"
```

릴리즈 APK 출력 경로: `app/build/outputs/apk/release/app-release.apk`

## 빌드 설정

- Kotlin DSL (`build.gradle.kts`) 사용
- 버전 카탈로그 (`gradle/libs.versions.toml`) 로 의존성 관리
- Compose BOM (`2025.05.00`)으로 Compose 라이브러리 버전 통합 관리
- `namespace` / `applicationId`: `com.seocho.ppd.obe`
- `minSdk 26` / `compileSdk 36` / `targetSdk 35`

## API 설정

백엔드: **Java 17 Spring Boot** 앱. `BuildConfig`로 빌드 타입별 API Base URL을 자동 분기한다.

| 빌드 타입 | Base URL | 용도 |
|-----------|----------|------|
| `debug` | `http://10.0.2.2:10204/rest/api/v1/android-app/` | 로컬 개발 (에뮬레이터) |
| `release` | `https://seochobus.seocho.go.kr/rest/api/v1/android-app/` | 운영 배포 |

- `app/build.gradle.kts`의 `buildTypes` 블록에 `buildConfigField`로 `BASE_URL` 정의
- 코드에서는 `BuildConfig.BASE_URL`로 참조 (하드코딩 금지)
- API 엔드포인트 경로: `android-app/` 하위에 추가 (예: `android-app/route-info`)
- `release`는 HTTPS 필수, `debug`에서만 HTTP(cleartext) 허용
- 에뮬레이터에서 호스트 PC 접근 시 `10.0.2.2` 사용 (`localhost`는 에뮬레이터 자체를 가리킴)
- 로컬 개발 서버 포트: **10204**

### 외부 API 개발 참조

API request/response 개발 시 **백엔드 소스를 참조**하여 대응 개발한다:

- **백엔드 소스 경로**: `/Users/choinakhyun/Documents/project/seocho-ppd-sdk/seocho-point-drive`
- **Android 외부 API 컨트롤러**: `src/main/java/com/wecoms24/flow/android_external_api/AndroidExternalController.java`
- API 엔드포인트, 요청/응답 모델 구조를 백엔드 소스에서 확인 후 Android 데이터 모델(`data/model/`)에 반영
- 백엔드 JPA 엔티티는 필드가 많으므로, Android 모델에는 **앱에서 필요한 필드만** 선언 (`ignoreUnknownKeys = true` 설정으로 나머지 무시)
- `@JsonView(FlowDataJsonView.ListEntityView.class)` 어노테이션이 응답 직렬화 범위를 결정함

## 권한 요구사항

앱 기기에서 **인터넷 접속**과 **위치정보**가 필요함. AndroidManifest.xml에 다음 권한을 반드시 포함:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

위치 권한은 런타임 퍼미션이므로, 위치 기능 사용 전 `ActivityResultContracts.RequestPermission`으로 사용자 동의를 받아야 한다.

## 아키텍처

### 패키지 구조

```
com.seocho.ppd.obe/
├── MainActivity.kt          # Single-Activity (Compose setContent)
├── ObeApplication.kt        # @HiltAndroidApp
├── data/
│   ├── api/                  # Retrofit API 인터페이스 (ObeApiService)
│   └── model/                # API 응답 데이터 모델 (@Serializable)
├── di/                       # Hilt DI 모듈 (NetworkModule 등)
└── ui/
    ├── main/                 # MainViewModel + MainScreen 관련
    └── theme/                # M3 테마 (Color, Type, Theme)
```

### 계층 구조

```
UI (Composable + ViewModel)
  → Domain (UseCase / Interactor)
    → Data (Repository, DataSource, API/DB)
```

- **UI 계층**: 상태 렌더링 + 이벤트 전파만 담당. 비즈니스 로직 금지.
- **ViewModel**: `StateFlow` + sealed interface로 UI 상태 관리 (예: `RouteUiState.Loading/Success/Error`).
- **Composable**: `collectAsState`로 상태 구독, state hoisting (상태는 받고 변경은 콜백으로 위임).
- **네트워크**: Retrofit + OkHttp. `di/NetworkModule`에서 Hilt `@Singleton`으로 제공. `BuildConfig.BASE_URL` 기반.
- **Navigation**: 단일 `NavHost`, 개별 화면은 `NavController`를 직접 참조하지 않고 상위에서 주입된 콜백만 사용.
- **DI**: ViewModel/Repository/ApiService 모두 Hilt 컨테이너에서 관리, UI에서 직접 생성 금지.

### 디자인 시스템

Atomic Design 계층을 따름:
- **Atoms**: 버튼, 아이콘, 텍스트 (단일 요소)
- **Molecules**: 아이콘+텍스트, 버튼+배지 (소규모 조합)
- **Organisms**: 리스트 아이템, 카드 목록, 툴바, 섹션

핵심 규칙:
- 모든 색상 → `MaterialTheme.colorScheme`, 타이포 → `MaterialTheme.typography`, 쉐이프 → `MaterialTheme.shapes`
- 하드코딩 색상/폰트/크기 사용 금지
- 간격은 4/8/12/16/20/24/32/40/48dp 세트만 사용, `Spacing`/`Dimens` 상수로 관리
- 새 컴포넌트 생성 전 반드시 기존 컴포넌트 검색 → 확장 가능하면 확장 우선
- 컴포넌트는 stateless 우선 설계, 필요 시 별도 stateful 래퍼 제공
- 로딩/에러/Empty state는 공통 컴포넌트(`AppLoading`, `AppError`, `AppEmptyState`)로 재사용
- 모든 컴포넌트에 최소 1개 `@Preview` 필수

컴포넌트 네이밍: `접두사(App/Flow 등) + 역할(Button/Card/ListItem)` (예: `AppPrimaryButton`, `FlowOutlinedCard`)

## 태블릿 반응형 레이아웃

대상 기기가 태블릿이므로 가로/세로 회전에 모두 대응해야 한다.

- **WindowSizeClass** (`material3-window-size-class`) 사용하여 Compact / Medium / Expanded 분기
- 화면 설계 시 `currentWindowAdaptiveInfo()` 또는 `calculateWindowSizeClass()`로 너비/높이 클래스 판별
- **가로(Landscape)**: 좌우 분할(List-Detail), NavigationRail, 넓은 여백 활용
- **세로(Portrait)**: 단일 컬럼, BottomNavigation, 모바일 유사 레이아웃
- 좌우 패딩: Compact 16dp, Medium 24dp, Expanded 32dp+
- `@Preview` 작성 시 태블릿 크기 프리뷰 포함 (예: `@Preview(widthDp = 1280, heightDp = 800)`)
- 회전 시 상태 유지: ViewModel에 상태 보관, `rememberSaveable` 활용

## 문서 기준

- Compose/M3/Navigation/디자인 시스템 작업 시 `docs/compose-official-docs.md`에 정리된 공식 Android 문서를 단일 기준(Source of Truth)으로 사용
- 블로그/QA/비공식 예제가 공식 문서와 충돌하면 공식 문서 우선
- Context7 MCP로 최신 공식 문서를 조회하여 구현

## Skills & Agents

`.claude/skills/` 하위에 팀 표준 Skill이 정의되어 있음. 관련 작업 시 자동으로 활성화됨:

| Skill | 용도 |
|-------|------|
| `android-architecture-standards` | MVVM/계층 분리/상태 관리/네비게이션/DI 규칙 |
| `android-material3-ui-guidelines` | M3 레이아웃/간격/타이포/색상/고도/접근성 규칙 |
| `compose-component-standards` | 디자인 시스템 컴포넌트 설계/중복 방지/네이밍 규칙 |
| `compose-layout-standards` | 화면 레이아웃 구조/패딩/간격/반응형 규칙 |
| `compose-official-docs` | 공식 문서 우선 참조 절차 |

`.claude/agents/compose-ui-architect.md`: UI 설계/구현 시 활성화되는 Compose UI 아키텍트 에이전트.

## 언어 규칙

- 프로젝트 문서(`docs/`), 주석, 개발자 참고 사항: **한글**
- 코드(변수명, 함수명, 클래스명): 영문
- 커밋 메시지: 팀 컨벤션 따름
