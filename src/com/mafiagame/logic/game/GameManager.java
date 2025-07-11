package com.mafiagame.logic.game;

import com.mafiagame.logic.common.enums.*;	// enums 임포트
import com.mafiagame.logic.job.*;			// job 패키지 임포트
import com.mafiagame.ui.GameUI;				// UI 인터페이스 임포트


// 자바 라이브러리 임포트
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 마피아 게임의 전체 진행을 관리하는 클래스
 * 게임 설정, 턴 관리, 능력 결과 처리, 승리 조건 판정 등을 담당
 */
public class GameManager {
	
    private final GameUI ui; 		// UI 인터페이스
	private List<Player> players;	// 전체 플레이어 리스트
	private int playerCount;		// 전체 플레이어 숫자
	private GameMode gameMode;		// 게임 모드
	private GamePhase currentPhase;	// 현재 페이즈
	private int dayCount;			// 현재 날짜
	private boolean isGameOver;		// 게임 종료 여부
	private Team winningTeam;		// 승리 팀
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
	
    // 마피아의 실시간 지명 기록 (지명자, 지명 대상)
	private Map<Player, Player> mafiaNominations;
	
	// 밤 턴을 진행할 플레이어 목록
    private List<Player> playersForNightTurn;
    
    // 공개적으로 밝혀진 직업 정보 저장 (플레이어, 직업 타입)
    private Map<Player, JobType> revealedJobs;


	public GameManager(GameUI ui) {
		this.ui = ui; // 외부에서 생성된 UI 객체 유입
		this.players = new ArrayList<>();
		this.nightResultsForPrivateConfirmation = new HashMap<>();
		this.nightAbilityTargets = new HashMap<>();
		this.voteRecords = new HashMap<>();
		this.publicAnnouncements = new ArrayList<>();
		this.intimidatedPlayers = new ArrayList<>();
		this.executedPlayersToday = new ArrayList<>();
		this.mafiaNominations = new HashMap<>();
		this.playersForNightTurn = new ArrayList<>();
        this.revealedJobs = new HashMap<>();
		this.dayCount = 1; // 1일차부터 시작
		this.isGameOver = false;
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

		ui.displayPublicMessage("게임 설정이 완료되었습니다. 총 " + playerCount + "명의 플레이어, 모드: " + gameMode);
		for (Player p : players) {
			System.out.println("[디버그] " + p.toString()); // 배정된 직업 확인용 (디버깅)
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
		System.out.println("[디버그] 모든 플레이어에게 직업이 성공적으로 배정되었습니다.");
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
		ui.displaySystemMessage("\n마피아 게임을 시작합니다!");
		this.currentPhase = GamePhase.NIGHT_JOB_CONFIRM_ABILITY;
		this.isGameOver = false;
		
		this.playersForNightTurn.clear();
	    this.playersForNightTurn.addAll(this.players);

		while (!isGameOver) {
			ui.displaySystemMessage("\n--- [" + (dayCount) + "일차] " + getPhaseName(currentPhase) + " 페이즈 ---");
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
				ui.displayPublicMessage("알 수 없는 게임 단계입니다. 게임을 종료합니다.");
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
		ui.displaySystemMessage("\n마피아 게임이 종료되었습니다.");
	}
	
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
			this.mafiaNominations.clear(); // 마피아 지명 기록 초기화
			this.playersForNightTurn.clear();
            this.playersForNightTurn.addAll(getLivingPlayers());
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
		ui.displayPublicMessage("첫날 밤입니다. 각자 직업을 확인하고 직업 확인 후 바로 개인 능력을 사용합니다.");
		nightAbilityTargets.clear();

		for (Player currentPlayer : this.playersForNightTurn) {
			if (!currentPlayer.isAlive()) continue;

			// 1. 직업 확인 메시지
			ui.displayPrivateMessage(currentPlayer, "당신의 차례입니다. 화면을 확인하세요.");
			Job currentJob = currentPlayer.getJob();
			ui.displayPrivateMessage(currentPlayer, "당신의 직업은 [" + currentJob.getJobName() + "] 입니다.");

			if (currentJob != null) {
				currentJob.onAssigned(currentPlayer, this);
			}
			
			handlePlayerNightActionTurn(currentPlayer);

			ui.waitForPlayerConfirmation(currentPlayer, "Enter 키를 누르고 다음 사람에게 넘기세요.");
            ui.clearScreen();
        }
        applyNightActionsAndResults();
    }
	
	/**
	 * (일반) 밤 능력 사용 페이즈
	 */
	private void processNightAbilityUsePhase() {
		ui.displayPublicMessage("밤입니다. 능력을 사용할 플레이어는 차례대로 진행합니다.");
		nightAbilityTargets.clear(); // 밤 능력 대상 기록 초기화

		for (Player currentPlayer : this.playersForNightTurn) {
			if (!currentPlayer.isAlive()) continue;
			
			handlePlayerNightActionTurn(currentPlayer);

			ui.waitForPlayerConfirmation(currentPlayer, "선택이 완료되었습니다. Enter 키를 누르고 다음 사람에게 넘기세요.");
			ui.clearScreen();
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
            	currentPlayer.performNightAction(this);
            } else {
                String cantUseMessage = currentPlayer.getName() + "님은 이번 밤에 능력을 사용할 수 없습니다.";
                if (currentPlayer.getJob().isOneTimeAbility() && currentPlayer.getJob().hasUsedOneTimeAbility()) {
                    cantUseMessage += " (이미 사용)";
                } else if (currentPlayer.getJob() instanceof Reporter && this.dayCount == 1) {
                    cantUseMessage += " (엠바고)";
                }
                ui.displayPrivateMessage(currentPlayer, cantUseMessage);
            }
        } else {
        	ui.displayPrivateMessage(currentPlayer, "사용할 수 있는 밤 능력이 없습니다.");
        }
    }
	
