package com.mafiagame.logic.job;

import com.mafiagame.logic.common.enums.JobType;
import com.mafiagame.logic.common.enums.Team;     
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;
import java.util.List;

/**
 * 모든 직업의 기본이 되는 추상 클래스
 * 각 직업은 이 클래스를 상속받아 구체적인 능력과 특성을 구현
 */
public abstract class Job {

    protected String jobName;				// 직업 이름
    protected Team team;					// 해당 직업의 기본 소속 팀
    protected JobType jobType;				// 해당 직업 종류
    protected String description;			// 직업 설명 (UI 표시용)
    
    // 능력 관련
    protected boolean hasNightAbility;		// 밤 능력 사용 가능 여부
    protected boolean isOneTimeAbility; 	// 이 직업의 주 능력이 1회성인지 여부
    protected boolean oneTimeAbilityUsed;	// 1회성 능력을 사용했는지 여부

    /**
     * 생성자
     *
     * @param jobName 직업 이름
     * @param team 소속 팀
     * @param jobType 직업 종류
     * @param description 직업 설명
     * @param hasNightAbility 밤 능력 사용 가능 여부
     * @param isOneTimeAbility 주 능력이 1회성인지 여부
     */
    public Job(String jobName, Team team, JobType jobType, String description, boolean hasNightAbility, boolean isOneTimeAbility) {
        this.jobName = jobName;
        this.team = team;
        this.jobType = jobType;
        this.description = description;
        this.hasNightAbility = hasNightAbility;
        this.isOneTimeAbility = isOneTimeAbility;
        this.oneTimeAbilityUsed = false; // 기본적으로 사용 안 함으로 초기화
    }

    // --- Getter 메서드들 ---
    public String getJobName() {
        return jobName;
    }

