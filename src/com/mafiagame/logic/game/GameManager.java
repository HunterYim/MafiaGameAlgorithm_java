package com.mafiagame.logic.game;

// enums 임포트
import com.mafiagame.logic.common.enums.*;

// job 패키지 임포트
import com.mafiagame.logic.job.*;

// 자바 라이브러리 임포트
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 마피아 게임의 전체 진행을 관리하는 클래스
 * 게임 설정, 턴 관리, 능력 결과 처리, 승리 조건 판정 등을 담당
 */
public class GameManager {

	private List<Player> players;	// 전체 플레이어 리스트
	private int playerCount;		// 전체 플레이어 숫자
	private GameMode gameMode;		// 게임 모드
	private GamePhase currentPhase;	// 현재 페이즈
	private int dayCount;			// 현재 날짜
	private boolean isGameOver;		// 게임 종료 여부
	private Team winningTeam;		// 승리 팀
	private int currentPlayerIndex; // 현재 턴을 진행 중인 플레이어의 인덱스 (텍스트용)

	private Scanner scanner;		// 자바 스캐너 (텍스트용)
	private Random random;			// 직업 랜덤 배정 등에 사용

	// 게임 상태 기록 Maps & Lists
	
	// 밤 능력 사용 시 선택 대상 기록 (능력 사용자, 대상 플레이어)
	// Player 객체 자체를 키로 사용하기 위해 Player 클래스의 equals()와 hashCode() 사용
	private Map<Player, Player> nightAbilityTargets;
	
	// 밤 동안의 능력 사용 결과를 저장 (개인 결과 확인 페이즈에서 사용)
	// Key: 결과를 확인하려는 Player, Value: 해당 Player와 관련된 밤 행동 결과 정보 (Map 또는 사용자 정의 객체)
	private Map<Player, Object> nightResultsForPrivateConfirmation;
	
	// 투표 기록 (투표자, 투표 대상)
	private Map<Player, Player> voteRecords;
	
	// 다음 날 낮에 공개될 이벤트 로그
	private List<String> publicAnnouncements;
	
	// 건달에게 협박당한 플레이어 목록 (다음 날 투표 금지)
	private List<Player> intimidatedPlayers;
	
	// 오늘 낮에 추방된 플레이어 목록 (테러리스트 등 처리용)
	private List<Player> executedPlayersToday;

	public GameManager() {
		this.players = new ArrayList<>();
		this.nightResultsForPrivateConfirmation = new HashMap<>();
		this.nightAbilityTargets = new HashMap<>();
		this.voteRecords = new HashMap<>();
		this.publicAnnouncements = new ArrayList<>();
		this.intimidatedPlayers = new ArrayList<>();
		this.executedPlayersToday = new ArrayList<>();
		this.dayCount = 1; // 1일차부터 시작
		this.isGameOver = false;
		this.currentPlayerIndex = 0; // 첫 번째 플레이어부터 시작

		this.scanner = new Scanner(System.in);
		this.random = new Random();
	}

	/**
	 * 게임 설정, 플레이어 생성, 직업 배정
	 * 
	 * @param playerCount 총 플레이어 수
	 * @param gameMode    게임 모드 (CLASSIC, SPY)
	 */
	public void setupGame(int playerCount, GameMode gameMode) {
		this.playerCount = playerCount;
		this.gameMode = gameMode;
		this.players.clear(); // 기존 플레이어 정보 초기화

		// 1. 플레이어 객체 생성 (이름은 "플레이어 1", "플레이어 2" 등으로 초기 설정)
		for (int i = 0; i < playerCount; i++) {
			// TODO 플레이어 이름 입력받는 로직 필요
			this.players.add(new Player("플레이어 " + (i + 1), i + 1, null));
		}

		// 2. 직업 목록 생성 및 배정
		assignJobs();

		// 3. 직업 배정 후 각 플레이어에게 onAssigned 호출 (예: 마피아 동료 인지)
		for (Player player : this.players) {
			if (player.getJob() != null) {
				player.getJob().onAssigned(player, this);
			}
		}

		System.out.println("게임 설정이 완료되었습니다. 총 " + playerCount + "명의 플레이어, 모드: " + gameMode);
		for (Player p : players) {
			System.out.println(p.toString()); // 배정된 직업 확인용 (디버깅)
		}
	}

	/**
	 * 게임 모드와 인원수에 따라 직업 배정
	 */
	private void assignJobs() {
		List<Job> jobsToAssign = new ArrayList<>();

		// 1. 게임 모드와 플레이어 수에 따라 배정할 직업 목록 생성
		switch (this.gameMode) {
		case CLASSIC:
			jobsToAssign = getClassicModeJobs(this.playerCount);
			break;
		case SPY:
			jobsToAssign = getSpyModeJobs(this.playerCount);
			break;
		default:
			System.err.println("오류: 알 수 없는 게임 모드입니다 - " + this.gameMode);
			return;
		}

		// 생성된 직업 수가 플레이어 수와 맞는지 최종 확인
		if (jobsToAssign.size() != this.playerCount) {
			System.err
					.println("오류: 생성된 직업 수(" + jobsToAssign.size() + ")가 플레이어 수(" + this.playerCount + ")와 일치하지 않습니다.");
			// TODO 이 경우 게임 진행이 불가능하므로 적절한 처리 필요
			return;
		}

		// 2. 생성된 직업 목록을 무작위로 섞음
		Collections.shuffle(jobsToAssign, this.random);

		// 3. 섞인 직업을 플레이어에게 순서대로 할당
		for (int i = 0; i < this.players.size(); i++) {
			if (i < jobsToAssign.size()) {
				this.players.get(i).setJob(jobsToAssign.get(i));
			}
		}
		System.out.println("모든 플레이어에게 직업이 성공적으로 배정되었습니다.");
	}

