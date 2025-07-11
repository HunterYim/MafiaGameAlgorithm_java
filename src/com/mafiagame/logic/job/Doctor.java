package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

public class Doctor extends Job {

    private static final String JOB_NAME = "의사";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.DOCTOR;
    private static final String DESCRIPTION = "밤마다 한 사람을 마피아의 공격으로부터 치료합니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;

    public Doctor() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "치료할 대상을 선택하세요.";
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        // 의사는 자기 자신을 포함하여 모든 생존자를 치료 가능
        Player target = gameManager.getPlayerInputForNightAction(self, getNightActionPrompt(self, gameManager), livingPlayers, false);
        if (target != null) {
            gameManager.recordNightAbilityTarget(self, target);
        }
    }

    @Override
    public int getNightActionPriority() {
        return 4; // 우선순위: 치료 (공격보다 높아야 함)
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        if (target == null) return;
        
        // 대상 플레이어의 '치료받음' 상태를 true로 설정
        target.setHealedByDoctor(true);
    }
}