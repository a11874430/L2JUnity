/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.scripts.quests.Q10416_InSearchOfTheEyeOfArgos;

import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;

/**
 * In Search of the Eye of Argos (10416)
 * @author St3eT
 */
public final class Q10416_InSearchOfTheEyeOfArgos extends Quest
{
	// NPCs
	private static final int JANITT = 33851;
	private static final int EYE_OF_ARGOS = 31683;
	// Items
	private static final int EAA = 730; // Scroll: Enchant Armor (A-grade)
	// Misc
	private static final int MIN_LEVEL = 70;
	private static final int MAX_LEVEL = 75;
	
	public Q10416_InSearchOfTheEyeOfArgos()
	{
		super(10416);
		addStartNpc(JANITT);
		addTalkId(JANITT, EYE_OF_ARGOS);
		addCondNotRace(Race.ERTHEIA, "33851-06.html");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "33851-07.htm");
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		final QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "33851-02.htm":
			case "33851-03.htm":
			{
				htmltext = event;
				break;
			}
			case "33851-04.htm":
			{
				st.startQuest();
				htmltext = event;
				break;
			}
			case "31683-02.html":
			{
				if (st.isCond(1))
				{
					st.exitQuest(false, true);
					giveItems(player, EAA, 2);
					giveStoryQuestReward(npc, player);
					if (player.getLevel() > MIN_LEVEL)
					{
						addExp(player, 1_088_640);
						addSp(player, 261);
					}
					htmltext = event;
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		final QuestState st = getQuestState(player, true);
		String htmltext = null;
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == JANITT)
				{
					htmltext = "33851-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (st.isCond(1))
				{
					htmltext = npc.getId() == JANITT ? "33851-05.html" : "31683-01.html";
				}
				break;
			}
			case State.COMPLETED:
			{
				if (npc.getId() == JANITT)
				{
					htmltext = getAlreadyCompletedMsg(player);
				}
				break;
			}
		}
		return htmltext;
	}
}