	/**
	 * 클래식 모드의 인원수에 따른 직업 목록 반환
	 * 
	 * @param playerCount 플레이어 수
	 * @return 해당 인원수에 맞는 Job 객체 리스트
	 */
	private List<Job> getClassicModeJobs(int playerCount) {
		List<Job> jobs = new ArrayList<>();
		int mafiaCount = 0;
		int supporterCount = 0; // 보조 직업 (정보원 또는 늑대인간)
		int specialCitizenCount = 0;
		int normalCitizenCount = 0;

		// 룰 3.1.(3) 플레이어 숫자에 따른 직업 목록
		switch (playerCount) {
		case 4:
			mafiaCount = 1;
			specialCitizenCount = 1;
			break;
		case 5:
			mafiaCount = 1;
			specialCitizenCount = 2;
			break;
		case 6:
			mafiaCount = 1;
			supporterCount = 1;
			specialCitizenCount = 2;
			break;
		case 7:
			mafiaCount = 1;
			supporterCount = 1;
			specialCitizenCount = 3;
			break;
		case 8:
			mafiaCount = 2;
			supporterCount = 1;
			specialCitizenCount = 3;
			break;
		case 9:
			mafiaCount = 2;
			supporterCount = 1;
			specialCitizenCount = 4;
			break;
		case 10:
			mafiaCount = 2;
			supporterCount = 1;
			specialCitizenCount = 4;
			normalCitizenCount = 1;
			break;
		case 11:
			mafiaCount = 3;
			supporterCount = 1;
			specialCitizenCount = 5;
			break;
		case 12:
			mafiaCount = 3;
			supporterCount = 1;
			specialCitizenCount = 5;
			normalCitizenCount = 1;
			break;
		default:
			System.err.println("오류 (Classic): 지원하지 않는 플레이어 수입니다 - " + playerCount);
			return jobs; // 빈 리스트 반환
		}

		// 필수 직업 추가 (경찰, 의사 - 각 1명)
		jobs.add(new Police());
		jobs.add(new Doctor());

		// 마피아 추가
		for (int i = 0; i < mafiaCount; i++) {
			jobs.add(new Mafia());
		}

		// 보조 직업 추가 (정보원 또는 늑대인간 중 랜덤 1명)
		if (supporterCount > 0) {
			if (this.random.nextBoolean()) {
				jobs.add(new Informant());
			} else {
				jobs.add(new Warewolf());
			}
		}

		// 특수 시민 직업 추가 (랜덤, 중복 없이)
		List<Job> availableSpecialJobs = getAllSpecialCitizenJobs();
		Collections.shuffle(availableSpecialJobs, this.random);
		for (int i = 0; i < specialCitizenCount && i < availableSpecialJobs.size(); i++) {
			jobs.add(availableSpecialJobs.get(i));
		}

		// 일반 시민 추가
		for (int i = 0; i < normalCitizenCount; i++) {
			jobs.add(new Citizen());
		}

		return jobs;
	}

	/**
	 * 간첩 모드의 인원수에 따른 직업 목록 반환
	 * 
	 * @param playerCount 플레이어 수
	 * @return 해당 인원수에 맞는 Job 객체 리스트
	 */
	private List<Job> getSpyModeJobs(int playerCount) {
		List<Job> jobs = new ArrayList<>();
		int mafiaCount = 0;
		int supporterCount = 0;
		int specialCitizenCount = 0;
		int normalCitizenCount = 0;
		int spyCount = 0; // 간첩

		// 룰 3.2.(3) 플레이어 숫자에 따른 직업 목록
		switch (playerCount) {
		case 9:
			mafiaCount = 2;
			supporterCount = 1;
			specialCitizenCount = 3;
			spyCount = 1;
			break;
		case 10:
			mafiaCount = 2;
			supporterCount = 1;
			specialCitizenCount = 4;
			spyCount = 1;
			break;
		case 11:
			mafiaCount = 3;
			supporterCount = 1;
			specialCitizenCount = 4;
			spyCount = 1;
			break;
		case 12:
			mafiaCount = 3;
			supporterCount = 1;
			specialCitizenCount = 4;
			spyCount = 1;
			normalCitizenCount = 1;
			break;
		default:
			System.err.println("오류 (Spy): 지원하지 않는 플레이어 수입니다 - " + playerCount);
			return jobs; // 빈 리스트 반환
		}

		// 필수 직업 추가 (경찰, 의사 - 각 1명)
		jobs.add(new Police());
		jobs.add(new Doctor());

		// 마피아 추가
		for (int i = 0; i < mafiaCount; i++) {
			jobs.add(new Mafia());
		}

		// 보조 직업 추가
		if (supporterCount > 0) {
			if (this.random.nextBoolean()) {
				jobs.add(new Informant());
			} else {
				jobs.add(new Warewolf());
			}
		}

		// 특수 시민 직업 추가
		List<Job> availableSpecialJobs = getAllSpecialCitizenJobs();
		Collections.shuffle(availableSpecialJobs, this.random);
		for (int i = 0; i < specialCitizenCount && i < availableSpecialJobs.size(); i++) {
			jobs.add(availableSpecialJobs.get(i));
		}

		// 일반 시민 추가
		for (int i = 0; i < normalCitizenCount; i++) {
			jobs.add(new Citizen());
		}

		// 간첩 추가
		for (int i = 0; i < spyCount; i++) {
			jobs.add(new Spy());
		}

		return jobs;
	}

	/**
	 * 선택 가능한 모든 특수 시민 직업 객체 리스트 반환
	 * 
	 * @return 특수 시민 Job 객체 리스트
	 */
	private List<Job> getAllSpecialCitizenJobs() {
		List<Job> specialJobs = new ArrayList<>();
		specialJobs.add(new Soldier());
		specialJobs.add(new Politician());
		specialJobs.add(new Undertaker());
		specialJobs.add(new Gangster());
		specialJobs.add(new Reporter());
		specialJobs.add(new Detective());
		specialJobs.add(new GraveRobber());
		specialJobs.add(new Terrorist());
		// (추가) 새로운 특수 직업 추가 시 여기에 추가
		return specialJobs;
	}