    /**
     * 밤 동안 사용된 능력들의 결과를 종합하고 플레이어 상태를 업데이트
     */
    private void applyNightActionsAndResults() {
        // 1. 밤 행동 기록을 리스트로 변환
        List<Map.Entry<Player, Player>> sortedActions = new ArrayList<>(nightAbilityTargets.entrySet());
        
        // 2. 우선순위가 높은 순서(내림차순)로 정렬
        sortedActions.sort((entry1, entry2) -> {
            int priority1 = entry1.getKey().getJob().getNightActionPriority();
            int priority2 = entry2.getKey().getJob().getNightActionPriority();
            return Integer.compare(priority2, priority1);
        });

        // 3. 정렬된 순서대로 각 직업의 효과 적용 메서드 호출
        for (Map.Entry<Player, Player> action : sortedActions) {
            Player user = action.getKey();
            Player target = action.getValue();
            
            // 능력을 사용한 플레이어가 그 사이 다른 능력에 의해 죽지 않았는지 확인
            if (user != null && user.isAlive()) {
                 user.getJob().applyNightEffect(this, user, target);
            }
        }

        // 4. '이번 밤에' 사망한 플레이어 탐색 찾아내는 로직으로 변경
        List<Player> diedThisNight = new ArrayList<>();
        for (Player p : this.playersForNightTurn) {
            if (!p.isAlive()) {
                // 사망 공지가 이미 기록되지 않은 경우에만 추가 (중복 방지)
                if (!isDeathAnnounced(p)) {
                    diedThisNight.add(p);
                }
            }
        }

        // 5. 도굴꾼 능력 처리
        handleGraveRobberAbility(diedThisNight);
        
        // 6. 사망자 최종 공지
        for(Player deadPlayer : diedThisNight){
            addPublicAnnouncement(deadPlayer.getName() + "님이 밤 사이 사망했습니다.");
       }
        
        nightAbilityTargets.clear();
    }

    /**
     * 첫날 밤, 도굴꾼의 직업 훔치기 능력 처리
     */
    private void handleGraveRobberAbility(List<Player> diedThisNight) {
        // 도굴꾼 능력은 첫날 밤(dayCount == 1)에만 발동
        if (dayCount != 1) return;

        Player graveRobber = getPlayerByJob(GraveRobber.class);
        // 도굴꾼이 없거나, 죽었거나, 첫날 밤에 아무도 죽지 않았다면 발동 안함
        if (graveRobber == null || !graveRobber.isAlive() || diedThisNight.isEmpty()) return;

        // 공격으로 죽은 첫 번째 플레이어를 대상으로 함
        Player firstDead = diedThisNight.get(0);
        
        // 직업을 훔침
        Job stolenJob = firstDead.getJob();
        graveRobber.setJob(stolenJob);
        
        // 도굴꾼에게 알림
        String message = "당신은 " + firstDead.getName() + "님의 직업인 [" + stolenJob.getJobName() + "]을 훔쳤습니다!";
        recordPrivateNightResult(graveRobber, message);
    }
    
