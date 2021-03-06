/*
 * Copyright (C) 2004-2015 L2J Unity
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
package org.l2junity.gameserver.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.l2junity.gameserver.config.PlayerConfig;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.olympiad.OlympiadGameManager;
import org.l2junity.gameserver.model.olympiad.OlympiadGameTask;
import org.l2junity.gameserver.model.skills.AbnormalType;
import org.l2junity.gameserver.model.skills.AbnormalVisualEffect;
import org.l2junity.gameserver.model.skills.BuffInfo;
import org.l2junity.gameserver.model.skills.EffectScope;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.skills.SkillBuffType;
import org.l2junity.gameserver.model.stats.BooleanStat;
import org.l2junity.gameserver.network.client.send.AbnormalStatusUpdate;
import org.l2junity.gameserver.network.client.send.ExAbnormalStatusUpdateFromTarget;
import org.l2junity.gameserver.network.client.send.ExOlympiadSpelledInfo;
import org.l2junity.gameserver.network.client.send.PartySpelled;
import org.l2junity.gameserver.network.client.send.ShortBuffStatusUpdate;

/**
 * Effect lists.<br>
 * Holds all the {@code BuffInfo}s that are affecting a creature.<br>
 * Manages the logic that controls whether a buff is added, remove, replaced or set inactive.<br>
 * Uses maps with skill ID as key and {@code BuffInfo} DTO as value to avoid iterations.<br>
 * Uses Double-Checked Locking to avoid useless initialization and synchronization issues and overhead.<br>
 * Methods may resemble List interface, although it doesn't implement such interface.
 * @author Zoey76
 */
public final class CharEffectList
{
	/** Queue containing all effects from buffs for this effect list. */
	private volatile Queue<BuffInfo> _actives;
	/** List containing all options for this effect list. They bypass most of the actions and they are not included in most operations. */
	private volatile Set<BuffInfo> _options;
	/** Map containing the all stacked effect in progress for each {@code AbnormalType}. */
	private volatile Set<AbnormalType> _stackedEffects = EnumSet.noneOf(AbnormalType.class);
	/** Set containing all {@code AbnormalType}s that shouldn't be added to this creature effect list. */
	private volatile Set<AbnormalType> _blockedAbnormalTypes = null;
	/** Set containing all abnormal visual effects this creature currently displays. */
	private volatile Set<AbnormalVisualEffect> _abnormalVisualEffects = EnumSet.noneOf(AbnormalVisualEffect.class);
	/** Short buff skill ID. */
	private BuffInfo _shortBuff = null;
	/** Count of specific types of buffs. */
	private final AtomicInteger _buffCount = new AtomicInteger();
	private final AtomicInteger _triggerBuffCount = new AtomicInteger();
	private final AtomicInteger _danceCount = new AtomicInteger();
	private final AtomicInteger _toggleCount = new AtomicInteger();
	private final AtomicInteger _debuffCount = new AtomicInteger();
	/** If {@code true} this effect list has buffs removed on any action. */
	private final AtomicInteger _hasBuffsRemovedOnAnyAction = new AtomicInteger();
	/** If {@code true} this effect list has buffs removed on damage. */
	private final AtomicInteger _hasBuffsRemovedOnDamage = new AtomicInteger();
	/** The owner of this effect list. */
	private final Creature _owner;
	/** Hidden buffs count, prevents iterations. */
	private final AtomicInteger _hiddenBuffs = new AtomicInteger();
	
	/**
	 * Constructor for effect list.
	 * @param owner the creature that owns this effect list
	 */
	public CharEffectList(Creature owner)
	{
		_owner = owner;
	}
	
	/**
	 * Gets option effects.
	 * @return an unmodifiable set containing all options.
	 */
	public Set<BuffInfo> getOptions()
	{
		return _options != null ? Collections.unmodifiableSet(_options) : Collections.emptySet();
	}
	
	/**
	 * Gets all the active effects on this effect list.
	 * @return an unmodifiable set containing all the active effects on this effect list
	 */
	public Collection<BuffInfo> getEffects()
	{
		return _actives != null ? Collections.unmodifiableCollection(_actives) : Collections.emptyList();
	}
	
