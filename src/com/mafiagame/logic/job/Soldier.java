package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

public class Soldier extends Job {

    private static final String JOB_NAME = "군인";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.SOLDIER;
    private static final String DESCRIPTION = "마피아의 공격을 한 번 방어할 수 있습니다.";
    private static final boolean HAS_NIGHT_ABILITY = false; // 군인은 스스로 능력을 사용하지 않음
    private static final boolean IS_ONE_TIME_ABILITY = true;  // 방어 능력은 1회성

    public Soldier() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }
    
    /**
     * 군인은 밤 능력 없음
     */
    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        // 아무것도 하지 않음
    }
    
    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "사용할 수 있는 능력이 없습니다.";
    }

    /**
     * 마피아나 늑대인간의 공격을 받았을 때 호출되는 방어 로직
     * 
     * @param self 이 직업을 가진 플레이어 객체
     * @param gameManager 게임 매니저 객체
     * @return 방어에 성공하면 true, 실패하면(이미 사용해서) false
     */
    public boolean tryActivateDefense(Player self, GameManager gameManager) {
        if (!this.oneTimeAbilityUsed) {
            this.oneTimeAbilityUsed = true;
            gameManager.revealJob(self);
            return true;
        }
        return false;
    }
}