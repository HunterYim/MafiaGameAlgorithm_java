package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
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
                .filter(p -> p.getJob().getInitialTeam() == Team.MAFIA && !p.equals(self))
                .collect(Collectors.toList());

        if (mafiaTeam.isEmpty()) {
            gameManager.recordPrivateNightResult(self, "당신 외에 다른 마피아는 없습니다.");
        } else {
            String teamMates = mafiaTeam.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            gameManager.recordPrivateNightResult(self, "당신의 동료 마피아는 " + teamMates + " 입니다.");
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
        return "공격할 대상을 선택하세요. (공격권이 없는 경우 공격 대상 추천으로 기록됩니다)";
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
    	String finalPrompt;
    	
    	// 1. 공격수일 경우, 동료들의 지명 현황을 확인
        if (isAttackCommander(self, gameManager)) {
            Map<Player, Player> nominations = gameManager.getMafiaNominations();
            StringBuilder promptBuilder = new StringBuilder(getNightActionPrompt(self, gameManager));
            
            if (!nominations.isEmpty()) {
                promptBuilder.append("\n[동료 지명 현황]");
                for (Map.Entry<Player, Player> entry : nominations.entrySet()) {
                    promptBuilder.append("\n- ").append(entry.getKey().getName()).append(" -> ").append(entry.getValue().getName());
                }
            } else {
                promptBuilder.append("\n(아직 동료의 지명이 없습니다)");
            }
            finalPrompt = promptBuilder.toString();
        
        // 2. 공격수가 아닐 경우 (지명자), 기본 안내 메시지 사용
        } else {
            finalPrompt = getNightActionPrompt(self, gameManager);
        }

        // 3. UI를 통해 대상 선택 입력받기
        List<Player> selectablePlayers = gameManager.getAllPlayers().stream()
                .filter(p -> !p.equals(self))
                .collect(Collectors.toList());
        
        if (selectablePlayers.isEmpty()) return;

        Player target = gameManager.getPlayerInputForNightAction(self, finalPrompt, selectablePlayers, false);
        if (target == null) return;
        
        // 4. 역할에 따라 다른 기록 메서드 호출
        if (isAttackCommander(self, gameManager)) {
            // 공격수는 최종 공격 대상을 기록 (applyNightEffect에서 사용됨)
            gameManager.recordNightAbilityTarget(self, target);
        } else {
            // 지명자는 실시간 지명 기록에 추가
            gameManager.recordMafiaNomination(self, target);
            // 동시에, 탐정의 '추리' 등을 위해 '능력을 사용했다'는 사실 자체는 기록해줌
            gameManager.recordNightAbilityTarget(self, target);
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
            gameManager.getAllPlayers().stream()
                .filter(p -> p.getJob() instanceof Doctor && p.isAlive())
                .findFirst()
                .ifPresent(doctor -> gameManager.recordPrivateNightResult(doctor, target.getName() + "님을 성공적으로 치료했습니다!"));
            return;
        }
        
        if (target.getJob() instanceof Soldier) {
            Soldier soldierJob = (Soldier) target.getJob();
            if (soldierJob.tryActivateDefense()) {
                gameManager.addPublicAnnouncement(target.getName() + "님이 마피아의 공격을 받았으나, 군인의 방어 능력으로 살아남았습니다!");
                return;
            }
        }
        
        target.die();
    }
}