	/**
	 * Gets all the active positive effects on this effect list.
	 * @return all the buffs on this effect list
	 */
	public List<BuffInfo> getBuffs()
	{
		return _actives != null ? _actives.stream().filter(b -> b.getSkill().getBuffType().equals(SkillBuffType.BUFF)).collect(Collectors.toList()) : Collections.emptyList();
	}
	
	/**
	 * Gets all the active negative effects on this effect list.
	 * @return all the debuffs on this effect list
	 */
	public List<BuffInfo> getDebuffs()
	{
		return _actives != null ? _actives.stream().filter(b -> b.getSkill().isDebuff()).collect(Collectors.toList()) : Collections.emptyList();
	}
	
	/**
	 * Verifies if this effect list contains the given skill ID.<br>
	 * @param skillId the skill ID to verify
	 * @return {@code true} if the skill ID is present in the effect list, {@code false} otherwise
	 */
	public boolean isAffectedBySkill(int skillId)
	{
		return ((_actives != null) && _actives.stream().anyMatch(i -> i.getSkill().getId() == skillId));
	}
	
	/**
	 * Gets the first {@code BuffInfo} found in this effect list.
	 * @param skillId the skill ID
	 * @return {@code BuffInfo} of the first active effect found.
	 */
	public BuffInfo getBuffInfoBySkillId(int skillId)
	{
		return (_actives != null ? _actives.stream() : Stream.<BuffInfo> empty()).filter(b -> b.getSkill().getId() == skillId).findFirst().orElse(null);
	}
	
	/**
	 * Check if any active {@code BuffInfo} of this {@code AbnormalType} exists.<br>
	 * @param type the abnormal skill type
	 * @return {@code true} if there is any {@code BuffInfo} matching the specified {@code AbnormalType}, {@code false} otherwise
	 */
	public final boolean hasAbnormalType(AbnormalType type)
	{
		return _stackedEffects.contains(type);
	}
	
	/**
	 * Check if any active {@code BuffInfo} of this {@code AbnormalType} exists.<br>
	 * @param types the abnormal skill type
	 * @return {@code true} if there is any {@code BuffInfo} matching one of the specified {@code AbnormalType}s, {@code false} otherwise
	 */
	public boolean hasAbnormalType(Collection<AbnormalType> types)
	{
		return _stackedEffects.stream().anyMatch(types::contains);
	}
	
	/**
	 * @param type the {@code AbnormalType} to match for.
	 * @param filter any additional filters to match for once a {@code BuffInfo} of this {@code AbnormalType} is found.
	 * @return {@code true} if there is any {@code BuffInfo} matching the specified {@code AbnormalType} and given filter, {@code false} otherwise
	 */
	public boolean hasAbnormalType(AbnormalType type, Predicate<BuffInfo> filter)
	{
		return hasAbnormalType(type) && _actives.stream().filter(i -> i.isAbnormalType(type)).anyMatch(filter);
	}
	
	/**
	 * Gets the first {@code BuffInfo} found by the given {@code AbnormalType}.<br>
	 * <font color="red">There are some cases where there are multiple {@code BuffInfo} per single {@code AbnormalType}</font>.
	 * @param type the abnormal skill type
	 * @return the {@code BuffInfo} if it's present, {@code null} otherwise
	 */
	public BuffInfo getFirstBuffInfoByAbnormalType(AbnormalType type)
	{
		return hasAbnormalType(type) ? _actives.stream().filter(i -> i.isAbnormalType(type)).findFirst().orElse(null) : null;
	}
	
	/**
	 * Adds {@code AbnormalType}s to the blocked buff slot set.
	 * @param blockedAbnormalTypes the blocked buff slot set to add
	 */
	public void addBlockedAbnormalTypes(Set<AbnormalType> blockedAbnormalTypes)
	{
		// Initialize
		if (_blockedAbnormalTypes == null)
		{
			synchronized (this)
			{
				if (_blockedAbnormalTypes == null)
				{
					_blockedAbnormalTypes = EnumSet.copyOf(blockedAbnormalTypes);
				}
			}
		}
		
		_blockedAbnormalTypes.addAll(blockedAbnormalTypes);
	}
	