    public Team getInitialTeam() { // 초기 팀 반환 (간첩 포섭 고려)
        return team;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasNightAbility() {
        return hasNightAbility;
    }

    public boolean isOneTimeAbility() {
        return isOneTimeAbility;
    }

    public boolean hasUsedOneTimeAbility() {
        return oneTimeAbilityUsed;
    }

    /**
     * 1회성 능력을 사용했음을 표시
     * 이 메서드는 1회성 능력을 사용하는 직업의 performNightAction 내부에서 호출될 수 있습니다.
     */
    protected void markOneTimeAbilityUsed() {
        if (isOneTimeAbility) {
            this.oneTimeAbilityUsed = true;
        }
    }
    
    /**
     * 특정 조건에서 1회성 능력을 사용을 시도
     * (예: 정치인의 처세, 군인의 방어 - 피격 시 발동)
     * 
     * 성공하면 내부적으로 oneTimeAbilityUsed 플래그를 true로 설정
     *
     * @return 능력 사용에 성공했으면 true, 실패했으면 (이미 사용했거나 조건 미충족 등) false
     */
    public boolean tryActivateConditionalOneTimeAbility() {
        if (isOneTimeAbility() && !hasUsedOneTimeAbility()) {
            // 여기에 추가적인 발동 조건이 있다면 해당 직업 클래스에서 이 메서드를 오버라이드하여 검사 가능.
            // 예를 들어, 군인의 방어는 '공격 대상이 되었을 때'만 발동.
            // 정치인의 처세는 '추방 대상이 되었을 때' 발동.
            // 이 메서드가 호출되는 시점 자체가 그 조건이 만족되었다고 볼 수도 있음.
            markOneTimeAbilityUsed(); // 내부적으로 protected 메서드 호출
            return true; // 능력 사용 성공
        }
        return false; // 능력 사용 실패 (이미 사용했거나, 1회성 능력이 아니거나 등)
    }


    /**
     * 현재 이 직업이 밤 능력을 사용할 수 있는지 확인
     * (예: 1회성 능력인데 이미 사용했거나, 특정 조건(엠바고, 홀/짝수 밤)을 만족하지 못하는 경우 등)
     * 
     * 추가적인 직업별 사용 조건은 하위 클래스에서 필요에 따라 Override 하여 구체적인 조건을 추가
     *
     * @param self 능력을 사용하려는 플레이어 자신
     * @param dayCount 현재 게임 일차
     * @param gameManager 게임 매니저 객체 (게임 상태 접근용)
     * @return 능력 사용 가능 여부
     */
    public boolean canUseAbility(Player self, int dayCount, GameManager gameManager) {
        if (!hasNightAbility) { // 밤 능력이 없는 직업
            return false;
        }
        if (isOneTimeAbility && oneTimeAbilityUsed) { // 1회성 능력인데 이미 사용한 경우
            return false;
        }
        return true;
    }
    
    /**
     * 밤 능력 처리의 우선순위 반환
     * 숫자가 높을수록 먼저 처리
     * 
     * (예: 방해/방어 5, 치료 4, 정보수집 3, 공격 2, 기타 1)
     * 
     * @return 능력 처리 우선순위 정수
     */
    public int getNightActionPriority() {
        return 1; // 대부분의 직업은 기본 우선순위로 설정
    }

    /**
     * 밤 능력의 결과를 실제 게임 상태에 적용
     * GameManager가 정해진 순서에 따라 이 메서드를 호출하며, 세부 로직은 각 직업 클래스가 구현
     *
     * @param gameManager 게임 매니저 (공개 공지 추가, 다른 플레이어 상태 확인 등)
     * @param user        능력을 사용한 플레이어
     * @param target      능력의 대상이 된 플레이어
     */
    public void applyNightEffect(GameManager gameManager, Player user, Player target) {
        // 능력이 없는 직업이나, 적용할 효과가 없는 직업은 아무것도 하지 않음
        // 필요한 직업 클래스에서 메서드 재정의
    }

    /**
     * 추상 메서드 -> 각 직업 클래스는 이 메서드 구현
     * 
     * 밤에 수행할 직업 능력을 정의하는 추상 메서드
     * 
     * 능력 사용 결과(대상 선택 등)는 GameManager를 통해 처리되거나,
     * 이 메서드 내에서 직접 GameManager의 상태를 변경 가능
     *
     * @param self 능력을 사용하는 플레이어 자신
     * @param livingPlayers 현재 살아있는 모든 플레이어 목록 (대상 선택용)
     * @param gameManager 게임 매니저 객체 (입력 처리, 메시지 출력, 게임 상태 접근/변경용)
     */
    public abstract void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager);

    /**
     * 추상 메서드 -> 각 직업 클래스는 이 메서드 구현
     * 
     * (텍스트용) 밤에 능력 사용 시 사용자에게 보여줄 안내 메시지 반환
     * 각 직업 클래스에서 구체적인 안내 메시지 구현
     *
     * @param self 능력을 사용하려는 플레이어 자신
     * @return 능력 사용 안내 프롬프트 문자열
     */
    public abstract String getNightActionPrompt(Player self);


    /**
     * (선택 사항) 추상 메서드 -> 각 직업 클래스는 이 메서드 구현
     * 
     * 직업이 플레이어에게 처음 배정될 때 호출될 수 있는 메서드
     * 
     * (예: 마피아가 동료 마피아를 인지하는 등의 초기 설정을 수행)
     *
     * @param self 이 직업을 배정받은 플레이어
     * @param gameManager 게임 매니저 객체
     */
    public void onAssigned(Player self, GameManager gameManager) {
        // 기본적으로 아무것도 하지 않음. 필요한 직업에서 재정의.
    }

    /**
     * (선택 사항) 추상 메서드 -> 각 직업 클래스는 이 메서드 구현
     * 
     * 밤 능력 사용 후, '비공개 확인' 페이즈에서 해당 플레이어에게 전달할 개인적인 결과 메시지를 생성
     * 
     * @param self 능력을 사용한 플레이어 자신
     * @param target 선택했던 대상 플레이어 (null일 수 있음)
     * @param gameManager 게임 매니저 객체
     * @return 개인 결과 메시지 문자열 (없으면 null 또는 빈 문자열 반환)
     */
    public String getPrivateNightResultMessage(Player self, Object resultInfo, GameManager gameManager) {
        return null;
    }

}