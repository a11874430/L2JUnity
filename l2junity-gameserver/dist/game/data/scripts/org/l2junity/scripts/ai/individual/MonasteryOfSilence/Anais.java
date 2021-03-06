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
package org.l2junity.scripts.ai.individual.MonasteryOfSilence;

import java.util.ArrayList;

import org.l2junity.gameserver.ai.CtrlIntention;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.holders.SkillHolder;
import org.l2junity.gameserver.model.quest.QuestTimer;
import org.l2junity.gameserver.scripting.annotations.GameScript;

import org.l2junity.scripts.ai.AbstractNpcAI;

/**
 * Anais AI.
 * @author nonom
 */
public final class Anais extends AbstractNpcAI
{
	// NPCs
	private static final int ANAIS = 25701;
	private static final int DIVINE_BURNER = 18915;
	private static final int GRAIL_WARD = 18929;
	// Skill
	private static SkillHolder DIVINE_NOVA = new SkillHolder(6326, 1);
	// Instances
	ArrayList<Npc> _divineBurners = new ArrayList<>(4);
	private PlayerInstance _nextTarget = null;
	private Npc _current = null;
	private int _pot = 0;
	
	private Anais()
	{
		addAttackId(ANAIS);
		addSpawnId(DIVINE_BURNER);
		addKillId(GRAIL_WARD);
	}
	
	private void burnerOnAttack(int pot, Npc anais)
	{
		Npc npc = _divineBurners.get(pot);
		npc.setState(1);
		npc.setIsRunning(false);
		if (pot < 4)
		{
			_current = npc;
			QuestTimer checkAround = getQuestTimer("CHECK", anais, null);
			if (checkAround == null) // || !checkAround.getIsActive()
			{
				startQuestTimer("CHECK", 3000, anais, null);
			}
		}
		else
		{
			cancelQuestTimer("CHECK", anais, null);
		}
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, PlayerInstance player)
	{
		switch (event)
		{
			case "CHECK":
			{
				if (!npc.isAttackingNow())
				{
					cancelQuestTimer("CHECK", npc, null);
				}
				if ((_current != null) || (_pot < 4))
				{
					final WorldObject target = npc.getTarget();
					_nextTarget = target instanceof PlayerInstance ? (PlayerInstance) target : null;
					final Npc b = _divineBurners.get(_pot);
					_pot = _pot + 1;
					b.setState(1);
					b.setIsRunning(false);
					Npc ward = addSpawn(GRAIL_WARD, new Location(b.getX(), b.getY(), b.getZ()), true, 0);
					((Attackable) ward).addDamageHate(_nextTarget, 0, 999);
					ward.setIsRunning(true);
					ward.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _nextTarget, null);
					startQuestTimer("GUARD_ATTACK", 1000, ward, _nextTarget, true);
					startQuestTimer("SUICIDE", 20000, ward, null);
					ward.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, _nextTarget);
				}
				break;
			}
			case "GUARD_ATTACK":
			{
				if (_nextTarget != null)
				{
					final double distance = npc.distance2d(_nextTarget);
					if (distance < 100)
					{
						npc.doCast(DIVINE_NOVA.getSkill());
					}
					else if (distance > 2000)
					{
						npc.doDie(null);
						cancelQuestTimer("GUARD_ATTACK", npc, player);
					}
				}
				break;
			}
			case "SUICIDE":
			{
				npc.doCast(DIVINE_NOVA.getSkill());
				cancelQuestTimer("GUARD_ATTACK", npc, _nextTarget);
				if (_current != null)
				{
					_current.setState(2);
					_current.setIsRunning(false);
					_current = null;
				}
				npc.doDie(null);
				break;
			}
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon)
	{
		if (_pot == 0)
		{
			burnerOnAttack(0, npc);
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxRecoverableHp() * 0.75)) && (_pot == 1))
		{
			burnerOnAttack(1, npc);
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxRecoverableHp() * 0.5)) && (_pot == 2))
		{
			burnerOnAttack(2, npc);
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxRecoverableHp() * 0.25)) && (_pot == 3))
		{
			burnerOnAttack(3, npc);
		}
		return super.onAttack(npc, attacker, damage, isSummon);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.l2junity.gameserver.model.quest.Quest#onSpawn(org.l2junity.gameserver.model.actor.L2Npc)
	 */
	@Override
	public String onSpawn(Npc npc)
	{
		_divineBurners.add(npc);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onKill(Npc npc, PlayerInstance killer, boolean isSummon)
	{
		npc.doCast(DIVINE_NOVA.getSkill());
		cancelQuestTimer("GUARD_ATTACK", npc, _nextTarget);
		cancelQuestTimer("CHECK", npc, null);
		if (_current != null)
		{
			_current.setState(2);
			_current.setIsRunning(false);
			_current = null;
		}
		return super.onKill(npc, killer, isSummon);
	}
	
	@GameScript
	public static void main()
	{
		new Anais();
	}
}