	/**
	 * Removes {@code AbnormalType}s from the blocked buff slot set.
	 * @param blockedBuffSlots the blocked buff slot set to remove
	 * @return {@code true} if the blocked buff slots set has been modified, {@code false} otherwise
	 */
	public boolean removeBlockedAbnormalTypes(Set<AbnormalType> blockedBuffSlots)
	{
		return (_blockedAbnormalTypes != null) && _blockedAbnormalTypes.removeAll(blockedBuffSlots);
	}
	
	/**
	 * Gets all the blocked {@code AbnormalType}s for this creature effect list.
	 * @return the current blocked {@code AbnormalType}s set in unmodifiable view.
	 */
	public Set<AbnormalType> getBlockedAbnormalTypes()
	{
		return _blockedAbnormalTypes != null ? Collections.unmodifiableSet(_blockedAbnormalTypes) : Collections.emptySet();
	}
	
	/**
	 * Sets the Short Buff data and sends an update if the effected is a player.
	 * @param info the {@code BuffInfo}
	 */
	public void shortBuffStatusUpdate(BuffInfo info)
	{
		if (_owner.isPlayer())
		{
			_shortBuff = info;
			if (info == null)
			{
				_owner.sendPacket(ShortBuffStatusUpdate.RESET_SHORT_BUFF);
			}
			else
			{
				_owner.sendPacket(new ShortBuffStatusUpdate(info.getSkill().getId(), info.getSkill().getLevel(), info.getSkill().getSubLevel(), info.getTime()));
			}
		}
	}
	
	/**
	 * Gets the buffs count without including the hidden buffs (after getting an Herb buff).<br>
	 * Prevents initialization.
	 * @return the number of buffs in this creature effect list
	 */
	public int getBuffCount()
	{
		return _actives != null ? (_buffCount.intValue() - _hiddenBuffs.get()) : 0;
	}
	
	/**
	 * Gets the Songs/Dances count.<br>
	 * Prevents initialization.
	 * @return the number of Songs/Dances in this creature effect list
	 */
	public int getDanceCount()
	{
		return _danceCount.intValue();
	}
	
	/**
	 * Gets the triggered buffs count.<br>
	 * Prevents initialization.
	 * @return the number of triggered buffs in this creature effect list
	 */
	public int getTriggeredBuffCount()
	{
		return _triggerBuffCount.intValue();
	}
	
	/**
	 * Gets the toggled skills count.<br>
	 * Prevents initialization.
	 * @return the number of toggle skills in this creature effect list
	 */
	public int getToggleCount()
	{
		return _toggleCount.intValue();
	}
	
	/**
	 * Gets the debuff skills count.<br>
	 * Prevents initialization.
	 * @return the number of debuff effects in this creature effect list
	 */
	public int getDebuffCount()
	{
		return _debuffCount.intValue();
	}
	
	/**
	 * Gets the hidden buff count.
	 * @return the number of hidden buffs
	 */
	public int getHiddenBuffsCount()
	{
		return _hiddenBuffs.get();
	}
	
	/**
	 * Exits all effects in this effect list.<br>
	 * Stops all the effects, clear the effect lists and updates the effect flags and icons.
	 * @param broadcast {@code true} to broadcast update packets, {@code false} otherwise.
	 */
	public void stopAllEffects(boolean broadcast)
	{
		stopEffects(b -> !b.getSkill().isNecessaryToggle() && !b.getSkill().isIrreplacableBuff(), true, broadcast);
	}
	
	/**
	 * Stops all effects in this effect list except those that last through death.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		stopEffects(info -> !info.getSkill().isStayAfterDeath(), true, true);
	}
	
	/**
	 * Stops all active toggle skills.
	 */
	public void stopAllToggles()
	{
		if (getToggleCount() > 0)
		{
			// Ignore necessary toggles.
			stopEffects(b -> b.getSkill().isToggle() && !b.getSkill().isNecessaryToggle() && !b.getSkill().isIrreplacableBuff(), true, true);
		}
	}
	
	public void stopAllTogglesOfGroup(int toggleGroup)
	{
		if (getToggleCount() > 0)
		{
			stopEffects(b -> b.getSkill().isToggle() && (b.getSkill().getToggleGroupId() == toggleGroup), true, true);
		}
	}
	
