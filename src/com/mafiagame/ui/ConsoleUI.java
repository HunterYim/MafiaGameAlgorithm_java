package com.mafiagame.ui;

import com.mafiagame.logic.game.Player;
import java.util.List;
import java.util.Scanner;

/**
 * GameUI 인터페이스의 콘솔(텍스트) 기반 구현체
 * 사용자의 입력을 받고, 콘솔에 게임 상태를 출력하는 모든 역할 담당
 */
public class ConsoleUI implements GameUI {

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public void displayPublicMessage(String message) {
        System.out.println("[전체 공지] " + message);
    }

    @Override
    public void displayPrivateMessage(Player player, String message) {
        if (player != null) {
            System.out.println("[" + player.getName() + "님께] " + message);
        } else {
            // actor가 null인 경우도 공지처럼 처리
            System.out.println(message);
        }
    }
    
    @Override
    public void displaySystemMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void clearScreen() {
        for (int i = 0; i < 30; i++) {
            System.out.println();
        }
        System.out.println("--- (화면이 전환되었습니다) ---");
    }

    @Override
    public void waitForPlayerConfirmation(Player actor, String prompt) {
        if (actor != null) {
            System.out.print("[" + actor.getName() + "님] " + prompt);
        } else {
            System.out.print(prompt);
        }
        scanner.nextLine();
    }

    @Override
    public Player promptForPlayerSelection(Player actor, String prompt, List<Player> targets, boolean allowDeadTargets) {
        displayPrivateMessage(actor, prompt);
        
        // "0. 아무도 선택하지 않음" 옵션
        displayPrivateMessage(actor, "0. 아무도 선택하지 않음");
        
        // 선택지 출력
        for (int i = 0; i < targets.size(); i++) {
            Player p = targets.get(i);
            String status = p.isAlive() ? "" : " (탈락)";
            displayPrivateMessage(actor, String.format("%d. %s%s", i + 1, p.getName(), status));
        }

        while (true) {
            System.out.print("[" + actor.getName() + "님] 번호를 입력하세요: ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                
                if (choice == 0) {
                    return null; 
                }
                
                int choiceIndex = choice - 1;
                if (choiceIndex >= 0 && choiceIndex < targets.size()) {
                	Player selected = targets.get(choiceIndex);
                    // 사망자 선택 가능 여부 체크 로직
                    if (!selected.isAlive() && !allowDeadTargets) {
                        displayPrivateMessage(actor, "사망한 플레이어는 선택할 수 없습니다. 다시 입력해주세요.");
                        continue;
                    }
                    
                    return selected; // 유효한 선택
                } else {
                    displayPrivateMessage(actor, "잘못된 번호입니다. 다시 입력해주세요.");
                }
            } catch (NumberFormatException e) {
                displayPrivateMessage(actor, "숫자로만 입력해주세요.");
            }
        }
    }
    
    /**
     * 게임이 완전히 종료될 때 Scanner 자원을 해제하기 위한 메서드
     * MainGame에서 호출 가능
     */
    public void close() {
        scanner.close();
    }
}