    // 유틸리티 메서드: 사망 공지가 이미 되었는지 확인 (중복 공지 방지용)
    private boolean isDeathAnnounced(Player player) {
        String deathMessage1 = player.getName() + "님이 밤 사이 사망했습니다.";
        String deathMessage2 = "늑대인간의 습격으로 " + player.getName() + "님이 살해당했습니다!";
        return publicAnnouncements.stream().anyMatch(ann -> ann.contains(player.getName()));
    }
    
	/**
	 * 밤 개인 결과 확인 페이즈
	 */
	private void processNightPrivateConfirmPhase() {
		ui.displayPublicMessage("밤 동안의 개인 결과를 확인합니다.");
		for (Player currentPlayer : this.playersForNightTurn) {
			ui.displayPrivateMessage(currentPlayer, "당신의 차례입니다. 화면을 확인하세요.");

			// GameManager에 저장된 해당 플레이어의 밤 결과 정보를 가져옴
			Object resultInfo = nightResultsForPrivateConfirmation.get(currentPlayer);
			String privateMessage = currentPlayer.getJob().getPrivateNightResultMessage(currentPlayer, resultInfo, this); // Job에서 직접 메시지 생성

			if (privateMessage != null && !privateMessage.isEmpty()) {
				ui.displayPrivateMessage(currentPlayer, privateMessage);
			} else {
				ui.displayPrivateMessage(currentPlayer, "특별한 개인 결과가 없습니다.");
			}

			ui.waitForPlayerConfirmation(currentPlayer, "확인 후 Enter 키를 누르고 다음 사람에게 넘기세요.");
			ui.clearScreen();
		}
		nightResultsForPrivateConfirmation.clear(); // 확인 후 초기화

	}

	/**
     * 낮 공개 결과 발표 페이즈
     * 
     * 1. 기자/장의사의 취재/부검 결과를 처리하여 공개 공지 목록에 추가
     * 2. 밤 동안 발생한 사망자 및 기타 사건들 공지
     */
    private void processDayPublicAnnouncementPhase() {
        // 기자와 장의사의 능력 결과를 여기서 처리
        handleDelayedPublicAnnouncements();

        ui.displayPublicMessage("낮이 밝았습니다. 밤 동안의 공개 결과입니다.");
        if (publicAnnouncements.isEmpty()) {
            ui.displayPublicMessage("밤 사이 아무 일도 일어나지 않았습니다.");
        } else {
            for (String announcement : publicAnnouncements) {
                ui.displayPublicMessage(announcement);
            }
        }
        publicAnnouncements.clear();
        executedPlayersToday.clear();
    }
    
    /**
     * 기자, 장의사 등 밤에 사용했지만 다음 날 아침에 공개되는 능력들 처리
     */
    private void handleDelayedPublicAnnouncements() {
        // nightAbilityTargets에 기록된 모든 밤 행동을 확인
        for (Map.Entry<Player, Player> entry : nightAbilityTargets.entrySet()) {
            Player user = entry.getKey();
            Player target = entry.getValue();

            // 기자의 취재 결과 처리
            if (user.getJob() instanceof Reporter) {
                if (user.isAlive()) {
                    addPublicAnnouncement("기자의 취재 결과, " + target.getName() + "님의 직업은 [" + target.getJob().getJobName() + "] 입니다!");
                } else {
                    addPublicAnnouncement("기자가 취재를 시도했으나 밤 사이 사망하여 특종이 묻혔습니다.");
                }
            }
            
            // 장의사의 부검 결과 처리
            else if (user.getJob() instanceof Undertaker) {
                if (user.isAlive()) {
                    addPublicAnnouncement("장의사의 부검 결과, " + target.getName() + "님의 직업은 [" + target.getJob().getJobName() + "] 였습니다.");
                } else {
                    addPublicAnnouncement("장의사가 부검을 시도했으나 밤 사이 사망하여 부검 결과가 유실되었습니다.");
                }
            }
        }
    }

