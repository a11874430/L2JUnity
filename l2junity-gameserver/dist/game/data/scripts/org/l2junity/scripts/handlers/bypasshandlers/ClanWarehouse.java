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
package org.l2junity.scripts.handlers.bypasshandlers;

import org.l2junity.gameserver.handler.BypassHandler;
import org.l2junity.gameserver.handler.IBypassHandler;
import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.L2WarehouseInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.gameserver.network.client.send.WareHouseDepositList;
import org.l2junity.gameserver.network.client.send.WareHouseWithdrawalList;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.scripting.annotations.GameScript;

public class ClanWarehouse implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"withdrawc",
		"depositc"
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance activeChar, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		final Npc npc = (Npc) target;
		if (!(npc instanceof L2WarehouseInstance) && (npc.getClan() != null))
		{
			return false;
		}
		
		if (activeChar.hasItemRequest())
		{
			return false;
		}
		else if (activeChar.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
			return false;
		}
		else if (activeChar.getClan().getLevel() == 0)
		{
			activeChar.sendPacket(SystemMessageId.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_ABOVE_CAN_USE_A_CLAN_WAREHOUSE);
			return false;
		}
		else
		{
			try
			{
				if (command.toLowerCase().startsWith(COMMANDS[0])) // WithdrawC
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					
					if (!activeChar.hasClanPrivilege(ClanPrivilege.CL_VIEW_WAREHOUSE))
					{
						activeChar.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
						return true;
					}
					
					activeChar.setActiveWarehouse(activeChar.getClan().getWarehouse());
					
					if (activeChar.getActiveWarehouse().getSize() == 0)
					{
						activeChar.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
						return true;
					}
					
					for (ItemInstance i : activeChar.getActiveWarehouse().getItems())
					{
						if (i.isTimeLimitedItem() && (i.getRemainingTime() <= 0))
						{
							activeChar.getActiveWarehouse().destroyItem("L2ItemInstance", i, activeChar, null);
						}
					}
					
					activeChar.sendPacket(new WareHouseWithdrawalList(activeChar, WareHouseWithdrawalList.CLAN));
					return true;
				}
				else if (command.toLowerCase().startsWith(COMMANDS[1])) // DepositC
				{
					activeChar.sendPacket(ActionFailed.STATIC_PACKET);
					activeChar.setActiveWarehouse(activeChar.getClan().getWarehouse());
					activeChar.setInventoryBlockingStatus(true);
					activeChar.sendPacket(new WareHouseDepositList(activeChar, WareHouseDepositList.CLAN));
					return true;
				}
				
				return false;
			}
			catch (Exception e)
			{
				LOGGER.warn("Exception in " + getClass().getSimpleName(), e);
			}
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
	
	@GameScript
	public static void main()
	{
		BypassHandler.getInstance().registerHandler(new ClanWarehouse());
	}
}