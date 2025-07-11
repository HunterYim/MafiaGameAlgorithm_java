package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

public class Politician extends Job {

    private static final String JOB_NAME = "정치인";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.POLITICIAN;
    private static final String DESCRIPTION = "투표로 처형당할 때 한 번 무효화시킬 수 있으며, 투표권이 2표입니다.";
    private static final boolean HAS_NIGHT_ABILITY = false; // 밤 능력이 없음
    private static final boolean IS_ONE_TIME_ABILITY = true;  // 처세 능력은 1회성

    public Politician() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        // 아무것도 하지 않음
    }

    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "사용할 수 있는 능력이 없습니다.";
    }

    /**
     * 정치인의 투표권은 2표
     */
    @Override
    public int getVoteWeight() {
        return 2;
    }

    /**
     * '처세' 능력을 사용하여 처형 회피
     * 
     * @param self 이 직업을 가진 플레이어 객체
     * @param gameManager 게임 매니저 객체
     * @return 처세 능력 발동에 성공하면 true, 실패하면 false
     */
    public boolean tryEvadeExecution(Player self, GameManager gameManager) {
        if (!this.oneTimeAbilityUsed) {
            this.oneTimeAbilityUsed = true;
            gameManager.revealJob(self);
            return true;
        }
        return false;
    }
}