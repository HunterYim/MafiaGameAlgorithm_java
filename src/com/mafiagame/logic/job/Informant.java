package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;
import java.util.stream.Collectors;

public class Informant extends Job {

    private static final String JOB_NAME = "정보원";
    private static final Team TEAM = Team.MAFIA; // 초기 소속은 마피아팀
    private static final JobType JOB_TYPE = JobType.INFORMANT;
    private static final String DESCRIPTION = "밤마다 한 명을 선택해 직업을 알거나, 마피아와 접선합니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;

    private boolean hasContacted = false;

    public Informant() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "직업을 알아낼 대상을 선택하세요. (대상이 마피아라면 '접선')";
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        // 살아있는 플레이어가 자기 자신뿐이면 능력 사용 불가
        if (livingPlayers.size() <= 1) {
            return;
        }

        List<Player> selectablePlayers = livingPlayers.stream()
                .filter(p -> !p.equals(self))
                .collect(Collectors.toList());

        if (selectablePlayers.isEmpty()) return;
        
        Player target = gameManager.getPlayerInputForNightAction(self, getNightActionPrompt(self, gameManager), selectablePlayers);
        if (target != null) {
            gameManager.recordNightAbilityTarget(self, target);
        }
    }

    @Override
    public int getNightActionPriority() {
        return 3; // 우선순위: 중간
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        if (target == null) return;

        // 아직 접선하지 않은 상태에서 '접선' 성공
        if (!hasContacted && target.getJob().getInitialTeam() == Team.MAFIA) {
            this.hasContacted = true;

            // 정보원에게 접선 성공 및 팀원 정보 전달
            List<Player> mafiaTeam = gameManager.getLivingPlayers().stream()
                    .filter(p -> p.getCurrentTeam() == Team.MAFIA)
                    .collect(Collectors.toList());
            String teamMates = mafiaTeam.stream().map(Player::getName).collect(Collectors.joining(", "));
            gameManager.recordPrivateNightResult(user, target.getName() + "님과 접선에 성공했습니다! 당신의 팀원은 " + teamMates + " 입니다.");

            // 다른 마피아들에게 정보원 접선 사실 알림
            for (Player mafia : mafiaTeam) {
                if (!mafia.equals(user)) {
                    gameManager.recordPrivateNightResult(mafia, "정보원 " + user.getName() + "이(가) 우리 팀에 합류했습니다.");
                }
            }
        }
        
        // 대상이 마피아가 아닌 경우 '첩보'
        else {
            gameManager.recordPrivateNightResult(user, target.getName() + "님의 직업은 [" + target.getJob().getJobName() + "] 입니다.");
        }
    }
}