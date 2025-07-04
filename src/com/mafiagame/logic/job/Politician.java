package com.mafiagame.logic.job;

import java.util.List;

import com.mafiagame.logic.common.enums.Team;
import com.mafiagame.logic.game.GameManager;
import com.mafiagame.logic.game.Player;

public class Politician extends Job {

	public Politician(String jobName, Team team, String description, boolean hasNightAbility,
			boolean isOneTimeAbility) {
		super(jobName, team, description, hasNightAbility, isOneTimeAbility);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void performNightAction(Player self, List<Player> livingPlayers, GameManager gameManager) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getNightActionPrompt(Player self) {
		// TODO Auto-generated method stub
		return null;
	}

}