	/**
	 * Stops all active dances/songs skills.
	 * @param update set to true to update the effect flags and icons
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void stopAllOptions(boolean update, boolean broadcast)
	{
		if (_options != null)
		{
			_options.forEach(this::remove);
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Exits all effects created by a specific skill ID.<br>
	 * Removes the effects from the effect list.<br>
	 * Removes the stats from the creature.<br>
	 * Updates the effect flags and icons.<br>
	 * Presents overload:<br>
	 * {@link #stopSkillEffects(boolean, Skill)}<br>
	 * @param removed {@code true} if the effect is removed, {@code false} otherwise
	 * @param skillId the skill ID
	 */
	public void stopSkillEffects(boolean removed, int skillId)
	{
		final BuffInfo info = getBuffInfoBySkillId(skillId);
		if (info != null)
		{
			remove(info, removed, true, true);
		}
	}
	
	/**
	 * Exits all effects created by a specific skill.<br>
	 * Removes the effects from the effect list.<br>
	 * Removes the stats from the creature.<br>
	 * Updates the effect flags and icons.<br>
	 * Presents overload:<br>
	 * {@link #stopSkillEffects(boolean, int)}<br>
	 * @param removed {@code true} if the effect is removed, {@code false} otherwise
	 * @param skill the skill
	 */
	public void stopSkillEffects(boolean removed, Skill skill)
	{
		// Check for existing abnormal type of the skill to make things quicker. None abnormal type should also work.
		if ((skill != null) && hasAbnormalType(skill.getAbnormalType()))
		{
			stopSkillEffects(removed, skill.getId());
		}
	}
	
