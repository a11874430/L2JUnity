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
package org.l2junity.gameserver.network.client.send.shuttle;

import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.OutgoingPackets;
import org.l2junity.gameserver.network.client.send.IClientOutgoingPacket;
import org.l2junity.network.PacketWriter;

/**
 * @author UnAfraid
 */
public class ExValidateLocationInShuttle implements IClientOutgoingPacket
{
	private final PlayerInstance _activeChar;
	private final int _shipId, _heading;
	private final Location _loc;
	
	public ExValidateLocationInShuttle(PlayerInstance player)
	{
		_activeChar = player;
		_shipId = _activeChar.getShuttle().getObjectId();
		_loc = player.getInVehiclePosition();
		_heading = player.getHeading();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_VALIDATE_LOCATION_IN_SHUTTLE.writeId(packet);
		
		packet.writeD(_activeChar.getObjectId());
		packet.writeD(_shipId);
		packet.writeD((int) _loc.getX());
		packet.writeD((int) _loc.getY());
		packet.writeD((int) _loc.getZ());
		packet.writeD(_heading);
		return true;
	}
}
