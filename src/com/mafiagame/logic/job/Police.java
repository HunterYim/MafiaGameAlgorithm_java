package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;
import java.util.stream.Collectors;

public class Police extends Job {

    private static final String JOB_NAME = "경찰";
    private static final Team TEAM = Team.CITIZEN;
    private static final JobType JOB_TYPE = JobType.POLICE;
    private static final String DESCRIPTION = "밤마다 한 명을 선택해 마피아인지 여부를 알아냅니다.";
    private static final boolean HAS_NIGHT_ABILITY = true;
    private static final boolean IS_ONE_TIME_ABILITY = false;

    public Police() {
        super(JOB_NAME, TEAM, JOB_TYPE, DESCRIPTION, HAS_NIGHT_ABILITY, IS_ONE_TIME_ABILITY);
    }

    @Override
    public String getNightActionPrompt(Player self, GameManager gameManager) {
        return "조사할 대상을 선택하세요.";
    }

    @Override
    public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
        List<Player> selectablePlayers = livingPlayers.stream()
                .filter(p -> !p.equals(self))
                .collect(Collectors.toList());

        if (selectablePlayers.isEmpty()) {
            // 선택할 플레이어가 없는 경우 (매우 드문 케이스)
            return;
        }

        Player target = gameManager.getPlayerInputForNightAction(self, getNightActionPrompt(self, gameManager), selectablePlayers);
        if (target != null) {
            gameManager.recordNightAbilityTarget(self, target);
        }
    }

    @Override
    public int getNightActionPriority() {
        return 3; // 우선순위: 정보 수집
    }

    @Override
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        if (target == null) return;
        
        boolean isMafiaJob = target.getJob().getJobType() == JobType.MAFIA;

        // 조사 결과(대상 플레이어, 마피아 여부)를 Map 형태로 저장
        // getPrivateNightResultMessage에서 이 정보를 사용하여 최종 메시지 생성
        java.util.Map<String, Object> resultData = new java.util.HashMap<>();
        resultData.put("targetName", target.getName());
        resultData.put("isMafia", isMafiaJob);

        gameManager.recordPrivateNightResult(user, resultData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getPrivateNightResultMessage(Player self, Object resultInfo, GameManager gameManager) {
        if (resultInfo instanceof java.util.Map) {
            java.util.Map<String, Object> resultData = (java.util.Map<String, Object>) resultInfo;
            String targetName = (String) resultData.get("targetName");
            boolean isMafia = (Boolean) resultData.get("isMafia");

            if (isMafia) {
                return "조사 결과, " + targetName + "님은 마피아가 맞습니다.";
            } else {
                return "조사 결과, " + targetName + "님은 마피아가 아닙니다.";
            }
        }
        return "조사 결과를 가져오는 데 실패했습니다.";
    }
}