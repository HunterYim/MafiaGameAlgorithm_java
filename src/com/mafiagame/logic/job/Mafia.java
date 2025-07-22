package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import com.mafiagame.ui.GameUI;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Mafia extends Job {

    private static final String JOB_NAME = "마피아";
    private static final Team TEAM = Team.MAFIA;
    private static final JobType JOB_TYPE = JobType.MAFIA;
    private static final String DESCRIPTION = "밤마다 한 사람을 지목하여 공격합니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;

    public Mafia() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public void onAssigned(Player self, GameManager gameManager) {
        List<Player> mafiaTeam = gameManager.getAllPlayers().stream()
                .filter(p -> p.getJob() instanceof Mafia && !p.equals(self))
                .collect(Collectors.toList());
        
        // GameManager에서 UI 객체를 직접 가져와 메시지를 즉시 출력합니다.
        GameUI ui = gameManager.getUi();

        if (mafiaTeam.isEmpty()) {
            ui.displayPrivateMessage(self, "당신 외에 다른 마피아는 없습니다.");
        } else {
            String teamMates = mafiaTeam.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            ui.displayPrivateMessage(self, "당신의 동료 마피아는 " + teamMates + " 입니다.");
        }
    }

    public boolean isAttackCommander(Player self, GameManager gameManager) {
        return gameManager.getLivingPlayers().stream()
                .filter(p -> p.getCurrentTeam() == Team.MAFIA && p.getJob() instanceof Mafia)
                .max(Comparator.comparingInt(Player::getPlayerNumber)) // Player 번호로 비교
                .map(p -> p.equals(self))
                .orElse(false);
    }
    
    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
    	if (isAttackCommander(self, gameManager)) {
            return "당신은 공격권이 있습니다. 공격할 대상을 선택하세요.";
        } else {
            return "당신은 공격권이 없습니다. 동료에게 추천할 대상을 선택하세요.";
        }
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        // 1. 역할에 맞는 프롬프트 생성
        String basePrompt;
        if (isAttackCommander(self, gameManager)) {
            basePrompt = "당신은 오늘 밤의 공격권자입니다. 공격할 대상을 선택하세요.";
        } else {
            basePrompt = "당신은 공격권이 없습니다. 추천할 대상을 선택하세요.";
        }
        Map<Player, Player> nominations = gameManager.getMafiaNominations();
        StringBuilder promptBuilder = new StringBuilder(basePrompt);
        if (!nominations.isEmpty()) {
            promptBuilder.append("\n[동료 지명 현황]");
            for (Map.Entry<Player, Player> entry : nominations.entrySet()) {
                promptBuilder.append("\n- ").append(entry.getKey().getName()).append(" -> ").append(entry.getValue().getName());
            }
        } else {
            promptBuilder.append("\n(아직 동료의 지명이 없습니다)");
        }
        String finalPrompt = promptBuilder.toString();

        // 2. 유효성 검사 루프 추가 및 필터링 제거 
        while (true) {
            Player target = gameManager.getPlayerInputForNightAction(self, finalPrompt, gameManager.getAllPlayers(), false);
            
            // Case 1: 아무도 선택하지 않은 경우 루프 종료
            if (target == null) {
                break;
            }

            // Case 2: 유효성 검사 - 자기 자신을 선택했는지 확인
            if (target.equals(self)) {
                gameManager.getUi().displayPrivateMessage(self, "자기 자신은 공격/지명할 수 없습니다. 다시 선택해주세요.");
                continue; // 다시 선택하도록 루프를 계속 진행
            }
            
            // Case 3: 유효성 검사를 통과한 경우, 행동을 기록하고 루프 종료
            if (isAttackCommander(self, gameManager)) {
                gameManager.recordNightAbilityTarget(self, target);
            } else {
                gameManager.recordMafiaNomination(self, target);
                gameManager.recordNightAbilityTarget(self, target);
            }
            break;
        }
    }

    @Override
    public int getNightActionPriority() {
        return 2; // 우선순위: 공격, 지명
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
    	if (!isAttackCommander(user, gameManager) || target == null) {
            return;
        }
    	
        if (target.getJob() instanceof Warewolf) {
            ((Warewolf) target.getJob()).triggerContact(gameManager, target, user);
            return;
        }

        if (target.isHealedByDoctor()) {
            gameManager.addPublicAnnouncement(target.getName() + "님이 마피아의 공격을 받았지만, 의사의 치료로 생존했습니다!");            
            return;
        }
        
        if (target.getJob() instanceof Soldier) {
            Soldier soldierJob = (Soldier) target.getJob();
            if (soldierJob.tryActivateDefense(target, gameManager)) {
                gameManager.addPublicAnnouncement(target.getName() + "님이 마피아의 공격을 받았으나, 군인의 방어 능력으로 살아남았습니다!");
                return;
            }
        }
        
        target.die();
    }
}