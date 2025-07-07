package com.mafiagame.logic.game;

import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.job.Job;

/**
 * 게임에 참여하는 개별 플레이어를 나타내는 클래스
 * 플레이어의 이름, 번호, 직업, 생존 여부, 팀 정보 등을 관리
 */
public class Player {

    private final String name;         // 플레이어 이름 (변경 불가)
    private final int playerNumber;    // 플레이어 고유 번호 (변경 불가)
    private Job job;                   // 플레이어가 맡은 직업
    private boolean isAlive;           // 생존 여부
    private Team initialTeam;          // 플레이어에게 처음 배정된 팀
    private Team currentTeam;          // 현재 소속된 팀 (간첩 포섭으로 변경 가능)
    private boolean canVoteToday;      // 오늘 투표 가능 여부 (건달 협박 시 false)

    // 밤 능력 결과 처리를 위한 임시 상태 변수 (GameManager가 밤 결과 계산 시 사용, 매일 밤 시작 전에 초기화)
    private boolean targetedByMafiaAttack; // 마피아의 공격 대상이 되었는지
    private boolean healedByDoctor;        // 의사의 치료를 받았는지

    /**
     * 생성자
     *
     * @param name 플레이어 이름
     * @param playerNumber 플레이어 고유 번호
     * @param job 플레이어에게 할당된 직업 객체
     */
    public Player(String name, int playerNumber, Job job) {
        this.name = name;
        this.playerNumber = playerNumber;
        this.isAlive = true;
        this.canVoteToday = true;
        setJob(job); // setJob 메서드가 job과 팀 설정 
        resetNightStatus(); // 밤 상태 변수 초기화
    }

    // --- Getter 메서드 ---
    public String getName() { return name; }

    public int getPlayerNumber() { return playerNumber; }

    public Job getJob() { return job; }

    public boolean isAlive() { return isAlive; }

    public Team getInitialTeam() { return initialTeam; }

    public Team getCurrentTeam() { return currentTeam; }

    public boolean canVoteToday() { return canVoteToday; }

    public boolean isTargetedByMafiaAttack() { return targetedByMafiaAttack; }

    public boolean isHealedByDoctor() { return healedByDoctor; }

    // --- Setter 및 상태 변경 메서드 ---
    
    /**
     * 플레이어에게 직업 할당, 초기 팀 설정
     * 
     * @param job 할당할 직업 객체
     */
    public final void setJob(Job job) {
        this.job = job;
        if (job != null) {
            // 직업이 처음 할당되거나 변경될 때, 현재 팀은 초기 팀과 동일하게 설정
            this.initialTeam = job.getInitialTeam();
            this.currentTeam = job.getInitialTeam();
        }
    }

    public void die() {
        this.isAlive = false;
    }

    /**
     * 플레이어의 현재 팀 변경
     * 
     * 팀 변경 관련 로그나 알림은 GameManager가 담당
     *
     * @param newTeam 변경할 새로운 팀
     * @return 팀 변경 성공 여부 (예: 이미 같은 팀이거나 변경 불가능한 조건일 경우 false 반환 가능)
     */
    public boolean setCurrentTeam(Team newTeam) {
    	if (newTeam == null || this.currentTeam == newTeam) {
            return false; // 변경할 필요가 없거나 잘못된 요청
        }
        this.currentTeam = newTeam;
        return true;
    }
	
    public void setCanVoteToday(boolean canVote) {
        this.canVoteToday = canVote;
    }

    public void setTargetedByMafiaAttack(boolean targeted) {
        this.targetedByMafiaAttack = targeted;
    }

    public void setHealedByDoctor(boolean healed) {
        this.healedByDoctor = healed;
    }

    /**
     * 매일 밤 시작 시, 밤 동안의 임시 상태 변수 초기화
     * 
     * GameManager가 호출
     */
    public void resetNightStatus() {
        this.targetedByMafiaAttack = false;
        this.healedByDoctor = false;
        // (추가) 다른 밤 관련 임시 상태도 여기서 초기화
    }


    // --- 직업 능력 관련 위임 메서드 ---
    /**
     * 플레이어가 자신의 직업 능력을 밤에 수행하도록 요청
     * 실제 로직은 Job 객체에 위임
     * 
     * 이 메서드는 GameManager가 canUseAbility()를 이미 확인하고 호출하는 것을 전제로 함
     * 
     * @param gameManager 게임 매니저 객체
     */ 
    public void performNightAction(GameManager gameManager) {
    	// GameManager에서 isAlive, hasNightAbility, canUseAbility 체크
        // 바로 Job의 메서드를 호출
        if (this.job != null) {
            job.performNightAction(this, gameManager.getLivingPlayers(), gameManager);
        }
    }

    /**
     * (비공개 확인 페이즈용) 이 플레이어에게 전달될 개인적인 밤 결과 메시지 반환
     * 
     * @param resultInfo GameManager로부터 전달받은 결과 정보 객체
     * @param gameManager 게임 매니저 객체
     * @return 개인 결과 메시지 문자열
     */
    public String getPrivateNightResultMessage(Object resultInfo, GameManager gameManager) {
        if (job != null) {
            // Job에게 결과 정보와 함께 메시지 생성을 위임
            return job.getPrivateNightResultMessage(this, resultInfo, gameManager);
        }
        return null;
    }


    // --- 유틸리티 메서드 ---
    @Override
    public String toString() {
    	String jobNameStr = (job != null) ? job.getJobName() : "미정";
        String teamStr = (currentTeam != null) ? currentTeam.name() : "미정";
        return String.format("%d. %s (%s, 팀: %s, 직업: %s)",
                playerNumber, name, (isAlive ? "생존" : "사망"), teamStr, jobNameStr);
    }
    
    public int compareTo(Player other) {
    	return Integer.compare(this.getPlayerNumber(), other.getPlayerNumber());
    }

    // equals()와 hashCode()는 플레이어 객체를 Set이나 Map의 키로 사용할 경우 사용
    // 고유 번호(playerNumber)를 기준으로 구현
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return playerNumber == player.playerNumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(playerNumber);
    }
}