package com.mafiagame.logic.common.enums;

/**
 * 게임 진행 상태 페이즈 목록
 */
public enum GamePhase {
    // 게임 시작 전 설정 단계
    SETUP,

    // 첫날 밤: 직업 확인 및 즉시 능력 사용
    NIGHT_JOB_CONFIRM_ABILITY,

    // 일반 밤: 능력 사용
    NIGHT_ABILITY_USE,

    // 일반 밤: 비공개 결과 확인
    NIGHT_PRIVATE_CONFIRM,

    // 낮: 공개 결과 발표
    DAY_PUBLIC_ANNOUNCEMENT,

    // 낮: 토론
    DAY_DISCUSSION,

    // 낮: 투표
    DAY_VOTE,

    // 낮: 추방
    DAY_EXECUTION,

    // 낮: 테러 (추방된 플레이어가 테러리스트인 경우 진행)
    DAY_TERROR,

    // 게임 종료
    GAME_OVER;
}