	/**
	 * 게임을 시작하고 메인 루프 실행
	 */
	public void startGame() {
		if (players.isEmpty()) {
			System.err.println("오류: 게임이 설정되지 않았습니다. setupGame()을 먼저 호출해주세요.");
			return;
		}
		System.out.println("\n마피아 게임을 시작합니다!");
		this.currentPhase = GamePhase.NIGHT_JOB_CONFIRM_ABILITY;
		this.isGameOver = false;

		while (!isGameOver) {
			System.out.println("\n--- " + (dayCount + 1) + "일차 " + getPhaseName(currentPhase) + " 시작 ---");
			switch (currentPhase) {
			case NIGHT_JOB_CONFIRM_ABILITY:
				processNightJobConfirmAbilityPhase();
				proceedToNextPhase();
				break;
			case NIGHT_ABILITY_USE:
				processNightAbilityUsePhase();
				proceedToNextPhase();
				break;
			case NIGHT_PRIVATE_CONFIRM:
				processNightPrivateConfirmPhase();
				proceedToNextPhase();
				break;
			case DAY_PUBLIC_ANNOUNCEMENT:
				processDayPublicAnnouncementPhase();
				if (checkWinConditions())
					break; // 승리 조건 확인
				proceedToNextPhase();
				break;
			case DAY_DISCUSSION:
				processDayDiscussionPhase();
				proceedToNextPhase();
				break;
			case DAY_VOTE:
				processDayVotePhase();
				proceedToNextPhase();
				break;
			case DAY_EXECUTION:
				processDayExecutionPhase();
				if (checkWinConditions())
					break; // 승리 조건 확인
				proceedToNextPhase(); // 다음 페이즈 NIGHT_ABILITY_USE (새로운 밤)
				break;
			case GAME_OVER:
				announceWinner();
				isGameOver = true; // 루프 종료
				break;
			default:
				System.out.println("알 수 없는 게임 단계입니다. 게임을 종료합니다.");
				isGameOver = true; // 예외 상황 시 종료
				break;
			}

			// 간단한 딜레이 (텍스트 게임 가독성)
			try {
				Thread.sleep(500); // 0.5초 딜레이
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		System.out.println("\n마피아 게임이 종료되었습니다.");
		scanner.close();
	}

	// TODO 초기 페이즈 순서 수정 필요 -> 직업 확인 후 능력 바로 사용
	
	/**
	 * 다음 게임 단계로 진행 (페이즈 순서 관리)
	 * 
	 * 현재 페이즈의 모든 작업이 완료 후 호출
	 */
	private void proceedToNextPhase() {
		if (isGameOver) {
			this.currentPhase = GamePhase.GAME_OVER;
			return;
		}

		GamePhase nextPhase;
		
		switch (currentPhase) {
		case NIGHT_JOB_CONFIRM_ABILITY: // 첫날 밤: 직업 확인 및 즉시 능력 사용 후
		case NIGHT_ABILITY_USE: // (일반) 밤: 능력 사용 후
			nextPhase = GamePhase.NIGHT_PRIVATE_CONFIRM;
			break;
		case NIGHT_PRIVATE_CONFIRM: // 밤: 개인 결과 확인 후
			nextPhase = GamePhase.DAY_PUBLIC_ANNOUNCEMENT;
			break;
		case DAY_PUBLIC_ANNOUNCEMENT: // 낮: 공개 결과 발표 후
			nextPhase = GamePhase.DAY_DISCUSSION;
			break;
		case DAY_DISCUSSION: // 낮: 토론 후
			nextPhase = GamePhase.DAY_VOTE;
			break;
		case DAY_VOTE: // 낮: 투표 후
			nextPhase = GamePhase.DAY_EXECUTION;
			break;
		case DAY_EXECUTION: // 낮: 처형 후
			nextPhase = GamePhase.NIGHT_ABILITY_USE; // 새로운 밤 시작 (능력 사용 페이즈)
			dayCount++; // 새로운 날 시작
			resetPlayersNightStatus(); // 플레이어 밤 상태 및 투표 가능 여부 초기화
			this.intimidatedPlayers.clear(); // 건달 협박 목록 초기화 (다음 날을 위해)
			this.executedPlayersToday.clear(); // 이전 낮 추방자 목록 초기화
			break;
		case GAME_OVER:
			// 이미 게임 종료 상태이므로 변경 없음
			return; // 여기서 바로 종료
		default:
			System.err.println("오류: 알 수 없는 현재 페이즈입니다 - " + currentPhase + ". 게임을 종료합니다.");
			this.isGameOver = true;
			nextPhase = GamePhase.GAME_OVER;
			break;
		}
		this.currentPhase = nextPhase;
	}

	/**
	 * 첫날 밤: 직업 확인 및 즉시 능력 사용 페이즈
	 */
	private void processNightJobConfirmAbilityPhase() {
		System.out.println("첫날 밤입니다. 각자 직업을 확인하고 직업 확인 후 바로 개인 능력을 사용합니다.");
		nightAbilityTargets.clear();

		for (int i = 0; i < players.size(); i++) {
			currentPlayerIndex = i;
			Player currentPlayer = players.get(currentPlayerIndex);

			if (!currentPlayer.isAlive()) continue; // 죽은 플레이어 통과

			// 1. 직업 확인 메시지
			displayMessageToPlayer(currentPlayer, "당신의 차례입니다. 화면을 확인하세요.");
			Job currentJob = currentPlayer.getJob();
			displayMessageToPlayer(currentPlayer, "당신의 직업은 [" + currentJob.getJobName() + "] 입니다.");

			// 마피아 동료 확인은 Job의 onAssigned에서 처리되도록 위임
            // GameManager는 setupGame에서 onAssigned를 호출
			
			handlePlayerNightActionTurn(currentPlayer);

            getPlayerInput(currentPlayer, "확인 후 Enter 키를 누르고 다음 사람에게 넘기세요.");
            clearConsole();
        }
        applyNightActionsAndResults();
    }
	
	/**
	 * (일반) 밤 능력 사용 페이즈
	 */
	private void processNightAbilityUsePhase() {
		System.out.println("밤입니다. 능력을 사용할 플레이어는 차례대로 진행합니다.");
		nightAbilityTargets.clear(); // 밤 능력 대상 기록 초기화

		for (int i = 0; i < players.size(); i++) {
			currentPlayerIndex = i;
			Player currentPlayer = players.get(currentPlayerIndex);

			if (!currentPlayer.isAlive()) continue; // 죽은 플레이어 통과

			handlePlayerNightActionTurn(currentPlayer);

            getPlayerInput(currentPlayer, "선택이 완료되었습니다. Enter 키를 누르고 다음 사람에게 넘기세요.");
            clearConsole();
        }
        applyNightActionsAndResults();
    }
	
	/**
     * (헬퍼) 플레이어 한 명의 밤 능력 사용 턴을 처리하는 중복 로직
     * 
     * @param currentPlayer 현재 턴의 플레이어
     */
    private void handlePlayerNightActionTurn(Player currentPlayer) {
        if (currentPlayer.getJob().hasNightAbility()) {
            if (currentPlayer.getJob().canUseAbility(currentPlayer, this.dayCount, this)) {
                displayMessageToPlayer(currentPlayer, currentPlayer.getNightActionPrompt());
                currentPlayer.performNightAction(this);
            } else {
                String cantUseMessage = currentPlayer.getName() + "님은 이번 밤에 능력을 사용할 수 없습니다.";
                if (currentPlayer.getJob().isOneTimeAbility() && currentPlayer.getJob().hasUsedOneTimeAbility()) {
                    cantUseMessage += " (이미 사용)";
                } else if (currentPlayer.getJob() instanceof Reporter && this.dayCount == 1) {
                    cantUseMessage += " (엠바고)";
                }
                displayMessageToPlayer(currentPlayer, cantUseMessage);
            }
        } else {
            displayMessageToPlayer(currentPlayer, "사용할 수 있는 밤 능력이 없습니다.");
        }
    }
	
	/**
     * 밤 동안 사용된 능력들의 결과를 종합하고 플레이어 상태 업데이트
     * 각 기능별 private 메서드를 순서대로 호출하여 전체 흐름 제어
     */
    private void applyNightActionsAndResults() {
        // 0. 필요한 정보 저장을 위한 임시 컨테이너 생성
        NightActionContext context = new NightActionContext();

        // 1. 밤 능력 사용 기록 분석 및 컨텍스트 준비
        analyzeNightAbilityRecords(context);

        // 2. 방어/보호 능력 우선 적용 (예: 군인)
        handleDefensiveAbilities(context);

        // 3. 공격 능력 처리 및 사망자 결정 (마피아, 늑대인간)
        handleAttackAbilities(context);

        // 4. 기타 능력 결과 적용 (건달, 간첩, 기자, 장의사, 도굴꾼 등)
        handleOtherAbilities(context);

        // 밤 능력 대상 기록 초기화
        nightAbilityTargets.clear();
    }
	
	/**
     * 밤 동안의 행동 결과를 담는 내부 헬퍼 클래스
     * 각 처리 메서드 간에 정보를 전달하기 위해 사용
     */
    private static class NightActionContext {
        Map<Player, Boolean> isHealedMap = new HashMap<>();
        Map<Player, Boolean> isProtectedBySoldierMap = new HashMap<>();
        Player mafiaAttackTarget = null;
        Player warewolfAttackTarget = null;
        Map<Player, Player> gangsterTargets = new HashMap<>();
        Map<Player, Player> spyRecruitTargets = new HashMap<>();
        Player reporterTarget = null;
        String reporterOriginalJob = null;
        Player undertakerTarget = null; // 장의사 대상은 아직 분석 로직에 없었지만 추가
        String undertakerOriginalJob = null; // 장의사 결과 저장용
        List<Player> diedThisNight = new ArrayList<>();
    }
    
    /**
     * 1. nightAbilityTargets 맵을 분석하여 밤 동안의 능력 사용 기록을 NightActionContext 객체에 정리
     * 
     * @param context 밤 행동 결과를 저장하고 전달할 컨텍스트 객체
     */
    private void analyzeNightAbilityRecords(NightActionContext context) {
        // nightAbilityTargets에는 (능력 사용자, 능력 대상)이 기록되어 있음.
        for (Map.Entry<Player, Player> entry : nightAbilityTargets.entrySet()) {
            Player user = entry.getKey();
            Player target = entry.getValue();

            if (user.getJob() instanceof Doctor) {
                context.isHealedMap.put(target, true);
            } else if (user.getJob() instanceof Mafia && ((Mafia) user.getJob()).isAttackCommander(user, this)) {
                context.mafiaAttackTarget = target;
            } else if (user.getJob() instanceof Warewolf && ((Warewolf) user.getJob()).canUseMassacre(this)) {
                context.warewolfAttackTarget = target;
            } else if (user.getJob() instanceof Gangster) {
                context.gangsterTargets.put(user, target);
            } else if (user.getJob() instanceof Spy && user.getJob().canUseAbility(user, dayCount, this)) {
                context.spyRecruitTargets.put(user, target);
            } else if (user.getJob() instanceof Reporter && user.getJob().canUseAbility(user, dayCount, this)) {
                context.reporterTarget = target;
                if (context.reporterTarget != null) context.reporterOriginalJob = context.reporterTarget.getJob().getJobName();
            }
            // TODO ... 장의사, 탐정 등 기타 다른 직업들의 능력 사용 기록 분석 추가 ...
        }
    }
    
    /**
     * 2. 방어/보호 능력을 처리합니다. (예: 군인)
     * @param context 밤 행동 결과가 담긴 컨텍스트 객체
     */
    private void handleDefensiveAbilities(NightActionContext context) {
        // 마피아 공격 대상이 군인이고, 군인이 방어 능력을 사용하지 않았다면 방어 발동.
        if (context.mafiaAttackTarget != null && context.mafiaAttackTarget.getJob() instanceof Soldier) {
            Soldier soldierJob = (Soldier) context.mafiaAttackTarget.getJob();
            if (soldierJob.tryActivateDefense(context.mafiaAttackTarget)) { // Soldier 클래스에 이런 메서드가 있다고 가정
                context.isProtectedBySoldierMap.put(context.mafiaAttackTarget, true);
                publicAnnouncements.add(context.mafiaAttackTarget.getName() + "님이 마피아의 공격을 받았으나, 군인의 방어 능력으로 막아냈습니다! 직업은 [군인]입니다.");
            }
        }
        // 늑대인간 공격에 대한 군인 방어는 handleAttackAbilities 에서 직접 처리 (공격 종류에 따라 다를 수 있으므로)
    }

    /**
     * 3. 공격 능력(마피아, 늑대인간)을 처리하고 사망자를 결정합니다.
     * @param context 밤 행동 결과가 담긴 컨텍스트 객체
     */
    private void handleAttackAbilities(NightActionContext context) {
        // 3.1. 마피아 공격 처리
        if (context.mafiaAttackTarget != null && context.mafiaAttackTarget.isAlive()) {
            boolean saved = false;
            if (context.isHealedMap.getOrDefault(context.mafiaAttackTarget, false)) {
                publicAnnouncements.add(context.mafiaAttackTarget.getName() + "님이 마피아의 공격을 받았지만, 의사의 치료로 생존했습니다!");
                recordPrivateNightResult(getDoctorPlayer(), context.mafiaAttackTarget.getName() + "님을 성공적으로 치료했습니다.");
                saved = true;
            } else if (context.isProtectedBySoldierMap.getOrDefault(context.mafiaAttackTarget, false)) {
                saved = true; // 군인 방어 메시지는 handleDefensiveAbilities에서 이미 추가됨
            }

            if (!saved) {
                context.mafiaAttackTarget.die();
                context.diedThisNight.add(context.mafiaAttackTarget);
                publicAnnouncements.add(context.mafiaAttackTarget.getName() + "님이 밤 사이 마피아의 공격으로 사망했습니다.");
                if (getDoctorPlayer() != null && nightAbilityTargets.get(getDoctorPlayer()) == context.mafiaAttackTarget) {
                     recordPrivateNightResult(getDoctorPlayer(), context.mafiaAttackTarget.getName() + "님을 치료하려 했으나, 이미 사망했습니다.");
                }
            }
        } else if (context.mafiaAttackTarget == null && getDoctorPlayer() != null && nightAbilityTargets.containsKey(getDoctorPlayer())) {
            Player healedTargetByDoctor = nightAbilityTargets.get(getDoctorPlayer());
            if (healedTargetByDoctor != null) recordPrivateNightResult(getDoctorPlayer(), healedTargetByDoctor.getName() + "님은 공격받지 않았습니다.");
        }

        // 3.2. 늑대인간 살육 처리 (치료 무시)
        if (context.warewolfAttackTarget != null && context.warewolfAttackTarget.isAlive()) {
            boolean savedBySoldier = false;
            if (context.warewolfAttackTarget.getJob() instanceof Soldier) {
                Soldier soldierJob = (Soldier) context.warewolfAttackTarget.getJob();
                // isProtectedBySoldierMap을 체크하여 군인 방어가 이미 마피아 공격에 사용되었는지 확인 가능
                if (!context.isProtectedBySoldierMap.containsKey(context.warewolfAttackTarget) && soldierJob.tryActivateDefense(context.warewolfAttackTarget)) {
                    context.isProtectedBySoldierMap.put(context.warewolfAttackTarget, true);
                    publicAnnouncements.add(context.warewolfAttackTarget.getName() + "님이 늑대인간의 공격을 받았으나, 군인의 방어 능력으로 막아냈습니다! 직업은 [군인]입니다.");
                    savedBySoldier = true;
                }
            }

            if (!savedBySoldier) {
                context.warewolfAttackTarget.die();
                context.diedThisNight.add(context.warewolfAttackTarget);
                publicAnnouncements.add(context.warewolfAttackTarget.getName() + "님이 밤 사이 늑대인간의 공격으로 사망했습니다. (치료 불가)");
            }
        }
    }

    /**
     * 4. 건달, 간첩, 기자, 장의사, 도굴꾼 등 기타 능력들을 처리합니다.
     * @param context 밤 행동 결과가 담긴 컨텍스트 객체
     */
    private void handleOtherAbilities(NightActionContext context) {
        // 4.1. 건달 협박 적용
        for (Map.Entry<Player, Player> entry : context.gangsterTargets.entrySet()) {
            Player gangsterUser = entry.getKey();
            Player intimidatedTarget = entry.getValue();
            if (intimidatedTarget != null && intimidatedTarget.isAlive()) {
                intimidatedTarget.setCanVoteToday(false);
                recordPrivateNightResult(gangsterUser, intimidatedTarget.getName() + "님을 협박했습니다.");
                recordPrivateNightResult(intimidatedTarget, "당신은 건달에게 협박당해 오늘 투표할 수 없습니다.");
            }
        }

        // 4.2. 간첩 포섭 처리
        for (Map.Entry<Player, Player> entry : context.spyRecruitTargets.entrySet()) {
            Player spy = entry.getKey();
            Player targetToRecruit = entry.getValue();

            if (targetToRecruit != null && targetToRecruit.isAlive()) {
                boolean recruitSuccess = false;
                if (!(targetToRecruit.getJob() instanceof Mafia) && !(targetToRecruit.getJob() instanceof Soldier)) {
                    if (targetToRecruit.getCurrentTeam() != Team.SPY) {
                        targetToRecruit.setCurrentTeam(Team.SPY);
                        recruitSuccess = true;
                    }
                }

                String recruitResultMessageForSpy, recruitResultMessageForTarget = null, publicRecruitAnnouncement;
                if (recruitSuccess) {
                    recruitResultMessageForSpy = targetToRecruit.getName() + "님(" + targetToRecruit.getJob().getJobName() + ")을 성공적으로 포섭했습니다.";
                    recruitResultMessageForTarget = "당신은 간첩에게 포섭되었습니다. 이제부터 간첩 팀 소속입니다. 당신을 포섭한 간첩은 " + spy.getName() + " 입니다.";
                    publicRecruitAnnouncement = "간첩이 포섭에 성공했습니다.";
                } else {
                    recruitResultMessageForSpy = targetToRecruit.getName() + "님(" + targetToRecruit.getJob().getJobName() + ") 포섭에 실패했습니다.";
                    publicRecruitAnnouncement = "간첩이 포섭에 실패했습니다.";
                }
                recordPrivateNightResult(spy, recruitResultMessageForSpy);
                if (recruitSuccess && recruitResultMessageForTarget != null) recordPrivateNightResult(targetToRecruit, recruitResultMessageForTarget);
                publicAnnouncements.add(publicRecruitAnnouncement);
            }
        }

        // 4.3. 정보 수집 능력은 개인 결과 확인 페이즈에서 처리되므로, 여기서는 별도 처리 불필요.

        // 4.4. 접선 처리도 Job의 performNightAction에서 recordPrivateNightResult로 처리.

        // 4.5. 기자 취재 결과 (낮에 공개될 내용 준비)
        if (context.reporterTarget != null && context.reporterOriginalJob != null) {
            Player reporterPlayer = getPlayerByJob(Reporter.class);
            if (reporterPlayer != null && reporterPlayer.isAlive()) {
                 publicAnnouncements.add("기자의 취재 결과, " + context.reporterTarget.getName() + "님의 직업은 [" + context.reporterOriginalJob + "] 입니다.");
            } else {
                 publicAnnouncements.add("기자가 취재를 시도했으나, 밤 사이 사망하여 취재 결과가 무효화되었습니다.");
            }
        }
        // 장의사 부검도 유사하게 처리

        // 5. 도굴꾼 능력 처리 (첫날 밤)
        if (dayCount == 0 && !context.diedThisNight.isEmpty()) {
            Player graveRobber = getPlayerByJob(GraveRobber.class);
            if (graveRobber != null && graveRobber.isAlive()) {
                Player firstDeadByAttack = context.diedThisNight.stream()
                        .filter(p -> nightAbilityTargets.containsValue(p))
                        .findFirst().orElse(null);

                if (firstDeadByAttack != null) {
                    Job stolenJob = firstDeadByAttack.getJob();
                    graveRobber.setJob(stolenJob);
                    recordPrivateNightResult(graveRobber, "당신은 " + firstDeadByAttack.getName() + "님의 직업 [" + stolenJob.getJobName() + "]을 도굴했습니다.");
                    publicAnnouncements.add("도굴꾼이 밤 사이 누군가의 직업을 도굴한 것 같습니다...");
                }
            }
        }
    }
    
	/**
	 * 밤 개인 결과 확인 페이즈
	 */
	private void processNightPrivateConfirmPhase() {
		System.out.println("밤 동안의 개인 결과를 확인합니다.");
		for (int i = 0; i < players.size(); i++) {
			currentPlayerIndex = i;
			Player currentPlayer = players.get(currentPlayerIndex);

			if (!currentPlayer.isAlive())
				continue;

			displayMessageToPlayer(currentPlayer, "당신의 차례입니다. 화면을 확인하세요.");

			// GameManager에 저장된 해당 플레이어의 밤 결과 정보를 가져옴
			Object resultInfo = nightResultsForPrivateConfirmation.get(currentPlayer);

			// Job 객체를 통해 개인 결과 메시지 가져오기
			// 이제 getPrivateNightResultMessage에 결과 정보를 전달
			// String privateMessage = currentPlayer.getPrivateNightResultMessage(null,
			// this); // 아직 target 전달 안됨. target도 결과 정보에 포함되거나 다른 방식으로 전달 필요
			// **더 나은 호출 방식:** (위는 이전 버전)
			String privateMessage = currentPlayer.getJob().getPrivateNightResultMessage(currentPlayer, resultInfo,
					this); // Job에서 직접 메시지 생성

			if (privateMessage != null && !privateMessage.isEmpty()) {
				displayMessageToPlayer(currentPlayer, privateMessage);
			} else {
				displayMessageToPlayer(currentPlayer, "특별한 개인 결과가 없습니다.");
			}

			getPlayerInput(currentPlayer, "확인 후 Enter 키를 누르고 다음 사람에게 넘기세요.");
			clearConsole();
		}
		nightResultsForPrivateConfirmation.clear(); // 확인 후 초기화

	}

	/**
	 * 낮 공개 결과 발표 페이즈
	 */
	private void processDayPublicAnnouncementPhase() {
		System.out.println("낮이 밝았습니다. 밤 동안의 공개 결과입니다.");
		if (publicAnnouncements.isEmpty()) {
			System.out.println("밤 사이 아무 일도 일어나지 않았습니다.");
		} else {
			for (String announcement : publicAnnouncements) {
				System.out.println(announcement);
			}
		}
		publicAnnouncements.clear(); // 발표 후 초기화
		executedPlayersToday.clear(); // 새 날이므로 초기화
	}

	/**
	 * 낮 토론 페이즈
	 */
	private void processDayDiscussionPhase() {
		System.out.println("토론 시간입니다. 자유롭게 토론하세요.");
		// 텍스트 기반에서는 실제 토론은 플레이어들이 하고, 앱은 시간 제한 정도만 둘 수 있음
		// 여기서는 간단히 메시지만 출력하고 넘어감
		getPlayerInput(null, "토론이 끝나면 Enter 키를 누르세요."); // 대표로 한명만 입력받는 방식
	}

	/**
	 * 낮 투표 페이즈
	 */
	private void processDayVotePhase() {
		System.out.println("투표 시간입니다. 처형할 사람을 지목해주세요.");
		voteRecords.clear(); // 투표 기록 초기화

		List<Player> livingVoters = getLivingPlayers();
		for (Player voter : livingVoters) {
			if (!voter.canVoteToday()) {
				displayMessageToPlayer(voter, "당신은 오늘 투표할 수 없습니다 (건달 협박).");
				getPlayerInput(voter, "확인 후 Enter 키를 누르고 넘기세요.");
				clearConsole();
				continue;
			}

			currentPlayerIndex = players.indexOf(voter); // 현재 투표자 인덱스 설정
			displayMessageToPlayer(voter, voter.getName() + "님, 투표할 대상을 선택하세요.");
			List<Player> voteTargets = getLivingPlayers();

			for (int i = 0; i < voteTargets.size(); i++) {
				displayMessageToPlayer(voter, (i + 1) + ". " + voteTargets.get(i).getName());
			}

			int choice = -1;
			while (true) {
				String input = getPlayerInput(voter, "번호를 입력하세요: ");
				try {
					choice = Integer.parseInt(input) - 1;
					if (choice >= 0 && choice < voteTargets.size()) {
						break;
					} else {
						displayMessageToPlayer(voter, "잘못된 번호입니다. 다시 입력하세요.");
					}
				} catch (NumberFormatException e) {
					displayMessageToPlayer(voter, "숫자로 입력해주세요.");
				}
			}
			Player votedPlayer = voteTargets.get(choice);
			voteRecords.put(voter, votedPlayer);
			displayMessageToPlayer(voter, votedPlayer.getName() + "님에게 투표했습니다.");

			getPlayerInput(voter, "투표 완료. Enter 키를 누르고 넘기세요.");
			clearConsole();
		}
	}

	/**
	 * 낮 추방 결과 처리 페이즈
	 */
	private void processDayExecutionPhase() {
		if (voteRecords.isEmpty()) {
			System.out.println("투표가 진행되지 않았습니다.");
			return;
		}

		// 투표 결과 집계
		Map<Player, Integer> voteCounts = new HashMap<>();
		
		voteCounts.clear(); // 다시 계산
		int maxVotes = 0;
		
		for (Player targetPlayer : getLivingPlayers()) { // 모든 살아있는 플레이어를 대상으로 득표수 계산
			int currentVotesForTarget = 0;
			for (Map.Entry<Player, Player> entry : voteRecords.entrySet()) {
				Player voter = entry.getKey();
				Player votedTarget = entry.getValue();
				if (votedTarget.equals(targetPlayer)) { // 투표 대상이 현재 정확한지 확인
					if (voter.getJob() instanceof com.mafiagame.logic.job.Politician) { // 정치인 논객 능력 고려 (Politician 클래스
																						// 직접 참조)
						currentVotesForTarget += 2;
					} else {
						currentVotesForTarget += 1;
					}
				}
			}
			voteCounts.put(targetPlayer, currentVotesForTarget);
			if (currentVotesForTarget > maxVotes) {
				maxVotes = currentVotesForTarget;
			}
		}

		List<Player> mostVotedPlayers = new ArrayList<>();
		if (maxVotes > 0) { // 아무도 투표 안 받은 경우 제외
			for (Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
				if (entry.getValue() == maxVotes) {
					mostVotedPlayers.add(entry.getKey());
				}
			}
		}

		System.out.println("\n--- 투표 결과 ---");
		for (Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
			if (entry.getValue() > 0) { // 0표는 표시 안함 (선택)
				System.out.println(entry.getKey().getName() + ": " + entry.getValue() + "표");
			}
		}

		if (mostVotedPlayers.size() == 1) {
			Player executedPlayer = mostVotedPlayers.get(0);
			System.out.println("\n투표 결과, " + executedPlayer.getName() + "님이 최다 득표하였습니다.");

			// 정치인 처세 능력 확인
			if (executedPlayer.getJob() instanceof com.mafiagame.logic.job.Politician) {
				Politician politicianJob = (Politician) executedPlayer.getJob(); // 타입 캐스팅
				if (politicianJob.canEvadeExecutionByInfluence()) { // 정치인의 특화된 메서드 호출
					System.out.println(executedPlayer.getName() + "님은 정치인의 처세 능력으로 추방을 면했습니다! 직업은 [정치인] 입니다.");
					// 정체 공개, 추방되지 않음
				}
			} else {
				processExecution(executedPlayer);
			}

		} else if (mostVotedPlayers.size() > 1) {
			System.out.println("\n최다 득표자가 " + mostVotedPlayers.size() + "명으로 동점이므로, 아무도 추방되지 않습니다.");
		} else { // maxVotes == 0 인 경우 (아무도 투표 안했거나, 모든 투표가 0표)
			System.out.println("\n투표 결과, 아무도 추방되지 않았습니다.");
		}
		voteRecords.clear(); // 투표 기록 초기화
	}

	/**
	 * 실제 플레이어 추방 처리를 담당하는 메서드. 테러리스트 능력 발동 등을 여기서 처리.
	 * 
	 * @param executedPlayer 추방될 플레이어
	 */
	private void processExecution(Player executedPlayer) {
		System.out.println(executedPlayer.getName() + "님이 추방되어 게임에서 탈락합니다.");
		// executedPlayer.die(); // die()는 사망 메시지까지 출력하므로, 여기서는 상태만 변경하거나 메시지 조정

		// 테러리스트 능력 확인 및 처리
		if (executedPlayer.getJob() instanceof com.mafiagame.logic.job.Terrorist && executedPlayer.isAlive()) {
			// TODO Terrorist 클래스에 selectTargetForTerror(List<Player> livingPlayers,
			// GameManager gm) 같은 메서드 필요
			// 임시로 로직 구현
			System.out.println(executedPlayer.getName() + "님은 테러리스트입니다! 동반 탈락할 대상을 선택합니다.");
			List<Player> terrorTargets = new ArrayList<>(getLivingPlayers());
			terrorTargets.remove(executedPlayer); // 자신 제외

			if (!terrorTargets.isEmpty()) {
				for (int i = 0; i < terrorTargets.size(); i++) {
					displayMessageToPlayer(executedPlayer, (i + 1) + ". " + terrorTargets.get(i).getName());
				}
				int choice = -1;
				while (true) {
					String input = getPlayerInput(executedPlayer, "동반 탈락시킬 대상의 번호를 입력하세요: ");
					try {
						choice = Integer.parseInt(input) - 1;
						if (choice >= 0 && choice < terrorTargets.size()) {
							break;
						} else {
							displayMessageToPlayer(executedPlayer, "잘못된 번호입니다. 다시 입력하세요.");
						}
					} catch (NumberFormatException e) {
						displayMessageToPlayer(executedPlayer, "숫자로 입력해주세요.");
					}
				}
				Player terrorTarget = terrorTargets.get(choice);
				System.out.println(executedPlayer.getName() + "님의 테러로 " + terrorTarget.getName() + "님이 함께 탈락합니다!");
				terrorTarget.die();
				publicAnnouncements
						.add(executedPlayer.getName() + "님의 테러로 " + terrorTarget.getName() + "님이 함께 탈락했습니다.");
				if (terrorTarget.getJob().getInitialTeam() == Team.MAFIA) { // 룰: 마피아일 경우 직업 공개
					System.out.println(terrorTarget.getName() + "님의 직업은 [마피아]였습니다.");
					publicAnnouncements.add(terrorTarget.getName() + "님의 직업은 [마피아]였습니다.");
				}
			} else {
				System.out.println("테러할 대상이 없습니다.");
			}
		}

		executedPlayer.die(); // 추방된 플레이어 최종 사망 처리
		// 추방된 플레이어의 직업은 비공개 (룰에 따름)
	}

	/**
	 * 승리 조건을 확인하고, 게임 종료 여부를 결정합니다.
	 * 
	 * @return 게임이 종료되었으면 true, 아니면 false
	 */
	private boolean checkWinConditions() {
		// TODO: 룰에 정의된 각 팀의 승리 조건 판정 로직 구현
		// 1. 생존한 각 팀 인원수 계산 (마피아팀, 시민팀, 간첩팀)
		// 2. 마피아팀 승리 조건 확인 (정치인/건달 패널티 포함)
		// 3. 시민팀 승리 조건 확인 (클래식/간첩 모드별)
		// 4. 간첩팀 승리 조건 확인
		// 5. 승리팀 발생 시 isGameOver = true, winningTeam 설정. 승리 우선순위 적용.

		// 임시 로직: 모든 마피아가 죽으면 시민 승리 (매우 단순화)
		// List<Player> livingMafia = players.stream()
		// .filter(p -> p.isAlive() && p.getCurrentTeam() == Team.MAFIA)
		// .collect(Collectors.toList());
		// if (livingMafia.isEmpty() && players.stream().anyMatch(p -> p.isAlive() &&
		// p.getCurrentTeam() == Team.CITIZEN)) {
		// winningTeam = Team.CITIZEN;
		// isGameOver = true;
		// currentPhase = GamePhase.GAME_OVER;
		// return true;
		// }
		return false; // 임시 반환
	}

	/**
	 * 승리팀을 발표합니다.
	 */
	private void announceWinner() {
		if (winningTeam != null) {
			System.out.println("\n===================================");
			System.out.println("         게임 종료! 승리: " + winningTeam + " 팀!");
			System.out.println("===================================");
			// 추가적으로 최종 생존자 및 직업 공개 등을 할 수 있음
		} else {
			System.out.println("\n===================================");
			System.out.println("         게임 종료! (무승부 또는 오류)"); // 이 경우는 거의 없음
			System.out.println("===================================");
		}
	}

	/**
	 * 모든 플레이어의 밤 관련 임시 상태를 초기화합니다.
	 */
	private void resetPlayersNightStatus() {
		for (Player player : players) {
			player.resetNightStatus();
			player.setCanVoteToday(true); // 다음 날 투표 가능하도록 초기화
		}
	}

	// --- 헬퍼(유틸리티) 메서드 ---

	public List<Player> getLivingPlayers() {
		List<Player> living = new ArrayList<>();
		for (Player p : players) {
			if (p.isAlive()) {
				living.add(p);
			}
		}
		return living;
	}

	public int getDayCount() {
		return dayCount;
	}
	
	private Player getDoctorPlayer() {
        for (Player p : players) {
            if (p.isAlive() && p.getJob() instanceof Doctor) {
                return p;
            }
        }
        return null;
    }

    private Player getPlayerByJob(Class<? extends Job> jobClass) {
        for (Player p : players) {
            if (p.isAlive() && jobClass.isInstance(p.getJob())) {
                return p;
            }
        }
        return null;
    }

	/**
	 * (텍스트 기반) 특정 플레이어에게 메시지를 보여줍니다. 실제 앱에서는 UI 업데이트로 대체됩니다.
	 */
	public void displayMessageToPlayer(Player player, String message) {
		// player가 null이면 전체 공지로 처리하거나, 특정 플레이어 턴이 아닐 때
		if (player != null) {
			System.out.println("[" + player.getName() + "님께] " + message);
		} else if (player == null) { // player가 null인 경우로 변경, 단 추후 앱 단계에서 앱에 맞게 최적화 필요
			System.out.println("[전체] " + message); // 또는 다른 방식으로 처리
		}
	}

	/**
	 * (텍스트 기반) 현재 플레이어로부터 입력을 받습니다. 실제 앱에서는 버튼 클릭 등의 UI 이벤트로 대체됩니다.
	 * 
	 * @param currentPlayer 현재 입력을 받아야 할 플레이어 (null이면 일반적인 입력 대기)
	 * @param prompt        입력 안내 메시지
	 * @return 사용자 입력 문자열
	 */
	public String getPlayerInput(Player currentPlayer, String prompt) {
		if (currentPlayer != null) {
			System.out.print("[" + currentPlayer.getName() + "님] " + prompt);
		} else {
			System.out.print(prompt);
		}
		return scanner.nextLine().trim();
	}

	/**
	 * (텍스트 기반) 콘솔 화면을 지우는 효과를 냅니다. (실제 지우는 것은 아님) 다음 플레이어가 이전 플레이어의 정보를 보지 못하도록
	 * 합니다.
	 */
	private void clearConsole() {
		// 간단히 여러 줄을 출력하여 이전 내용을 밀어 올림
		for (int i = 0; i < 30; i++) { // 화면 크기에 따라 조절
			System.out.println();
		}
		System.out.println("--- (화면이 전환되었습니다) ---");
	}

	/**
	 * GamePhase Enum 값을 사람이 읽기 쉬운 문자열로 변환합니다. (디버깅/로그용)
	 */
	private String getPhaseName(GamePhase phase) {
		if (phase == null)
			return "알수없음";
		switch (phase) {
		case NIGHT_JOB_CONFIRM_ABILITY:
			return "첫날 밤: 직업 확인 및 능력 사용";
		case NIGHT_ABILITY_USE:
			return "밤: 능력 사용";
		case NIGHT_PRIVATE_CONFIRM:
			return "밤: 개인 결과 확인";
		case DAY_PUBLIC_ANNOUNCEMENT:
			return "낮: 공개 결과 발표";
		case DAY_DISCUSSION:
			return "낮: 토론";
		case DAY_VOTE:
			return "낮: 투표";
		case DAY_EXECUTION:
			return "낮: 처형";
		case GAME_OVER:
			return "게임 종료";
		default:
			return phase.name(); // Enum 이름 그대로 반환
		}
	}

	// GameManager가 다른 클래스(주로 Job)에서 현재 플레이어 목록이나 특정 플레이어 정보를
	// 가져갈 수 있도록 하는 public getter 메서드들이 필요할 수 있습니다.
	public Player getPlayerByNumber(int playerNumber) {
		for (Player p : players) {
			if (p.getPlayerNumber() == playerNumber) {
				return p;
			}
		}
		return null;
	}

	public List<Player> getAllPlayers() {
		return Collections.unmodifiableList(players); // 외부에서 리스트 직접 수정 방지: 캡슐화 유지
	}

	/**
	 * 밤 능력 사용 시 대상을 기록하는 메서드. Job 클래스의 performNightAction 내부에서 호출될 수 있습니다.
	 * 
	 * @param user   능력을 사용한 플레이어
	 * @param target 능력의 대상이 된 플레이어
	 */
	public void recordNightAbilityTarget(Player user, Player target) {
		if (user != null && target != null) {
			nightAbilityTargets.put(user, target);
		}
	}

	/**
	 * 밤 능력 사용 후 개인에게 전달할 결과 정보를 기록합니다. Job 클래스의 performNightAction 내부에서 호출될 수 있습니다.
	 * 
	 * @param player     결과를 확인할 플레이어
	 * @param resultInfo 해당 플레이어에게 전달할 결과 정보 (문자열, Map, 사용자 정의 객체 등)
	 */
	public void recordPrivateNightResult(Player player, Object resultInfo) {
		if (player != null) {
			nightResultsForPrivateConfirmation.put(player, resultInfo);
		}
	}

}