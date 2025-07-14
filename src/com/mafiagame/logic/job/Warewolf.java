package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;
import java.util.stream.Collectors;

public class Warewolf extends Job {

    private static final String JOB_NAME = "늑대인간";
    private static final Team TEAM = Team.MAFIA; // 초기 소속은 마피아팀
    private static final JobType JOB_TYPE = JobType.WAREWOLF;
    private static final String DESCRIPTION = "마피아에게 공격받으면 접선하며, 모든 마피아 사망 후 밤마다 치료 불가 공격을 합니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;

    private boolean hasContacted = false;

    public Warewolf() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }
    
    public boolean hasContacted() {
        return this.hasContacted;
    }
    
    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "살육할 대상을 선택하세요. (치료 무시)";
    }
    
    /**
     * 마피아의 공격을 받았을 때 호출될 메서드
     */
    public void triggerContact(GameManager gameManager, Player self, Player attacker) {
        if (hasContacted) return;

        this.hasContacted = true;

        gameManager.addPublicAnnouncement("마피아가 누군가를 공격했지만 아무 일도 일어나지 않았습니다...");
        
        List<Player> livingMafiaTeam = gameManager.getLivingPlayers().stream()
                .filter(p -> p.getCurrentTeam() == Team.MAFIA)
                .collect(Collectors.toList());
        
        String teamMatesNames = livingMafiaTeam.stream()
                .filter(p -> !p.equals(self))
                .map(Player::getName)
                .collect(Collectors.joining(", "));
        
        String messageForWerewolf = "마피아 " + attacker.getName() + "의 공격을 받아 접선했습니다! 당신의 새로운 동료는 " + teamMatesNames + " 입니다.";
        gameManager.recordPrivateNightResult(self, messageForWerewolf);

        String messageForMafia = "우리의 공격 대상은 늑대인간이었습니다! 늑대인간 " + self.getName() + "이(가) 팀에 합류합니다.";
        for (Player mafiaMember : livingMafiaTeam) {
            if (!mafiaMember.equals(self)) {
                 gameManager.recordPrivateNightResult(mafiaMember, messageForMafia);
            }
        }
    }

    /**
     * 마피아가 모두 죽었는지 확인하는 헬퍼 메서드
     */
    private boolean areAllMafiasDead(GameManager gameManager) {
        return gameManager.getAllPlayers().stream()
                .filter(p -> p.getJob() instanceof Mafia)
                .noneMatch(Player::isAlive);
    }

    @Override
    public boolean canUseAbility(Player self, int dayCount, GameManager gameManager) {
        // '접선'했고, '모든 마피아가 죽었을 때'만 살육 능력 사용 가능
        return this.hasContacted && areAllMafiasDead(gameManager);
    }
    
    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        // canUseAbility가 true일 때만 호출됨
        List<Player> attackablePlayers = livingPlayers.stream()
                .filter(p -> !p.equals(self))
                .collect(Collectors.toList());
        
        if (attackablePlayers.isEmpty()) return;

        Player target = gameManager.getPlayerInputForNightAction(self, getNightActionPrompt(self, gameManager), attackablePlayers, false);
        if (target != null) {
            gameManager.recordNightAbilityTarget(self, target);
        }
    }

    @Override
    public int getNightActionPriority() {
        return 2; // 우선순위: 공격
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        if (target == null) return;
        
        if (target.getJob() instanceof Soldier) {
            Soldier soldierJob = (Soldier) target.getJob();
            if (soldierJob.tryActivateDefense(target, gameManager)) {
                gameManager.addPublicAnnouncement(target.getName() + "님이 늑대인간의 살육을 당했으나, 군인의 방어 능력으로 살아남았습니다!");
                return;
            }
        }
        
        target.die();
        gameManager.addPublicAnnouncement(user.getName() + "의 살육으로 " + target.getName() + "님이 끔찍하게 살해당했습니다!");
    }
}