	/**
	 * Exits all effects created by a specific skill {@code AbnormalType}.<br>
	 * <font color="red">This function should not be used recursively, because it updates on every execute.</font>
	 * @param type the skill {@code AbnormalType}
	 * @return {@code true} if there was any {@code BuffInfo} with the given {@code AbnormalType}, {@code false} otherwise
	 */
	public boolean stopEffects(AbnormalType type)
	{
		if (hasAbnormalType(type))
		{
			stopEffects(i -> i.isAbnormalType(type), true, true);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Exits all effects created by a specific skill {@code AbnormalType}s.<br>
	 * @param types the skill {@code AbnormalType}s to be checked and removed.
	 * @return {@code true} if there was any {@code BuffInfo} with one of the given {@code AbnormalType}s, {@code false} otherwise
	 */
	public boolean stopEffects(Collection<AbnormalType> types)
	{
		if (hasAbnormalType(types))
		{
			stopEffects(i -> types.contains(i.getSkill().getAbnormalType()), true, true);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Exits all effects matched by a specific filter.<br>
	 * @param filter any filter to apply when selecting which {@code BuffInfo}s to be removed.
	 * @param update update effect flags and icons after the operation finishes.
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void stopEffects(Predicate<BuffInfo> filter, boolean update, boolean broadcast)
	{
		if (_actives != null)
		{
			_actives.stream().filter(filter).forEach(this::remove);
			
			// Update stats, effect flags and icons.
			if (update)
			{
				updateEffectList(broadcast);
			}
		}
	}
	
	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set.<br>
	 * Called on any action except movement (attack, cast).
	 */
	public void stopEffectsOnAction()
	{
		if (_hasBuffsRemovedOnAnyAction.intValue() > 0)
		{
			stopEffects(info -> info.getSkill().isRemovedOnAnyActionExceptMove(), true, true);
		}
	}
	
	public void stopEffectsOnDamage()
	{
		if (_hasBuffsRemovedOnDamage.intValue() > 0)
		{
			stopEffects(info -> info.getSkill().isRemovedOnDamage(), true, true);
		}
	}
	
	/**
	 * Checks if a given effect limitation is exceeded.
	 * @param buffTypes the {@code SkillBuffType} of the skill.
	 * @return {@code true} if the current effect count for any of the given types is greater than the limit, {@code false} otherwise.
	 */
	private boolean isLimitExceeded(SkillBuffType... buffTypes)
	{
		for (SkillBuffType buffType : buffTypes)
		{
			switch (buffType)
			{
				case TRIGGER:
				{
					return (getTriggeredBuffCount() > PlayerConfig.TRIGGERED_BUFFS_MAX_AMOUNT);
				}
				case DANCE:
				{
					return (getDanceCount() > PlayerConfig.DANCES_MAX_AMOUNT);
				}
				// case TOGGLE: Do toggles have limit?
				case DEBUFF:
				{
					return (getDebuffCount() > 24);
				}
				case BUFF:
				{
					return (getBuffCount() > _owner.getStat().getMaxBuffCount());
				}
			}
		}
		
		return false;
	}
	
	/**
	 * @param info the {@code BuffInfo} whose buff category will be increased/decreased in count.
	 * @param increase {@code true} to increase the category count of this {@code BuffInfo}, {@code false} to decrease.
	 * @return the new count of the given {@code BuffInfo}'s category.
	 */
	private int increaseDecreaseCount(BuffInfo info, boolean increase)
	{
		// If it's a hidden buff, manage hidden buff count.
		if (!info.isInUse())
		{
			if (increase)
			{
				_hiddenBuffs.incrementAndGet();
			}
			else
			{
				_hiddenBuffs.decrementAndGet();
			}
		}
		
		// Update flag for skills being removed on action or damage.
		if (info.getSkill().isRemovedOnAnyActionExceptMove())
		{
			if (increase)
			{
				_hasBuffsRemovedOnAnyAction.incrementAndGet();
			}
			else
			{
				_hasBuffsRemovedOnAnyAction.decrementAndGet();
			}
		}
		if (info.getSkill().isRemovedOnDamage())
		{
			if (increase)
			{
				_hasBuffsRemovedOnDamage.incrementAndGet();
			}
			else
			{
				_hasBuffsRemovedOnDamage.decrementAndGet();
			}
		}
		
		// Increase specific buff count
		switch (info.getSkill().getBuffType())
		{
			case TRIGGER:
			{
				return increase ? _triggerBuffCount.incrementAndGet() : _triggerBuffCount.decrementAndGet();
			}
			case DANCE:
			{
				return increase ? _danceCount.incrementAndGet() : _danceCount.decrementAndGet();
			}
			case TOGGLE:
			{
				return increase ? _toggleCount.incrementAndGet() : _toggleCount.decrementAndGet();
			}
			case DEBUFF:
			{
				return increase ? _debuffCount.incrementAndGet() : _debuffCount.decrementAndGet();
			}
			case BUFF:
			{
				return increase ? _buffCount.incrementAndGet() : _buffCount.decrementAndGet();
			}
		}
		
		return 0;
	}
	
	/**
	 * Removes a set of effects from this effect list.<br>
	 * <font color="red">Does NOT update effect icons and flags. </font>
	 * @param info the effects to remove
	 */
	private void remove(BuffInfo info)
	{
		remove(info, true, false, false);
	}
	
	/**
	 * Removes a set of effects from this effect list.
	 * @param info the effects to remove
	 * @param removed {@code true} if the effect is removed, {@code false} otherwise
	 * @param update {@code true} if effect flags and icons should be updated after this removal, {@code false} otherwise.
	 * @param broadcast {@code true} to broadcast update packets if updating, {@code false} otherwise.
	 */
	public void remove(BuffInfo info, boolean removed, boolean update, boolean broadcast)
	{
		if (info == null)
		{
			return;
		}
		
		if (info.getOption() != null)
		{
			// Remove separately if its an option.
			removeOption(info, removed);
		}
		else if (info.getSkill().isPassive())
		{
			throw new IllegalAccessError("Passive skills should not get removed from CharEffectList as it shouldn't be added!");
		}
		else
		{
			// Remove active effect.
			removeActive(info, removed);
		}
		
		// Update stats, effect flags and icons.
		if (update)
		{
			updateEffectList(broadcast);
		}
	}
	
	/**
	 * @param info
	 * @param removed
	 */
	private void removeActive(BuffInfo info, boolean removed)
	{
		if (_actives != null)
		{
			// Removes the buff from the given effect list.
			_actives.remove(info);
			
			// Remove short buff.
			if (info == _shortBuff)
			{
				shortBuffStatusUpdate(null);
			}
			
			// Stop the buff effects.
			info.stopAllEffects(removed);
			
			// Decrease specific buff count
			increaseDecreaseCount(info, false);
			
			if (!removed)
			{
				info.getSkill().applyInstantEffects(EffectScope.END, info.getEffector(), info.getEffected(), info.getSkill(), info.getItem());
			}
		}
	}
	
	private void removeOption(BuffInfo info, boolean removed)
	{
		if (_options != null)
		{
			_options.remove(info);
			info.stopAllEffects(removed);
		}
	}
	
	/**
	 * Adds a set of effects to this effect list.
	 * @param info the {@code BuffInfo}
	 */
	public void add(BuffInfo info)
	{
		if (info == null)
		{
			return;
		}
		
		if (info.getSkill() == null)
		{
			// Only options are without skills.
			addOption(info);
		}
		else if (info.getSkill().isPassive())
		{
			throw new IllegalAccessError("Passive skills should not get added to CharEffectList!");
		}
		else
		{
			// Add active effect
			addActive(info);
		}
		
		// Update stats, effect flags and icons.
		updateEffectList(true);
	}
	
	private void addActive(BuffInfo info)
	{
		final Skill skill = info.getSkill();
		
		// Cannot add active buff to dead creature. Even in retail if you are dead with Lv. 3 Shillien's Breath, it will disappear instead of going 1 level down.
		if (info.getEffected().isDead())
		{
			return;
		}
		
		if ((_blockedAbnormalTypes != null) && _blockedAbnormalTypes.contains(skill.getAbnormalType()))
		{
			return;
		}
		
		if (info.getEffector() != null)
		{
			// Check for debuffs against target. NOTE: there are good debuffs which shouldn't be blocked. Like Celestial Protection's Lingering protection. Thats why we check for bad skills.
			// Check if effected is debuff blocked.
			if ((skill.getEffectPoint() <= 0) && skill.isDebuff() && (info.getEffected().isDebuffBlocked() || (info.getEffector().isGM() && !info.getEffector().getAccessLevel().canGiveDamage())))
			{
				return;
			}
			
			if (info.getEffector().isPlayer() && info.getEffected().isPlayer() && info.getEffected().getStat().has(BooleanStat.FACE_OFF) && (info.getEffected().getActingPlayer().getAttackerObjId() != info.getEffector().getObjectId()))
			{
				return;
			}
			
			// Check if buff skills are blocked.
			if (info.getEffected().getStat().has(BooleanStat.BLOCK_BUFF) && !skill.isDebuff())
			{
				return;
			}
		}
		
		// Initialize
		if (_actives == null)
		{
			synchronized (this)
			{
				if (_actives == null)
				{
					_actives = new ConcurrentLinkedQueue<>();
				}
			}
		}
		
		// Manage effect stacking.
		if (hasAbnormalType(skill.getAbnormalType()))
		{
			for (BuffInfo existingInfo : _actives)
			{
				final Skill existingSkill = existingInfo.getSkill();
				// Check if existing effect should be removed due to stack.
				// Effects with no abnormal don't stack if their ID is the same. Effects of the same abnormal type don't stack.
				if ((skill.getAbnormalType().isNone() && (existingSkill.getId() == skill.getId())) || (!skill.getAbnormalType().isNone() && (existingSkill.getAbnormalType() == skill.getAbnormalType())))
				{
					// Check if there is subordination abnormal. Skills with subordination abnormal stack with each other, unless the caster is the same.
					if (!skill.getSubordinationAbnormalType().isNone() && (skill.getSubordinationAbnormalType() == existingSkill.getSubordinationAbnormalType()))
					{
						if ((info.getEffectorObjectId() == 0) || (existingInfo.getEffectorObjectId() == 0) || (info.getEffectorObjectId() != existingInfo.getEffectorObjectId()))
						{
							continue;
						}
					}
					
					// The effect we are adding overrides the existing effect. Delete or disable the existing effect.
					if (skill.getAbnormalLvl() >= existingSkill.getAbnormalLvl())
					{
						// If it is an herb, set as not in use the lesser buff, unless it is the same skill.
						if ((skill.isAbnormalInstant() || existingSkill.isIrreplacableBuff()) && (skill.getId() != existingSkill.getId()))
						{
							existingInfo.setInUse(false);
							_hiddenBuffs.incrementAndGet();
						}
						else
						{
							// Remove effect that gets overriden.
							remove(existingInfo);
						}
					}
					else if (skill.isIrreplacableBuff()) // The effect we try to add should be hidden.
					{
						info.setInUse(false);
					}
					else // The effect we try to add should be overriden.
					{
						return;
					}
				}
			}
		}
		
		// Increase buff count.
		increaseDecreaseCount(info, true);
		
		// Check if any effect limit is exceeded.
		if (isLimitExceeded(SkillBuffType.values()))
		{
			// Check for each category.
			for (BuffInfo existingInfo : _actives)
			{
				if (existingInfo.isInUse() && !skill.is7Signs() && isLimitExceeded(existingInfo.getSkill().getBuffType()))
				{
					remove(existingInfo);
				}
				
				// Break further loops if there is no any other limit exceeding.
				if (!isLimitExceeded(SkillBuffType.values()))
				{
					break;
				}
			}
		}
		
		// After removing old buff (same ID) or stacked buff (same abnormal type),
		// Add the buff to the end of the effect list.
		_actives.add(info);
		// Initialize effects.
		info.initializeEffects();
	}
	
	private void addOption(BuffInfo info)
	{
		if (info.getOption() != null)
		{
			// Initialize
			if (_options == null)
			{
				synchronized (this)
				{
					if (_options == null)
					{
						_options = ConcurrentHashMap.newKeySet();
					}
				}
			}
			
			// Remove previous options of this id.
			_options.stream().filter(Objects::nonNull).filter(b -> b.getOption().getId() == info.getOption().getId()).forEach(b ->
			{
				b.setInUse(false);
				_options.remove(b);
			});
			
			_options.add(info);
			
			// Initialize effects.
			info.initializeEffects();
		}
	}
	
	/**
	 * Update effect icons.<br>
	 * Prevents initialization.
	 * @param partyOnly {@code true} only party icons need to be updated.
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		final PlayerInstance player = _owner.getActingPlayer();
		if (player != null)
		{
			final Party party = player.getParty();
			final Optional<AbnormalStatusUpdate> asu = (_owner.isPlayer() && !partyOnly) ? Optional.of(new AbnormalStatusUpdate()) : Optional.empty();
			final Optional<PartySpelled> ps = ((party != null) || _owner.isSummon()) ? Optional.of(new PartySpelled(_owner)) : Optional.empty();
			final Optional<ExOlympiadSpelledInfo> os = (player.isInOlympiadMode() && player.isOlympiadStart()) ? Optional.of(new ExOlympiadSpelledInfo(player)) : Optional.empty();
			
			if (_actives != null)
			{
				//@formatter:off
				_actives.stream()
					.filter(Objects::nonNull)
					.filter(BuffInfo::isInUse)
					.forEach(info ->
					{
						if (info.getSkill().isHealingPotionSkill())
						{
							shortBuffStatusUpdate(info);
						}
						else
						{
							asu.ifPresent(a -> a.addSkill(info));
							ps.filter(p -> !info.getSkill().isToggle()).ifPresent(p -> p.addSkill(info));
							os.ifPresent(o -> o.addSkill(info));
						}
					});
				//@formatter:on
			}
			
			// Send icon update for player buff bar.
			asu.ifPresent(_owner::sendPacket);
			
			// Player or summon is in party. Broadcast packet to everyone in the party.
			if (party != null)
			{
				ps.ifPresent(party::broadcastPacket);
			}
			else // Not in party, then its a summon info for its owner.
			{
				ps.ifPresent(player::sendPacket);
			}
			
			// Send icon update to all olympiad observers.
			if (os.isPresent())
			{
				final OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
				if ((game != null) && game.isBattleStarted())
				{
					os.ifPresent(game.getStadium()::broadcastPacketToObservers);
				}
			}
		}
		
		// Update effect icons for everyone targeting this owner.
		final ExAbnormalStatusUpdateFromTarget upd = new ExAbnormalStatusUpdateFromTarget(_owner);
		
		// @formatter:off
		_owner.getStatus().getStatusListener().stream()
			.filter(Objects::nonNull)
			.filter(WorldObject::isPlayer)
			.map(Creature::getActingPlayer)
			.forEach(upd::sendTo);
		// @formatter:on
		
		if (_owner.isPlayer() && (_owner.getTarget() == _owner))
		{
			_owner.sendPacket(upd);
		}
	}
	
	/**
	 * Gets the currently applied abnormal visual effects.
	 * @return the abnormal visual effects
	 */
	public Set<AbnormalVisualEffect> getCurrentAbnormalVisualEffects()
	{
		return _abnormalVisualEffects;
	}
	
	/**
	 * Checks if the creature has the abnormal visual effect.
	 * @param ave the abnormal visual effect
	 * @return {@code true} if the creature has the abnormal visual effect, {@code false} otherwise
	 */
	public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave)
	{
		return _abnormalVisualEffects.contains(ave);
	}
	
	/**
	 * Adds the abnormal visual and sends packet for updating them in client.
	 * @param aves the abnormal visual effects
	 */
	public final void startAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			_abnormalVisualEffects.add(ave);
		}
		_owner.updateAbnormalVisualEffects();
	}
	
	/**
	 * Removes the abnormal visual and sends packet for updating them in client.
	 * @param aves the abnormal visual effects
	 */
	public final void stopAbnormalVisualEffect(AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			_abnormalVisualEffects.remove(ave);
		}
		_owner.updateAbnormalVisualEffects();
	}
	
