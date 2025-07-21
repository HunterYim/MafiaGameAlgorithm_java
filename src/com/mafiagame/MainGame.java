package com.mafiagame;

import com.mafiagame.logic.common.enums.GameMode;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.ui.ConsoleUI;

// 게임 실행을 담당하는 메인 클래스
public class MainGame {

	public static void main(String[] args) {

        // 1. UI 구현체 먼저 생성
        ConsoleUI consoleUI = new ConsoleUI();

        // 2. GameManager를 생성할 때 UI 구현체 전달
        GameManager gameManager = new GameManager(consoleUI);

        // 3. 게임 설정 및 시작
        // 예: 8인 클래식 모드
        gameManager.setupGame(4, GameMode.CLASSIC);
        gameManager.startGame();
        
        // 4. 게임이 모두 끝나면 UI 리소스 해제
        consoleUI.close();
    }

}
