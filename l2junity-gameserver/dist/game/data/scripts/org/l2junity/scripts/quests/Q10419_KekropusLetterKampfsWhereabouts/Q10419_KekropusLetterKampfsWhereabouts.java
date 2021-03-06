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
package org.l2junity.scripts.quests.Q10419_KekropusLetterKampfsWhereabouts;

import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.quest.QuestState;
import org.l2junity.gameserver.network.client.send.ExShowScreenMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;

import org.l2junity.scripts.quests.LetterQuest;

/**
 * Kekropus' Letter: Kampf's Whereabouts (10419)
 * @author St3eT
 */
public final class Q10419_KekropusLetterKampfsWhereabouts extends LetterQuest
{
	// NPCs
	private static final int ANDREI = 31292;
	private static final int HANSEN = 33853;
	private static final int INVISIBLE_NPC = 19543;
	// Items
	private static final int SOE_TOWN_OF_GODDARD = 37121; // Scroll of Escape: Town of Goddard
	private static final int SOE_VARKA_SILENOS_BARRACKS = 37034; // Scroll of Escape: Varka Silenos Barracks
	private static final int EWS = 959; // Scroll: Enchant Weapon (S-grade)
	// Location
	private static final Location TELEPORT_LOC = new Location(147491, -56633, -2776);
	// Misc
	private static final int MIN_LEVEL = 76;
	private static final int MAX_LEVEL = 80;
	
	public Q10419_KekropusLetterKampfsWhereabouts()
	{
		super(10419);
		addTalkId(ANDREI, HANSEN);
		addSeeCreatureId(INVISIBLE_NPC);
		
		setIsErtheiaQuest(false);
		setLevel(MIN_LEVEL, MAX_LEVEL);
		setStartQuestSound("Npcdialog1.kekrops_quest_9");
		setStartLocation(SOE_TOWN_OF_GODDARD, TELEPORT_LOC);
		registerQuestItems(SOE_TOWN_OF_GODDARD, SOE_VARKA_SILENOS_BARRACKS);
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
			case "31292-02.html":
			{
				htmltext = event;
				break;
			}
			case "31292-03.html":
			{
				if (st.isCond(1))
				{
					takeItems(player, SOE_TOWN_OF_GODDARD, -1);
					giveItems(player, SOE_VARKA_SILENOS_BARRACKS, 1);
					st.setCond(2, true);
					htmltext = event;
				}
				break;
			}
			case "33853-02.html":
			{
				if (st.isCond(2))
				{
					st.exitQuest(false, true);
					giveItems(player, EWS, 1);
					giveStoryQuestReward(npc, player);
					if (player.getLevel() >= MIN_LEVEL)
					{
						addExp(player, 1_277_640);
						addSp(player, 306);
					}
					showOnScreenMsg(player, NpcStringId.GROW_STRONGER_HERE_UNTIL_YOU_RECEIVE_THE_NEXT_LETTER_FROM_KEKROPUS_AT_LV_81, ExShowScreenMessage.TOP_CENTER, 6000);
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
		String htmltext = getNoQuestMsg(player);
		final QuestState st = getQuestState(player, false);
		
		if (st == null)
		{
			return htmltext;
		}
		
		if (st.isStarted())
		{
			if ((npc.getId() == ANDREI) && st.isCond(1))
			{
				htmltext = "31292-01.html";
			}
			else if (st.isCond(2))
			{
				htmltext = npc.getId() == ANDREI ? "31292-04.html" : "33853-01.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onSeeCreature(Npc npc, Creature creature, boolean isSummon)
	{
		if (creature.isPlayer())
		{
			final PlayerInstance player = creature.getActingPlayer();
			final QuestState st = getQuestState(player, false);
			
			if ((st != null) && st.isCond(2))
			{
				showOnScreenMsg(player, NpcStringId.VARKA_SILENOS_BARRACKS_IS_A_GOOD_HUNTING_ZONE_FOR_LV_76_OR_ABOVE, ExShowScreenMessage.TOP_CENTER, 6000);
			}
		}
		return super.onSeeCreature(npc, creature, isSummon);
	}
	
	@Override
	public boolean canShowTutorialMark(PlayerInstance player)
	{
		return !player.isInCategory(CategoryType.MAGE_GROUP);
	}
}