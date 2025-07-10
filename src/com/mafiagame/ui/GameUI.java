package com.mafiagame.ui;

import com.mafiagame.logic.game.Player;
import java.util.List;

/**
 * 게임 로직과 사용자 인터페이스(UI) 간의 계약을 정의하는 인터페이스
 * GameManager는 이 인터페이스에만 의존하여 UI 종류(콘솔, GUI 등)에 상관없이 동작 가능 -> 안드로이드 개발 시 유용
 */
public interface GameUI {
	/**
     * 모든 플레이어에게 공개적으로 보여줄 메시지 출력
     * "[전체 공지]" 같은 접두어가 붙음

     * @param message 공개할 메시지
     */
	void displayPublicMessage(String message);

	/**
     * 특정 플레이어에게만 비공개로 보여줄 메시지 출력
     *
     * @param player  메시지를 수신할 플레이어
     * @param message 전달할 비공개 메시지
     */
    void displayPrivateMessage(Player player, String message);
    
    /**
     * 페이즈 제목 등 접두어가 필요 없는 시스템 메시지 출력
     * 
     * @param message 출력할 메시지
     */
    void displaySystemMessage(String message);

    /**
     * 다음 플레이어를 위해 화면을 전환하거나 이전 내용 삭제
     */
    void clearScreen();

    /**
     * 특정 플레이어가 진행을 위해 확인(예: Enter 입력)할 때까지 대기
     *
     * @param actor  확인을 수행할 플레이어 (null인 경우, "진행하려면..." 같은 일반 프롬프트)
     * @param prompt 안내 메시지
     */
    Player promptForPlayerSelection(Player actor, String prompt, List<Player> targets, boolean allowDeadTargets);

    /**
     * 특정 플레이어에게 선택지 목록을 보여주고, 그중 하나를 선택하도록 요청
     * 입력값이 유효할 때까지 반복하여 올바른 선택 보장
     *
     * @param actor   선택을 수행할 플레이어
     * @param prompt  안내 메시지
     * @param targets 선택 가능한 대상 플레이어 목록
     * @return 선택된 플레이어 객체
     */
    void waitForPlayerConfirmation(Player actor, String prompt);
}