	/**
	 * Wrapper to update abnormal icons and effect flags.
	 * @param broadcast {@code true} sends update packets to observing players, {@code false} doesn't send any packets.
	 */
	private void updateEffectList(boolean broadcast)
	{
		// Create new empty flags.
		final Set<AbnormalType> abnormalTypeFlags = EnumSet.noneOf(AbnormalType.class);
		final Set<AbnormalVisualEffect> abnormalVisualEffectFlags = EnumSet.noneOf(AbnormalVisualEffect.class);
		final Set<BuffInfo> unhideBuffs = new HashSet<>();
		
		// Recalculate new flags
		if (_actives != null)
		{
			for (BuffInfo info : _actives)
			{
				if (info != null)
				{
					final Skill skill = info.getSkill();
					
					// Handle hidden buffs. Check if there was such abnormal before so we can continue.
					if ((getHiddenBuffsCount() > 0) && _stackedEffects.contains(skill.getAbnormalType()))
					{
						// If incoming buff isnt hidden, remove any hidden buffs with its abnormal type.
						if (info.isInUse())
						{
							unhideBuffs.removeIf(b -> b.isAbnormalType(skill.getAbnormalType()));
						}
						// If this incoming buff is hidden and its first of its abnormal, or it removes any previous hidden buff with the same or lower abnormal level and add this instead.
						else if (!abnormalTypeFlags.contains(skill.getAbnormalType()) || unhideBuffs.removeIf(b -> (b.isAbnormalType(skill.getAbnormalType())) && (b.getSkill().getAbnormalLvl() <= skill.getAbnormalLvl())))
						{
							unhideBuffs.add(info);
						}
					}
					
					// Add the AbnormalType flag.
					abnormalTypeFlags.add(skill.getAbnormalType());
					
					// Add AbnormalVisualEffect flag.
					if (skill.hasAbnormalVisualEffects())
					{
						abnormalVisualEffectFlags.addAll(skill.getAbnormalVisualEffects());
					}
				}
			}
		}
		
		// Replace the old flags with the new flags.
		_stackedEffects = abnormalTypeFlags;
		
		// Unhide the selected buffs.
		unhideBuffs.forEach(b ->
		{
			b.setInUse(true);
			_hiddenBuffs.decrementAndGet();
		});
		
		// Recalculate all stats
		if (!_owner.isPlayer() || _owner.asPlayer().isOnline())
		{
			_owner.getStat().recalculateStats(broadcast);
		}
		
		if (broadcast)
		{
			// Check if there is change in AbnormalVisualEffect
			if ((abnormalVisualEffectFlags.size() != _abnormalVisualEffects.size()) || !abnormalVisualEffectFlags.containsAll(_abnormalVisualEffects))
			{
				_abnormalVisualEffects = abnormalVisualEffectFlags;
				_owner.updateAbnormalVisualEffects();
			}
			
			// Send updates to the client
			updateEffectIcons(false);
		}
	}
}
