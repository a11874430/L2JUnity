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
package org.l2junity.gameserver.model.eventengine;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.eventengine.drop.IEventDrop;
import org.l2junity.gameserver.model.events.AbstractScript;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.ListenerRegisterType;
import org.l2junity.gameserver.model.events.annotations.RegisterEvent;
import org.l2junity.gameserver.model.events.annotations.RegisterType;
import org.l2junity.gameserver.model.events.impl.character.player.OnPlayerLogout;

/**
 * @author UnAfraid
 * @param <T>
 */
public abstract class AbstractEventManager<T extends AbstractEvent<?>> extends AbstractScript
{
	private String _name;
	private volatile StatsSet _variables = StatsSet.EMPTY_STATSET;
	private volatile Set<EventScheduler> _schedulers = Collections.emptySet();
	private volatile Set<IConditionalEventScheduler> _conditionalSchedulers = Collections.emptySet();
	private volatile Map<String, IEventDrop> _rewards = Collections.emptyMap();
	
	private final Set<T> _events = ConcurrentHashMap.newKeySet();
	private final Queue<PlayerInstance> _registeredPlayers = new ConcurrentLinkedDeque<>();
	private final AtomicReference<IEventState> _state = new AtomicReference<>();
	
	public abstract void onInitialized();
	
	/* ********************** */
	
	public String getName()
	{
		return _name;
	}
	
	public void setName(String name)
	{
		_name = name;
	}
	
	/* ********************** */
	
	public StatsSet getVariables()
	{
		return _variables;
	}
	
	public void setVariables(StatsSet variables)
	{
		_variables = new StatsSet(Collections.unmodifiableMap(variables.getSet()));
	}
	
	/* ********************** */
	
	public EventScheduler getScheduler(String name)
	{
		return _schedulers.stream().filter(scheduler -> scheduler.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	public void setSchedulers(Set<EventScheduler> schedulers)
	{
		_schedulers = Collections.unmodifiableSet(schedulers);
	}
	
	/* ********************** */
	
	public Set<IConditionalEventScheduler> getConditionalSchedulers()
	{
		return _conditionalSchedulers;
	}
	
	public void setConditionalSchedulers(Set<IConditionalEventScheduler> schedulers)
	{
		_conditionalSchedulers = Collections.unmodifiableSet(schedulers);
	}
	
	/* ********************** */
	
	public IEventDrop getRewards(String name)
	{
		return _rewards.get(name);
	}
	
	public void setRewards(Map<String, IEventDrop> rewards)
	{
		_rewards = Collections.unmodifiableMap(rewards);
	}
	
	/* ********************** */
	
	public Set<T> getEvents()
	{
		return _events;
	}
	
	/* ********************** */
	
	public void startScheduler()
	{
		_schedulers.forEach(EventScheduler::startScheduler);
	}
	
	public void stopScheduler()
	{
		_schedulers.forEach(EventScheduler::stopScheduler);
	}
	
	public void startConditionalSchedulers()
	{
		//@formatter:off
		_conditionalSchedulers.stream()
			.filter(IConditionalEventScheduler::test)
			.forEach(IConditionalEventScheduler::run);
		//@formatter:on
	}
	
	/* ********************** */
	
	public IEventState getState()
	{
		return _state.get();
	}
	
	public void setState(IEventState newState)
	{
		final IEventState previousState = _state.get();
		_state.set(newState);
		onStateChange(previousState, newState);
	}
	
	public boolean setState(IEventState previousState, IEventState newState)
	{
		if (_state.compareAndSet(previousState, newState))
		{
			onStateChange(previousState, newState);
			return true;
		}
		return false;
	}
	
	/* ********************** */
	
	public final boolean registerPlayer(PlayerInstance player)
	{
		return canRegister(player, true) && _registeredPlayers.offer(player);
	}
	
	public final boolean unregisterPlayer(PlayerInstance player)
	{
		return _registeredPlayers.remove(player);
	}
	
	public final boolean isRegistered(PlayerInstance player)
	{
		return _registeredPlayers.contains(player);
	}
	
	public boolean canRegister(PlayerInstance player, boolean sendMessage)
	{
		return !_registeredPlayers.contains(player);
	}
	
	public final Queue<PlayerInstance> getRegisteredPlayers()
	{
		return _registeredPlayers;
	}
	
	/* ********************** */
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL)
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final PlayerInstance player = event.getActiveChar();
		if (_registeredPlayers.remove(player))
		{
			onUnregisteredPlayer(player);
		}
	}
	
	/* ********************** */
	
	/**
	 * Triggered when a player is automatically removed from the event manager because he disconnected
	 * @param player
	 */
	protected void onUnregisteredPlayer(PlayerInstance player)
	{
		
	}
	
	/**
	 * Triggered when state is changed
	 * @param previousState
	 * @param newState
	 */
	protected void onStateChange(IEventState previousState, IEventState newState)
	{
		
	}
	
	/* ********************** */
	
	@Override
	public String getScriptName()
	{
		return getClass().getSimpleName();
	}
}
