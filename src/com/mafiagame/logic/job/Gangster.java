package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;
import java.util.stream.Collectors;

public class Gangster extends Job {

    // 직업 속성 정의 private static final
	private static final String JOB_NAME = "건달";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.GANGSTER;
    private static final String DESCRIPTION = "밤마다 한 명을 협박하여 다음 날 낮의 투표를 금지시킵니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;
    
    /**
     * 생성자에서 위 상수들 사용
     */
    public Gangster() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }
    
    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "협박할 대상을 선택하세요.";
    }
    
    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        List<Player> selectablePlayers = livingPlayers.stream()
                .filter(p -> !p.equals(self))
                .collect(Collectors.toList());

        if (selectablePlayers.isEmpty()) return;

        Player target = gameManager.getPlayerInputForNightAction(self, getNightActionPrompt(self, gameManager), selectablePlayers, false);
        if (target != null) {
            gameManager.recordNightAbilityTarget(self, target);
        }
    }
    
    @Override
    public int getNightActionPriority() {
        return 5; // 우선순위: 방해/디버프 (가장 먼저 처리되도록 설정)
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        if (target == null) return;
        
        target.setCanVoteToday(false);
                
        // 협박당한 대상에게 보낼 결과 메시지
        gameManager.recordPrivateNightResult(target, "당신은 건달에게 협박당해 다음 날 투표를 할 수 없습니다.");
    }
}