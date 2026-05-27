#!/usr/bin/env bash
# 오늘도출근 Phase 4 + 5 + 6 커밋 스크립트
# 실행 전: Android Studio 완전히 닫기 (git lock 해제)
# 실행 방법: Git Bash 또는 WSL에서 bash commit_phases.sh

set -e
cd "$(dirname "$0")"

echo "=== 오늘도출근 Phase 커밋 ==="

# ── Phase 4 ───────────────────────────────────────────────────
# MissionRepository, MainViewModel, MissionDao 추가
# Note: MainActivity.kt는 Phase 5에 포함 (레벨 배지와 함께)
echo ""
echo "[Phase 4] 미션 기록 저장 + 메인 화면 통계..."

git add \
  app/src/main/java/com/yeon/todaymorning/data/db/MissionDao.kt \
  app/src/main/java/com/yeon/todaymorning/di/DatabaseModule.kt \
  app/src/main/java/com/yeon/todaymorning/data/repository/MissionRepository.kt \
  "app/src/main/java/com/yeon/todaymorning/ui/main/" \
  app/src/main/java/com/yeon/todaymorning/ui/timeattack/TimeAttackViewModel.kt

git commit -m "Phase 4 — 미션 기록 저장 + 메인 화면 통계"
echo "Phase 4 커밋 완료"

# ── Phase 5 ───────────────────────────────────────────────────
# 게이미피케이션: UserLevel, Lottie 결과 화면, 레벨 배지 포함 MainScreen
echo ""
echo "[Phase 5] 게이미피케이션..."

git add \
  app/src/main/java/com/yeon/todaymorning/domain/model/UserLevel.kt \
  "app/src/main/assets/" \
  "app/src/main/java/com/yeon/todaymorning/ui/result/" \
  app/src/main/java/com/yeon/todaymorning/ui/NavGraph.kt \
  app/src/main/java/com/yeon/todaymorning/ui/timeattack/TimeAttackScreen.kt \
  app/src/main/java/com/yeon/todaymorning/ui/MainActivity.kt \
  SESSION_LOG.md \
  CLAUDE.md

git commit -m "Phase 5 — 게이미피케이션 (Lottie 결과 화면, 레벨 시스템)"
echo "Phase 5 커밋 완료"

# ── Phase 6 ───────────────────────────────────────────────────
# 안정화: DB 마이그레이션, 중복 방지, ProGuard, release 빌드
echo ""
echo "[Phase 6] 안정화 + 배포 준비..."

git add \
  app/src/main/java/com/yeon/todaymorning/data/db/MissionRecord.kt \
  app/src/main/java/com/yeon/todaymorning/data/db/MissionDao.kt \
  app/src/main/java/com/yeon/todaymorning/data/db/AppDatabase.kt \
  app/src/main/java/com/yeon/todaymorning/data/repository/MissionRepository.kt \
  app/src/main/java/com/yeon/todaymorning/ui/timeattack/TimeAttackViewModel.kt \
  app/src/main/java/com/yeon/todaymorning/ui/result/MissionResultScreen.kt \
  app/proguard-rules.pro \
  app/build.gradle.kts

git commit -m "Phase 6 — 안정화 (중복 기록 방지, DB 마이그레이션, ProGuard, release 빌드)"
echo "Phase 6 커밋 완료"