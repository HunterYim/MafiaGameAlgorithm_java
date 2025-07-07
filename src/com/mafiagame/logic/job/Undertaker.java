package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

public class Undertaker extends Job {

    private static final String JOB_NAME = "장의사";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.UNDERTAKER;
    private static final String DESCRIPTION = "밤에 단 한 번, 죽은 플레이어의 직업을 다음 날 아침 모두에게 공개합니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = true;

    public Undertaker() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public boolean canUseAbility(Player self, int dayCount, GameManager gameManager) {
        // 1회성 능력 사용 여부와 죽은 사람이 있는지 여부를 함께 확인
        return !this.oneTimeAbilityUsed && !gameManager.getDeadPlayers().isEmpty();
    }

    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "부검할 대상을 선택하세요.";
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        List<Player> deadPlayers = gameManager.getDeadPlayers();
        if (deadPlayers.isEmpty()) {
            // 이 경우는 canUseAbility에서 걸러지지만, 안전을 위해 추가
            return; 
        }

        Player target = gameManager.getPlayerInputForNightAction(self, getNightActionPrompt(self, gameManager), deadPlayers);
        if (target != null) {
            this.oneTimeAbilityUsed = true;
            gameManager.recordNightAbilityTarget(self, target);
        }
    }
}