	/**
	 * 낮 토론 페이즈
	 */
	private void processDayDiscussionPhase() {
		ui.displayPublicMessage("토론 시간입니다. 자유롭게 토론하세요.");
		// 텍스트 기반에서는 실제 토론은 플레이어들이 하고, 앱은 시간 제한 정도만 둘 수 있음
		// 여기서는 간단히 메시지만 출력하고 넘어감
		ui.waitForPlayerConfirmation(null, "토론이 끝나면 Enter 키를 누르세요.");
	}

	/**
	 * 낮 투표 페이즈
	 */
	private void processDayVotePhase() {
		ui.displayPublicMessage("투표 시간입니다. 처형할 사람을 지목해주세요.");
		voteRecords.clear(); // 투표 기록 초기화

		List<Player> livingVoters = getLivingPlayers();
		for (Player voter : livingVoters) {
			if (!voter.canVoteToday()) {
				ui.displayPrivateMessage(voter, "당신은 오늘 투표할 수 없습니다 (건달 협박).");
				ui.waitForPlayerConfirmation(voter, "확인 후 Enter 키를 누르고 넘기세요.");
				ui.clearScreen();
				continue;
			}

			while (true) {
	            Player votedPlayer = ui.promptForPlayerSelection(voter, "투표할 대상을 선택하세요.", getAllPlayers(), false, this);

	            if (votedPlayer != null && votedPlayer.equals(voter)) {
	                ui.displayPrivateMessage(voter, "자기 자신에게는 투표할 수 없습니다. 다시 선택해주세요.");
	                continue;
	            }
	            
	            // 유효성 검사 통과
	            if (votedPlayer != null) {
	                voteRecords.put(voter, votedPlayer);
	                ui.displayPrivateMessage(voter, votedPlayer.getName() + "님에게 투표했습니다.");
	            } else {
	                voteRecords.put(voter, null);
	                ui.displayPrivateMessage(voter, "기권했습니다.");
	            }
	            break; // 루프 종료
			}
            ui.waitForPlayerConfirmation(voter, "투표 완료. Enter 키를 누르고 넘기세요.");
            ui.clearScreen();
        }
    }

