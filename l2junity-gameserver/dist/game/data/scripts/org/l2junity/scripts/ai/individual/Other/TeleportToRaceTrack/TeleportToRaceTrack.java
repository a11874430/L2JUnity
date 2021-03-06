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
package org.l2junity.scripts.ai.individual.Other.TeleportToRaceTrack;

import java.util.HashMap;
import java.util.Map;

import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.variables.PlayerVariables;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.scripting.annotations.GameScript;

import org.l2junity.scripts.ai.AbstractNpcAI;

/**
 * Monster Derby Track teleport AI.
 * @author Plim
 */
public final class TeleportToRaceTrack extends AbstractNpcAI
{
	// NPC
	private static final int RACE_MANAGER = 30995;
	// Locations
	private static final Location TELEPORT = new Location(12661, 181687, -3540);
	private static final Location DION_CASTLE_TOWN = new Location(15670, 142983, -2700);
	private static final Location[] RETURN_LOCATIONS =
	{
		new Location(-80826, 149775, -3043),
		new Location(-12672, 122776, -3116),
		new Location(15670, 142983, -2705),
		new Location(83400, 147943, -3404),
		new Location(111409, 219364, -3545),
		new Location(82956, 53162, -1495),
		new Location(146331, 25762, -2018),
		new Location(116819, 76994, -2714),
		new Location(43835, -47749, -792),
		new Location(147930, -55281, -2728),
		new Location(87386, -143246, -1293),
		new Location(12882, 181053, -3560)
	};
	// Misc
	private static final Map<Integer, Integer> TELEPORTERS = new HashMap<>();
	
	static
	{
		TELEPORTERS.put(30059, 2); // Trisha
		TELEPORTERS.put(30080, 3); // Clarissa
		TELEPORTERS.put(30177, 5); // Valentina
		TELEPORTERS.put(30233, 7); // Esmeralda
		TELEPORTERS.put(30256, 1); // Bella
		TELEPORTERS.put(30320, 0); // Richlin
		TELEPORTERS.put(30848, 6); // Elisa
		TELEPORTERS.put(30899, 4); // Flauen
		TELEPORTERS.put(31320, 8); // Ilyana
		TELEPORTERS.put(31275, 9); // Tatiana
		TELEPORTERS.put(31964, 10); // Bilia
	}
	
	private TeleportToRaceTrack()
	{
		addStartNpc(RACE_MANAGER);
		addStartNpc(TELEPORTERS.keySet());
		addTalkId(RACE_MANAGER);
		addTalkId(TELEPORTERS.keySet());
	}
	
	@Override
	public String onTalk(Npc npc, PlayerInstance player)
	{
		if (npc.getId() == RACE_MANAGER)
		{
			final int returnId = player.getVariables().getInt(PlayerVariables.MONSTER_RETURN, -1);
			
			if (returnId != -1)
			{
				player.teleToLocation(RETURN_LOCATIONS[returnId]);
				player.getVariables().remove(PlayerVariables.MONSTER_RETURN);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.IF_YOUR_MEANS_OF_ARRIVAL_WAS_A_BIT_UNCONVENTIONAL_THEN_I_LL_BE_SENDING_YOU_BACK_TO_THE_TOWN_OF_RUNE_WHICH_IS_THE_NEAREST_TOWN);
				player.teleToLocation(DION_CASTLE_TOWN);
			}
		}
		else
		{
			player.teleToLocation(TELEPORT);
			player.getVariables().set(PlayerVariables.MONSTER_RETURN, String.valueOf(TELEPORTERS.get(npc.getId())));
		}
		return super.onTalk(npc, player);
	}
	
	@GameScript
	public static void main()
	{
		new TeleportToRaceTrack();
	}
}
