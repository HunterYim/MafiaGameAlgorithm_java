package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

public class GraveRobber extends Job {

    private static final String JOB_NAME = "도굴꾼";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.GRAVEROBBER;
    private static final String DESCRIPTION = "첫날 밤에 공격으로 죽은 사람의 직업을 얻습니다.";
    private static final boolean HAS_NIGHT_ABILITY = false; // 능동적인 밤 능력이 없음
    private static final boolean IS_ONE_TIME_ABILITY = true;

    public GraveRobber() {
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
}