	/**
     * 낮 추방 결과 처리 페이즈
     * 
     * 1. 모든 투표를 집계 (정치인의 2표 포함)
     * 2. 투표 결과 전체 공지
     * 3. 최다 득표자 결정, 동점 여부 확인
     * 4. 최다 득표자가 1명일 경우, 정치인의 처세 능력을 확인 후 추방 절차 진행
     */
    private void processDayExecutionPhase() {
        if (voteRecords.isEmpty()) {
            ui.displayPublicMessage("투표가 진행되지 않아 아무도 추방되지 않았습니다.");
            return;
        }

        // 1. 투표 집계: 각 플레이어가 받은 득표수를 계산
        Map<Player, Integer> voteCounts = new HashMap<>();
        int maxVotes = 0;

        for (Map.Entry<Player, Player> entry : voteRecords.entrySet()) {
            Player voter = entry.getKey();
            Player votedTarget = entry.getValue();
            
            // 유효한 투표인지 확인 (오류 방지)
            if (voter == null || votedTarget == null) continue;

            // 투표자의 직업에 따라 투표 가중치(voteWeight) 가져옴
            int voteWeight = voter.getJob().getVoteWeight();
            
            // 해당 후보의 득표수에 가중치를 더함
            int currentVotes = voteCounts.getOrDefault(votedTarget, 0);
            voteCounts.put(votedTarget, currentVotes + voteWeight);
        }

        // 2. 투표 결과 공지
        ui.displayPublicMessage("\n--- 투표 결과 ---");
        if (voteCounts.isEmpty()) {
            ui.displayPublicMessage("아무도 득표하지 않았습니다.");
        } else {
            for (Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
                ui.displayPublicMessage(entry.getKey().getName() + ": " + entry.getValue() + "표");
            }
        }
        
        // 3. 최다 득표자 찾기
        for (int votes : voteCounts.values()) {
            if (votes > maxVotes) {
                maxVotes = votes;
            }
        }

        List<Player> mostVotedPlayers = new ArrayList<>();
        if (maxVotes > 0) {
            for (Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
                if (entry.getValue() == maxVotes) {
                    mostVotedPlayers.add(entry.getKey());
                }
            }
        }

        // 4. 결과 처리
        if (mostVotedPlayers.size() == 1) {
            Player executedPlayer = mostVotedPlayers.get(0);
            ui.displayPublicMessage("\n투표 결과, " + executedPlayer.getName() + "님이 최다 득표하였습니다.");

            // 정치인의 '처세' 능력 발동 여부 확인
            if (executedPlayer.getJob() instanceof Politician) {
                Politician politicianJob = (Politician) executedPlayer.getJob();
                if (politicianJob.tryEvadeExecution(executedPlayer, this)) {
                    ui.displayPublicMessage(executedPlayer.getName() + "님은 정치인의 처세 능력으로 추방을 면했습니다! 직업은 [정치인] 입니다.");
                    // 추방이 무효화되었으므로, 여기서 메서드 실행을 종료
                    voteRecords.clear(); // 투표 기록만 초기화하고 종료
                    return;
                }
            }
            
            // 정치인 능력이 발동하지 않았거나, 다른 직업인 경우 정상 추방 절차 진행
            processExecution(executedPlayer);

        } else if (mostVotedPlayers.size() > 1) {
            ui.displayPublicMessage("\n최다 득표자가 " + mostVotedPlayers.size() + "명으로 동점이므로, 아무도 추방되지 않습니다.");
        } else { // maxVotes == 0
            ui.displayPublicMessage("\n투표 결과, 아무도 추방되지 않았습니다.");
        }
        
        // 다음 날을 위해 투표 기록 초기화
        voteRecords.clear();
    }

	/**
	 * 실제 플레이어 추방 처리를 담당하는 메서드
	 * 테러리스트 능력 발동 등을 처리
	 * 
	 * @param executedPlayer 추방될 플레이어
	 */
	private void processExecution(Player executedPlayer) {
		ui.displayPublicMessage(executedPlayer.getName() + "님이 추방되어 게임에서 탈락합니다.");
		// executedPlayer.die();
		// die()는 사망 메시지까지 출력하므로, 여기서는 상태만 변경하거나 메시지 조정

		// 테러리스트 능력 확인 및 처리
		if (executedPlayer.getJob() instanceof Terrorist && executedPlayer.isAlive()) {
			// TODO Terrorist 클래스에 selectTargetForTerror(List<Player> livingPlayers,
			// GameManager gm) 같은 메서드 필요
			// 임시로 로직 구현
			ui.displayPublicMessage(executedPlayer.getName() + "님은 테러리스트입니다! 동반 탈락할 대상을 선택합니다.");
			List<Player> terrorTargets = new ArrayList<>(getLivingPlayers());
			terrorTargets.remove(executedPlayer); // 자신 제외

			if (!terrorTargets.isEmpty()) {
				Player terrorTarget = ui.promptForPlayerSelection(executedPlayer, "동반 탈락시킬 대상의 번호를 입력하세요:", terrorTargets, false, this);
                
                ui.displayPublicMessage(executedPlayer.getName() + "님의 테러로 " + terrorTarget.getName() + "님이 함께 탈락합니다!");
                terrorTarget.die();
				
				if (terrorTarget.getJob().getInitialTeam() == Team.MAFIA) {
					ui.displayPublicMessage(terrorTarget.getName() + "님의 직업은 [마피아]였습니다.");
				}
			} else {
				ui.displayPublicMessage("테러할 대상이 없습니다.");
			}
		}

		executedPlayer.die(); // 추방된 플레이어 최종 사망 처리
		// 추방된 플레이어의 직업은 비공개 (룰에 따름)
	}

