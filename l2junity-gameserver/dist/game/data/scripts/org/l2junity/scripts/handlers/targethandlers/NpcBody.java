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
package org.l2junity.scripts.handlers.targethandlers;

import org.l2junity.gameserver.geodata.GeoData;
import org.l2junity.gameserver.handler.ITargetTypeHandler;
import org.l2junity.gameserver.handler.TargetHandler;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.scripting.annotations.SkillScript;

/**
 * Target dead monster.
 * @author Nik
 */
public class NpcBody implements ITargetTypeHandler
{
	@Override
	public WorldObject getTarget(Creature activeChar, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		if (selectedTarget == null)
		{
			return null;
		}
		
		if (!selectedTarget.isCreature())
		{
			return null;
		}
		
		if (!selectedTarget.isNpc())
		{
			if (sendMessage)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			
			return null;
		}
		
		Npc npc = (Npc) selectedTarget;
		
		if (npc.isDead())
		{
			// Check for cast range if character cannot move. TODO: char will start follow until within castrange, but if his moving is blocked by geodata, this msg will be sent.
			if (dontMove)
			{
				if (activeChar.distance2d(npc) > skill.getCastRange())
				{
					if (sendMessage)
					{
						activeChar.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
					}
					
					return null;
				}
			}
			
			// Geodata check when character is within range.
			if (!GeoData.getInstance().canSeeTarget(activeChar, npc))
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				}
				
				return null;
			}
			
			return npc;
		}
		
		// If target is not dead or not player/pet it will not even bother to walk within range, unlike Enemy target type.
		if (sendMessage)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
		
		return null;
	}
	
	@SkillScript
	public static void main()
	{
		TargetHandler.getInstance().registerTargetTypeHandler("NPC_BODY", new NpcBody());
	}
}