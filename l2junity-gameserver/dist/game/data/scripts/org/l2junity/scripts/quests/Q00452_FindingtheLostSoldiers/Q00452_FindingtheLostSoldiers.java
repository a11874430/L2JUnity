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
package org.l2junity.scripts.quests.Q00452_FindingtheLostSoldiers;

import org.l2junity.gameserver.enums.QuestType;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;

/**
 * Finding the Lost Soldiers (452)
 * @author Gigiikun
 * @version 2012-08-10
 */
public class Q00452_FindingtheLostSoldiers extends Quest
{
	private static final int JAKAN = 32773;
	private static final int TAG_ID = 15513;
	private static final int[] SOLDIER_CORPSES =
	{
		32769,
		32770,
		32771,
		32772
	};
	
	public Q00452_FindingtheLostSoldiers()
	{
		super(452);
		addStartNpc(JAKAN);
		addTalkId(JAKAN);
		addTalkId(SOLDIER_CORPSES);
		registerQuestItems(TAG_ID);
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		QuestState st = getQuestState(player, false);
		if (st == null)
		{
			return getNoQuestMsg(player);
		}
		
		String htmltext = event;
		
		if (npc.getId() == JAKAN)
		{
			if (event.equals("32773-3.htm"))
			{
				st.startQuest();
			}
		}
		else
		{
			if (st.isCond(1))
			{
				if (getRandom(10) < 5)
				{
					giveItems(player, TAG_ID, 1);
				}
				else
				{
					htmltext = "corpse-3.html";
				}
				st.setCond(2, true);
				npc.deleteMe();
			}
			else
			{
				htmltext = "corpse-3.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		if (st == null)
		{
			return htmltext;
		}
		
		if (npc.getId() == JAKAN)
		{
			switch (st.getState())
			{
				case State.CREATED:
					htmltext = (player.getLevel() < 84) ? "32773-0.html" : "32773-1.htm";
					break;
				case State.STARTED:
					if (st.isCond(1))
					{
						htmltext = "32773-4.html";
					}
					else if (st.isCond(2))
					{
						htmltext = "32773-5.html";
						takeItems(player, TAG_ID, -1);
						giveAdena(player, 95200, true);
						addExp(player, 435024);
						addSp(player, 50366); // TODO Incorrect SP reward.
						st.exitQuest(QuestType.DAILY, true);
					}
					break;
				case State.COMPLETED:
					if (st.isNowAvailable())
					{
						st.setState(State.CREATED);
						htmltext = (player.getLevel() < 84) ? "32773-0.html" : "32773-1.htm";
					}
					else
					{
						htmltext = "32773-6.html";
					}
					break;
			}
		}
		else
		{
			if (st.isCond(1))
			{
				htmltext = "corpse-1.html";
			}
		}
		return htmltext;
	}
}