	/**
	 * 승리 조건을 확인하고, 게임 종료 여부 결정
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
	 * 승리팀 발표
	 */
	private void announceWinner() {
		if (winningTeam != null) {
			ui.displayPublicMessage("\n===================================");
			ui.displayPublicMessage("         게임 종료! 승리: " + winningTeam + " 팀!");
			ui.displayPublicMessage("===================================");
			// 추가적으로 최종 생존자 및 직업 공개 등을 할 수 있음
		} else {
			ui.displayPublicMessage("\n===================================");
			ui.displayPublicMessage("         게임 종료! (무승부 또는 오류)"); // 이 경우는 거의 없음
			ui.displayPublicMessage("===================================");
		}
	}

	/**
	 * 모든 플레이어의 밤 관련 임시 상태 초기화
	 */
	private void resetPlayersNightStatus() {
		for (Player player : players) {
			player.resetNightStatus();
			player.setCanVoteToday(true); // 다음 날 투표 가능하도록 초기화
		}
	}

	// --- 헬퍼(유틸리티) 메서드 ---

	public GameUI getUi() {
		return this.ui;
	}
	
	public int getDayCount() {
		return dayCount;
	}
	
    private Player getPlayerByJob(Class<? extends Job> jobClass) {
        for (Player p : players) {
            if (p.isAlive() && jobClass.isInstance(p.getJob())) {
                return p;
            }
        }
        return null;
    }
	
	// GameManager가 다른 클래스(주로 Job)에서 현재 플레이어 목록이나 특정 플레이어 정보를
	// 가져갈 수 있도록 하는 public getter 메서드들이 필요할 수 있음
	public Player getPlayerByNumber(int playerNumber) {
		for (Player p : players) {
			if (p.getPlayerNumber() == playerNumber) {
				return p;
			}
		}
		return null;
	}

	public List<Player> getAllPlayers() {
		return Collections.unmodifiableList(players);
	}
	
	public List<Player> getLivingPlayers() {
		List<Player> living = new ArrayList<>();
		for (Player p : players) {
			if (p.isAlive()) {
				living.add(p);
			}
		}
		return living;
	}
	
	public List<Player> getDeadPlayers() {
        return players.stream().filter(p -> !p.isAlive()).collect(Collectors.toList());
    }
	
	public Map<Player, Player> getNightAbilityTargets() {
        return this.nightAbilityTargets;
    }
	
	/**
     * Job 클래스들이 플레이어 입력을 받기 위해 호출할 메서드
     * 실제 입력 처리는 UI 객체에 위임
     */
	public Player getPlayerInputForNightAction(Player actor, String prompt, List<Player> targets, boolean allowDeadTargets) {
        return ui.promptForPlayerSelection(actor, prompt, targets, allowDeadTargets, this);
    }
	
	/**
	 * 밤 능력 사용 시 대상을 기록하는 메서드
	 * Job 클래스의 performNightAction 내부에서 호출 가능
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
	 * 밤 능력 사용 후 개인에게 전달할 결과 정보 기록
	 * Job 클래스의 performNightAction 내부에서 호출 가능
	 * 
	 * @param player     결과를 확인할 플레이어
	 * @param resultInfo 해당 플레이어에게 전달할 결과 정보 (문자열, Map, 사용자 정의 객체 등)
	 */
	public void recordPrivateNightResult(Player player, Object resultInfo) {
		if (player != null) {
			nightResultsForPrivateConfirmation.put(player, resultInfo);
		}
	}
	
	/**
     * 특정 플레이어의 직업을 게임 전체에 공개적으로 밝힘
     * 
     * @param player 직업을 공개할 플레이어
     */
    public void revealJob(Player player) {
        if (player != null && player.getJob() != null) {
            this.revealedJobs.put(player, player.getJob().getJobType());
        }
    }
    
    /**
     * 특정 플레이어의 직업이 공개되었는지 확인하고, 공개되었다면 직업 타입을 반환
     * 
     * @param player 확인할 플레이어
     * @return 공개된 JobType, 없으면 null
     */
    public JobType getRevealedJob(Player player) {
        return this.revealedJobs.get(player);
    }
	
	/**
	 * (마피아용) 실시간 지명 내용을 기록
	 * 
	 * @param nominator 지명한 마피아
	 * @param target    지명된 플레이어
	 */
	public void recordMafiaNomination(Player nominator, Player target) {
		if (nominator != null && target != null) {
			this.mafiaNominations.put(nominator, target);
		}
	}
	
