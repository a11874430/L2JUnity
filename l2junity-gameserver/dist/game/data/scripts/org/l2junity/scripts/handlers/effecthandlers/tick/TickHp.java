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
package org.l2junity.scripts.handlers.effecthandlers.tick;

import org.l2junity.gameserver.enums.StatModifierType;
import org.l2junity.gameserver.handler.EffectHandler;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.scripting.annotations.SkillScript;

/**
 * Dam Over Time effect implementation.
 */
public final class TickHp extends AbstractEffect
{
	private final double _power;
	private final StatModifierType _mode;
	
	public TickHp(StatsSet params)
	{
		_power = params.getDouble("power");
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
		setTicks(params.getInt("ticks"));
	}
	
	@Override
	public void pumpStart(Creature caster, Creature target, Skill skill)
	{
		if (skill.isMagic())
		{
			// TODO: M.Crit can occur even if this skill is resisted. Only then m.crit damage is applied and not debuff
			final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), caster, target, skill);
			if (mcrit)
			{
				double damage = _power * getTicksMultiplier() * 10; // Tests show that 10 times HP DOT is taken during magic critical.
				if (damage < 0)
				{
					damage = Math.abs(damage);
					if (damage >= (target.getCurrentHp() - 1))
					{
						damage = target.getCurrentHp() - 1;
					}
					
					caster.doAttack(damage, target, skill, true, false, false, false);
				}
				else
				{
					final double maxHp = target.getMaxRecoverableHp();
					if (target.getCurrentHp() > maxHp)
					{
						return;
					}
					
					target.setCurrentHp(Math.min(target.getCurrentHp() + damage, maxHp));
				}
			}
		}
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.DMG_OVER_TIME;
	}
	
	@Override
	public void tick(Creature caster, Creature target, Skill skill)
	{
		if (target.isDead())
		{
			return;
		}
		
		double damage = 0;
		double hp = target.getCurrentHp();
		switch (_mode)
		{
			case DIFF:
			{
				damage = _power * getTicksMultiplier();
				break;
			}
			case PER:
			{
				damage = hp * _power * getTicksMultiplier();
				break;
			}
		}
		
		if (damage < 0)
		{
			damage = Math.abs(damage);
			if (damage >= (target.getCurrentHp() - 1))
			{
				damage = target.getCurrentHp() - 1;
			}
			
			caster.doAttack(damage, target, skill, true, false, false, false);
		}
		else
		{
			final double maxHp = target.getMaxRecoverableHp();
			if (target.getCurrentHp() > maxHp)
			{
				return;
			}
			
			target.setCurrentHp(Math.min(target.getCurrentHp() + damage, maxHp));
		}
	}
	
	@SkillScript
	public static void main()
	{
		EffectHandler.getInstance().registerHandler("t_hp", TickHp::new);
	}
}
