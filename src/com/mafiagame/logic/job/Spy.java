package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;
import java.util.stream.Collectors;

public class Spy extends Job {

    private static final String JOB_NAME = "간첩";
    private static final Team TEAM = Team.SPY;
    private static final JobType JOB_TYPE = JobType.SPY;
    private static final String DESCRIPTION = "홀수 밤에는 포섭, 짝수 밤에는 지령을 내립니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;

    public Spy() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        if (gameManager.getDayCount() % 2 != 0) { // 홀수 밤
            return "포섭할 대상을 선택하세요.";
        } else { // 짝수 밤
            return "투표하도록 지령을 내릴 대상을 선택하세요.";
        }
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
        return 4; // 우선순위: 포섭
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        if (target == null) return;
        
        int dayCount = gameManager.getDayCount();

        // 홀수 밤: 포섭
        if (dayCount % 2 != 0) {
            boolean canRecruit = !(target.getJob() instanceof Mafia || target.getJob() instanceof Soldier);
            
            if (canRecruit && target.getCurrentTeam() != Team.SPY) {
                // 포섭 성공
                target.setCurrentTeam(Team.SPY);
                gameManager.addPublicAnnouncement("누군가 간첩에게 포섭당하였습니다.");
                gameManager.recordPrivateNightResult(user, target.getName() + "님(" + target.getJob().getJobName() + ")을 성공적으로 포섭했습니다.");
                gameManager.recordPrivateNightResult(target, "당신은 간첩에게 포섭되었습니다! 이제부터 간첩 팀 소속입니다. 당신을 포섭한 간첩은 " + user.getName() + " 입니다.");
            } else {
                // 포섭 실패
                gameManager.addPublicAnnouncement("간첩이 포섭을 시도했지만 실패했습니다.");
                gameManager.recordPrivateNightResult(user, target.getName() + "님은 포섭할 수 없는 대상입니다.");
            }
        }
        
        // 짝수 밤: 지령
        else {
            // 다른 간첩 팀원들에게만 지령 전달
            gameManager.getLivingPlayers().stream()
                .filter(p -> p.getCurrentTeam() == Team.SPY && !p.equals(user))
                .forEach(teamMate -> gameManager.recordPrivateNightResult(teamMate, "간첩의 지령: 다음 날 " + target.getName() + "님에게 투표하십시오."));
            
            // 지령을 내린 간첩에게 확인 메시지
            gameManager.recordPrivateNightResult(user, target.getName() + "님을 투표 대상으로 지령을 내렸습니다.");
        }
    }

    @Override
    public String getPrivateNightResultMessage(Player self, Object resultInfo, GameManager gameManager) {
        if (resultInfo instanceof String) {
            return (String) resultInfo;
        }
        return null;
    }
}