	/**
	 * (마피아용) 현재까지의 지명 기록을 가져옴
	 * 
	 * @return 지명 기록 Map
	 */
	public Map<Player, Player> getMafiaNominations() {
		return this.mafiaNominations;
	}	
	
	/**
     * Job 클래스들이 공개 공지사항을 추가하기 위해 호출할 메서드
     */
    public void addPublicAnnouncement(String message) {
        this.publicAnnouncements.add(message);
    }
    
    /**
     * UI에 표시될 특정 플레이어의 상세 정보 목록 반환
     * 
     * @param actor 정보를 보려는 주체 (선택하는 사람)
     * @param target 정보가 표시될 대상 플레이어
     * @return 상세 정보 문자열 리스트
     */
    public List<String> getPlayerDisplayDetails(Player actor, Player target) {
        List<String> details = new ArrayList<>();

        // 1. 자기 자신인지 확인
        if (actor.equals(target)) {
            details.add("본인");
        }

        // 2. 공개적으로 밝혀진 직업이 있는지 확인
        JobType revealedJob = getRevealedJob(target);
        if (revealedJob != null) {
            details.add(revealedJob.name());
        }
        // 3. 직업이 공개되지 않았고, 같은 팀일 경우 직업 정보 추가
        else if (actor.getCurrentTeam() == target.getCurrentTeam() && !actor.equals(target)) {
        	Job actorJob = actor.getJob();
            Job targetJob = target.getJob();
            boolean showJob = false;

         // Case 1: 내가 '마피아'일 때
            if (actorJob instanceof Mafia) {
                if (targetJob instanceof Mafia) {
                    showJob = true; // 다른 마피아의 직업은 항상 본다.
                } else if (targetJob instanceof Informant) {
                    showJob = ((Informant) targetJob).hasContacted(); // 정보원은 접선해야만 본다.
                } else if (targetJob instanceof Warewolf) {
                    showJob = ((Warewolf) targetJob).hasContacted(); // 늑대인간도 접선해야만 본다.
                }
            }
            // Case 2: 내가 '정보원'일 때
            else if (actorJob instanceof Informant) {
                // 나는 '접선'을 해야만 다른 팀원의 직업을 볼 수 있다.
                if (((Informant) actorJob).hasContacted()) {
                    showJob = true;
                }
            }
            // Case 3: 내가 '늑대인간'일 때
            else if (actorJob instanceof Warewolf) {
                // 나도 '접선'을 해야만 다른 팀원의 직업을 볼 수 있다.
                if (((Warewolf) actorJob).hasContacted()) {
                    showJob = true;
                }
            }
            // Case 4: 내가 '간첩 팀'일 때 (예: 간첩)
            else if (actor.getCurrentTeam() == Team.SPY) {
                showJob = true; // 간첩 팀은 서로의 직업을 항상 본다.
            }

            if (showJob) {
                details.add(targetJob.getJobType().name());
            }
        }
        
        // 4. 탈락 여부 추가
        if (!target.isAlive()) {
            details.add("탈락");
        }

        return details;
    }
	
    /**
	 * GamePhase Enum 값을 사람이 읽기 쉬운 문자열로 변환 (디버깅/로그용)
	 */
	private String getPhaseName(GamePhase phase) {
		if (phase == null)
			return "알수없음";
		switch (phase) {
		case NIGHT_JOB_CONFIRM_ABILITY:
			return "첫날 밤 (직업 확인 및 능력 사용)";
		case NIGHT_ABILITY_USE:
			return "밤 (능력 사용)";
		case NIGHT_PRIVATE_CONFIRM:
			return "밤 (개인 결과 확인)";
		case DAY_PUBLIC_ANNOUNCEMENT:
			return "낮 (공개 결과 발표)";
		case DAY_DISCUSSION:
			return "낮 (토론)";
		case DAY_VOTE:
			return "낮 (투표)";
		case DAY_EXECUTION:
			return "낮 (처형)";
		case GAME_OVER:
			return "게임 종료";
		default:
			return phase.name(); // Enum 이름 그대로 반환
		}
	}
}