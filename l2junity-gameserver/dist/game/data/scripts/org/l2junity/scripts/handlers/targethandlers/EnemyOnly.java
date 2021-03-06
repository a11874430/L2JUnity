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
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.scripting.annotations.SkillScript;

/**
 * Target only enemy no matter if force attacking or not.
 * @author Nik
 */
public class EnemyOnly implements ITargetTypeHandler
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
		
		final Creature target = (Creature) selectedTarget;
		
		// You cannot attack yourself even with force.
		if (activeChar == target)
		{
			if (sendMessage)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			
			return null;
		}
		
		// You cannot attack dead targets.
		if (target.isDead())
		{
			if (sendMessage)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			
			return null;
		}
		
		// Doors do not care about force attack.
		if (target.isDoor() && !target.isAutoAttackable(activeChar))
		{
			if (sendMessage)
			{
				activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			}
			
			return null;
		}
		
		// Monsters can attack/be attacked anywhere. Players can attack creatures that aren't autoattackable with force attack.
		if (target.isAutoAttackable(activeChar))
		{
			// Check for cast range if character cannot move. TODO: char will start follow until within castrange, but if his moving is blocked by geodata, this msg will be sent.
			if (dontMove)
			{
				if (activeChar.distance3d(target) > skill.getCastRange())
				{
					if (sendMessage)
					{
						activeChar.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
					}
					
					return null;
				}
			}
			
			// Geodata check when character is within range.
			if (!GeoData.getInstance().canSeeTarget(activeChar, target))
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				}
				
				return null;
			}
			
			// Skills with this target type cannot be used by playables on playables in peace zone, but can be used by and on NPCs.
			if (target.isInsidePeaceZone(activeChar))
			{
				if (sendMessage)
				{
					activeChar.sendPacket(SystemMessageId.YOU_CANNOT_USE_SKILLS_THAT_MAY_HARM_OTHER_PLAYERS_IN_HERE);
				}
				
				return null;
			}
			
			// Is this check still actual?
			if ((target.getActingPlayer() != null) && (activeChar.getActingPlayer() != null))
			{
				if ((activeChar.getActingPlayer().getSiegeState() > 0) && activeChar.isInsideZone(ZoneId.SIEGE) && (target.getActingPlayer().getSiegeState() == activeChar.getActingPlayer().getSiegeState()) && (target.getActingPlayer() != activeChar.getActingPlayer()) && (target.getActingPlayer().getSiegeSide() == activeChar.getActingPlayer().getSiegeSide()))
				{
					if (sendMessage)
					{
						activeChar.sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
					}
					
					return null;
				}
			}
			
			return target;
		}
		
		if (sendMessage)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
		
		return null;
	}
	
	@SkillScript
	public static void main()
	{
		TargetHandler.getInstance().registerTargetTypeHandler("ENEMY_ONLY", new EnemyOnly());
	}
}