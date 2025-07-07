package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

public class Terrorist extends Job {

    private static final String JOB_NAME = "테러리스트";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.TERRORIST;
    private static final String DESCRIPTION = "투표로 처형당할 때, 원하는 플레이어 한 명을 같이 데려갈 수 있습니다.";
    private static final boolean HAS_NIGHT_ABILITY = false;
    private static final boolean IS_ONE_TIME_ABILITY = true;

    public Terrorist() {
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