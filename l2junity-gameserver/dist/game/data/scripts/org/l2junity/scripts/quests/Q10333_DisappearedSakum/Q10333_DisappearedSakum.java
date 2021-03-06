/*
 * Copyright (C) 2004-2016 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.scripts.quests.Q10333_DisappearedSakum;

import java.util.HashSet;
import java.util.Set;

import org.l2junity.gameserver.enums.QuestSound;
import org.l2junity.gameserver.enums.Race;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.NpcLogListHolder;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.model.quest.State;

/**
 * Disappeared Sakum (10333)
 * @author St3eT
 */
public final class Q10333_DisappearedSakum extends Quest
{
	// NPCs
	private static final int BATHIS = 30332;
	private static final int VENT = 33176;
	private static final int SCHUNAIN = 33508;
	private static final int LIZARDMEN = 20030;
	private static final int VAKU_ORC = 20017;
	private static final int[] SPIDERS =
	{
		23094, // Poisonous Spider
		23021, // Giant Venomous Spider
		23095, // Archnid Predator
	};
	// Items
	private static final int BADGE = 17583;
	// Misc
	private static final int MIN_LEVEL = 18;
	private static final int MAX_LEVEL = 40;
	
	public Q10333_DisappearedSakum()
	{
		super(10333);
		addStartNpc(BATHIS);
		addTalkId(BATHIS, VENT, SCHUNAIN);
		addKillId(LIZARDMEN, VAKU_ORC);
		addKillId(SPIDERS);
		registerQuestItems(BADGE);
		addCondNotRace(Race.ERTHEIA, "30332-09.htm");
		addCondLevel(MIN_LEVEL, MAX_LEVEL, "30332-10.htm");
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
			case "30332-02.htm":
			case "30332-03.htm":
			case "30332-04.htm":
			case "33176-02.htm":
			case "33508-02.htm":
			{
				htmltext = event;
				break;
			}
			case "30332-05.htm":
			{
				st.startQuest();
				htmltext = event;
				break;
			}
			case "33176-03.htm":
			{
				if (st.isCond(1))
				{
					htmltext = event;
					st.setCond(2, true);
				}
				break;
			}
			case "33508-03.htm":
			{
				if (st.isCond(3))
				{
					if ((player.getLevel() >= MIN_LEVEL))
					{
						addExp(player, 180_000);
						addSp(player, 43);
						st.exitQuest(false, true);
						htmltext = event;
					}
					else
					{
						htmltext = getNoQuestLevelRewardMsg(player);
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, true);
		
		switch (st.getState())
		{
			case State.CREATED:
			{
				if (npc.getId() == BATHIS)
				{
					htmltext = "30332-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (npc.getId())
				{
					case BATHIS:
					{
						htmltext = st.isCond(0) ? "30332-06.htm" : "30332-07.htm";
						break;
					}
					case VENT:
					{
						if (st.isCond(1))
						{
							htmltext = "33176-01.htm";
						}
						else if (st.isCond(2))
						{
							htmltext = "33176-04.htm";
						}
						else if (st.isCond(3))
						{
							htmltext = "33176-05.htm";
						}
						break;
					}
					case SCHUNAIN:
					{
						if (st.isCond(3))
						{
							htmltext = "33508-01.htm";
						}
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				switch (npc.getId())
				{
					case BATHIS:
					{
						htmltext = "30332-08.htm";
						break;
					}
					case VENT:
					{
						htmltext = "33176-06.htm";
						break;
					}
					case SCHUNAIN:
					{
						htmltext = "33508-04.htm";
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		final QuestState st = getQuestState(killer, false);
		
		if ((st != null) && st.isStarted() && st.isCond(2))
		{
			int killedLizardmen = st.getInt("killed_" + LIZARDMEN);
			int killedVakuOrc = st.getInt("killed_" + VAKU_ORC);
			
			switch (npc.getId())
			{
				case LIZARDMEN:
				{
					if (killedLizardmen < 7)
					{
						killedLizardmen++;
						st.set("killed_" + LIZARDMEN, killedLizardmen);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				case VAKU_ORC:
				{
					if (killedVakuOrc < 5)
					{
						killedVakuOrc++;
						st.set("killed_" + VAKU_ORC, killedVakuOrc);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
				default:
				{
					if ((getQuestItemsCount(killer, BADGE) < 5) && getRandomBoolean())
					{
						giveItems(killer, BADGE, 1);
						playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
					break;
				}
			}
			
			if ((getQuestItemsCount(killer, BADGE) == 5) && (killedLizardmen == 7) && (killedVakuOrc == 5))
			{
				st.setCond(3, true);
			}
			sendNpcLogList(killer);
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@Override
	public Set<NpcLogListHolder> getNpcLogList(PlayerInstance activeChar)
	{
		final QuestState st = getQuestState(activeChar, false);
		if ((st != null) && st.isStarted() && st.isCond(2))
		{
			final Set<NpcLogListHolder> npcLogList = new HashSet<>(2);
			npcLogList.add(new NpcLogListHolder(LIZARDMEN, false, st.getInt("killed_" + LIZARDMEN)));
			npcLogList.add(new NpcLogListHolder(VAKU_ORC, false, st.getInt("killed_" + VAKU_ORC)));
			return npcLogList;
		}
		return super.getNpcLogList(activeChar);
	}
}