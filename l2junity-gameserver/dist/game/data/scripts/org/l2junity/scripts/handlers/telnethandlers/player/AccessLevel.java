/*
 * Copyright (C) 2004-2013 L2J Unity
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
package org.l2junity.scripts.handlers.telnethandlers.player;

import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.telnet.ITelnetCommand;
import org.l2junity.gameserver.network.telnet.TelnetServer;
import org.l2junity.gameserver.scripting.annotations.GameScript;
import org.l2junity.gameserver.util.Util;

/**
 * @author UnAfraid
 */
public class AccessLevel implements ITelnetCommand
{
	private AccessLevel()
	{
	}
	
	@Override
	public String getCommand()
	{
		return "accesslevel";
	}
	
	@Override
	public String getUsage()
	{
		return "AccessLevel <player name> <access level>";
	}
	
	@Override
	public String handle(String ipAddress, String[] args)
	{
		if ((args.length < 2) || args[0].isEmpty() || !Util.isDigit(args[1]))
		{
			return null;
		}
		final PlayerInstance player = World.getInstance().getPlayer(args[0]);
		if (player != null)
		{
			final int level = Integer.parseInt(args[1]);
			player.setAccessLevel(level, true, true);
			return "Player " + player.getName() + "'s access level has been changed to: " + level;
		}
		return "Couldn't find player with such name.";
	}
	
	@GameScript
	public static void main()
	{
		TelnetServer.getInstance().addHandler(new AccessLevel());
	}
}
