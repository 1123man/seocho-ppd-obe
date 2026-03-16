#!/bin/bash
# ============================================================
# PF-830 태블릿 초기 세팅 스크립트
# 기기에 APK 설치 후 1회 실행
#
# 사용법:
#   1. USB 데이터 케이블 또는 무선 디버깅으로 기기 연결
#   2. 무선 디버깅인 경우: adb connect <IP>:<PORT>
#   3. ./scripts/setup-device.sh
#   4. 기기에서 앱을 한 번 직접 실행 → "다른 앱 위에 표시" 권한 허용
#   5. 기기 재부팅하여 자동 시작 확인
# ============================================================

set -e

PKG="com.seocho.ppd.obe"

# 연결된 기기 확인
DEVICE_COUNT=$(adb devices | grep -v "List" | grep -c "device$" || true)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ 연결된 기기가 없습니다."
    echo "   USB 케이블 연결 또는 'adb connect <IP>:<PORT>' 실행 후 다시 시도하세요."
    exit 1
fi

echo "✅ 기기 연결 확인 (${DEVICE_COUNT}대)"
echo ""

# 앱 설치 여부 확인
if ! adb shell pm list packages | grep -q "$PKG"; then
    echo "❌ $PKG 앱이 설치되어 있지 않습니다."
    echo "   먼저 APK를 설치하세요."
    exit 1
fi
echo "✅ 앱 설치 확인"

# 1. 배터리 최적화 화이트리스트
echo "🔧 배터리 최적화 제외..."
adb shell cmd deviceidle whitelist +"$PKG"

# 2. DuraSpeed 비활성화 (MediaTek 기기)
echo "🔧 DuraSpeed 비활성화..."
adb shell settings put system duraspeed_enabled 0 2>/dev/null || true

# 3. 백그라운드 Activity 시작 허용
echo "🔧 백그라운드 Activity 시작 허용..."
adb shell settings put global background_activity_starts_enabled 1 2>/dev/null || true

echo ""
echo "============================================"
echo "✅ 기기 설정 완료!"
echo ""
echo "다음 단계:"
echo "  1. 기기에서 앱을 한 번 직접 실행하세요"
echo "  2. '다른 앱 위에 표시' 권한 허용 팝업이 뜨면 설정에서 허용"
echo "  3. 기기를 재부팅하여 자동 시작을 확인하세요"
echo "============================================"
