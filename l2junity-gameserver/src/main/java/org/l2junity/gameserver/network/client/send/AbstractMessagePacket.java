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
package org.l2junity.gameserver.network.client.send;

import java.io.PrintStream;
import java.util.Arrays;

import org.l2junity.gameserver.datatables.ItemTable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.actor.instance.DoorInstance;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2junity.gameserver.model.items.L2Item;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.network.PacketWriter;

/**
 * @param <T>
 * @author UnAfraid
 */
@SuppressWarnings("unchecked")
public abstract class AbstractMessagePacket<T extends AbstractMessagePacket<?>> implements IClientOutgoingPacket
{
	private static final SMParam[] EMPTY_PARAM_ARRAY = new SMParam[0];
	
	private static final class SMParam
	{
		private final byte _type;
		private final Object _value;
		
		public SMParam(final byte type, final Object value)
		{
			_type = type;
			_value = value;
		}
		
		public final byte getType()
		{
			return _type;
		}
		
		public final String getStringValue()
		{
			return (String) _value;
		}
		
		public final int getIntValue()
		{
			return ((Integer) _value).intValue();
		}
		
		public final long getLongValue()
		{
			return ((Long) _value).longValue();
		}
		
		public final int[] getIntArrayValue()
		{
			return (int[]) _value;
		}
	}
	
	// id 22 d (shared with 1-3,17,22
	// id 21 h
	// id 20 c
	// id 19 c
	// id 18 Q (read same as 6)
	// id 17 shared with 1-3,17,22
	private static final byte TYPE_BYTE = 20;
	private static final byte TYPE_POPUP_ID = 16;
	private static final byte TYPE_CLASS_ID = 15;
	// id 14 dSSSSS
	private static final byte TYPE_SYSTEM_STRING = 13;
	private static final byte TYPE_PLAYER_NAME = 12;
	private static final byte TYPE_DOOR_NAME = 11;
	private static final byte TYPE_INSTANCE_NAME = 10;
	private static final byte TYPE_ELEMENT_NAME = 9;
	// id 8 - ddd
	private static final byte TYPE_ZONE_NAME = 7;
	private static final byte TYPE_LONG_NUMBER = 6;
	private static final byte TYPE_CASTLE_NAME = 5;
	private static final byte TYPE_SKILL_NAME = 4;
	private static final byte TYPE_ITEM_NAME = 3;
	private static final byte TYPE_NPC_NAME = 2;
	private static final byte TYPE_INT_NUMBER = 1;
	private static final byte TYPE_TEXT = 0;
	
	private SMParam[] _params;
	private final SystemMessageId _smId;
	private int _paramIndex;
	
	public AbstractMessagePacket(SystemMessageId smId)
	{
		if (smId == null)
		{
			throw new NullPointerException("SystemMessageId cannot be null!");
		}
		_smId = smId;
		_params = smId.getParamCount() > 0 ? new SMParam[smId.getParamCount()] : EMPTY_PARAM_ARRAY;
	}
	
	public final int getId()
	{
		return _smId.getId();
	}
	
	public final SystemMessageId getSystemMessageId()
	{
		return _smId;
	}
	
	private void append(SMParam param)
	{
		if (_paramIndex >= _params.length)
		{
			_params = Arrays.copyOf(_params, _paramIndex + 1);
			_smId.setParamCount(_paramIndex + 1);
			LOGGER.info("Wrong parameter count '" + (_paramIndex + 1) + "' for SystemMessageId: " + _smId);
		}
		
		_params[_paramIndex++] = param;
	}
	
	public final T addString(final String text)
	{
		append(new SMParam(TYPE_TEXT, text));
		return (T) this;
	}
	
	/**
	 * Appends a Castle name parameter type, the name will be read from CastleName-e.dat.<br>
	 * <ul>
	 * <li>1-9 Castle names</li>
	 * <li>21 Fortress of Resistance</li>
	 * <li>22-33 Clan Hall names</li>
	 * <li>34 Devastated Castle</li>
	 * <li>35 Bandit Stronghold</li>
	 * <li>36-61 Clan Hall names</li>
	 * <li>62 Rainbow Springs</li>
	 * <li>63 Wild Beast Reserve</li>
	 * <li>64 Fortress of the Dead</li>
	 * <li>81-89 Territory names</li>
	 * <li>90-100 null</li>
	 * <li>101-121 Fortress names</li>
	 * </ul>
	 * @param number the conquerable entity
	 * @return the system message with the proper parameter
	 */
	public final T addCastleId(final int number)
	{
		append(new SMParam(TYPE_CASTLE_NAME, number));
		return (T) this;
	}
	
	public final T addInt(final int number)
	{
		append(new SMParam(TYPE_INT_NUMBER, number));
		return (T) this;
	}
	
	public final T addLong(final long number)
	{
		append(new SMParam(TYPE_LONG_NUMBER, number));
		return (T) this;
	}
	
	public final T addCharName(final Creature cha)
	{
		if (cha.isNpc())
		{
			final Npc npc = (Npc) cha;
			if (npc.getTemplate().isUsingServerSideName())
			{
				return addString(npc.getTemplate().getName());
			}
			return addNpcName(npc);
		}
		else if (cha.isPlayer())
		{
			return addPcName(cha.getActingPlayer());
		}
		else if (cha.isSummon())
		{
			final Summon summon = (Summon) cha;
			if (summon.getTemplate().isUsingServerSideName())
			{
				return addString(summon.getTemplate().getName());
			}
			return addNpcName(summon);
		}
		else if (cha.isDoor())
		{
			final DoorInstance door = (DoorInstance) cha;
			return addDoorName(door.getId());
		}
		return addString(cha.getName());
	}
	
	public final T addPcName(final PlayerInstance pc)
	{
		append(new SMParam(TYPE_PLAYER_NAME, pc.getAppearance().getVisibleName()));
		return (T) this;
	}
	
	/**
	 * ID from doorData.xml
	 * @param doorId
	 * @return
	 */
	public final T addDoorName(int doorId)
	{
		append(new SMParam(TYPE_DOOR_NAME, doorId));
		return (T) this;
	}
	
	public final T addNpcName(Npc npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	public final T addNpcName(final Summon npc)
	{
		return addNpcName(npc.getId());
	}
	
	public final T addNpcName(final L2NpcTemplate template)
	{
		if (template.isUsingServerSideName())
		{
			return addString(template.getName());
		}
		return addNpcName(template.getId());
	}
	
	public final T addNpcName(final int id)
	{
		append(new SMParam(TYPE_NPC_NAME, 1000000 + id));
		return (T) this;
	}
	
	public T addItemName(final ItemInstance item)
	{
		return addItemName(item.getId());
	}
	
	public T addItemName(final L2Item item)
	{
		return addItemName(item.getId());
	}
	
	public final T addItemName(final int id)
	{
		final L2Item item = ItemTable.getInstance().getTemplate(id);
		if (item.getDisplayId() != id)
		{
			return addString(item.getName());
		}
		
		append(new SMParam(TYPE_ITEM_NAME, id));
		return (T) this;
	}
	
	public final T addZoneName(final double x, final double y, final double z)
	{
		append(new SMParam(TYPE_ZONE_NAME, new int[]
		{
			(int) x,
			(int) y,
			(int) z
		}));
		return (T) this;
	}
	
	public final T addSkillName(final Skill skill)
	{
		if (skill.getId() != skill.getDisplayId())
		{
			return addString(skill.getName());
		}
		return addSkillName(skill.getId(), skill.getLevel(), skill.getSubLevel());
	}
	
	public final T addSkillName(final int id)
	{
		return addSkillName(id, 1, 0);
	}
	
	public final T addSkillName(final int id, final int lvl, final int subLvl)
	{
		append(new SMParam(TYPE_SKILL_NAME, new int[]
		{
			id,
			lvl,
			subLvl
		}));
		return (T) this;
	}
	
	/**
	 * Elemental name - 0(Fire) ...
	 * @param type
	 * @return
	 */
	public final T addAttribute(final int type)
	{
		append(new SMParam(TYPE_ELEMENT_NAME, type));
		return (T) this;
	}
	
	/**
	 * ID from sysstring-e.dat
	 * @param type
	 * @return
	 */
	public final T addSystemString(final int type)
	{
		append(new SMParam(TYPE_SYSTEM_STRING, type));
		return (T) this;
	}
	
	/**
	 * ID from ClassInfo-e.dat
	 * @param type
	 * @return
	 */
	public final T addClassId(final int type)
	{
		append(new SMParam(TYPE_CLASS_ID, type));
		return (T) this;
	}
	
	public final T addPopup(int target, int attacker, int damage)
	{
		append(new SMParam(TYPE_POPUP_ID, new int[]
		{
			target,
			attacker,
			damage
		}));
		return (T) this;
	}
	
	public final T addByte(int time)
	{
		append(new SMParam(TYPE_BYTE, time));
		return (T) this;
	}
	
	/**
	 * Instance name from instantzonedata-e.dat
	 * @param type id of instance
	 * @return
	 */
	public final T addInstanceName(final int type)
	{
		append(new SMParam(TYPE_INSTANCE_NAME, type));
		return (T) this;
	}
	
	protected void writeParamsSize(PacketWriter packet, int size)
	{
		packet.writeC(size);
	}
	
	protected void writeParamType(PacketWriter packet, int type)
	{
		packet.writeC(type);
	}
	
	protected final void writeMe(PacketWriter packet)
	{
		writeParamsSize(packet, _params.length);
		SMParam param;
		for (int i = 0; i < _paramIndex; i++)
		{
			param = _params[i];
			
			writeParamType(packet, param.getType());
			switch (param.getType())
			{
				case TYPE_ELEMENT_NAME:
				case TYPE_BYTE:
				{
					packet.writeC(param.getIntValue());
					break;
				}
				
				case TYPE_CASTLE_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_CLASS_ID:
				{
					packet.writeH(param.getIntValue());
					break;
				}
				
				case TYPE_ITEM_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_DOOR_NAME:
				{
					packet.writeD(param.getIntValue());
					break;
				}
				
				case TYPE_LONG_NUMBER:
				{
					packet.writeQ(param.getLongValue());
					break;
				}
				
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					packet.writeS(param.getStringValue());
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					packet.writeD(array[0]); // skill id
					packet.writeH(array[1]); // skill level
					packet.writeH(array[2]); // skill sub level
					break;
				}
				
				case TYPE_POPUP_ID:
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					packet.writeD(array[0]); // x
					packet.writeD(array[1]); // y
					packet.writeD(array[2]); // z
					break;
				}
			}
		}
	}
	
	public final void printMe(PrintStream out)
	{
		out.println(0x62);
		
		out.println(getId());
		out.println(_params.length);
		
		for (SMParam param : _params)
		{
			switch (param.getType())
			{
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					out.println(param.getStringValue());
					break;
				}
				
				case TYPE_LONG_NUMBER:
				{
					out.println(param.getLongValue());
					break;
				}
				
				case TYPE_ITEM_NAME:
				case TYPE_CASTLE_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_ELEMENT_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_DOOR_NAME:
				case TYPE_CLASS_ID:
				{
					out.println(param.getIntValue());
					break;
				}
				
				case TYPE_POPUP_ID:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]); // Target
					out.println(array[1]); // Attacker
					out.println(array[2]); // Value
					break;
				}
				
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]); // SkillId
					out.println(array[1]); // SkillLevel
					out.println(array[2]); // SkillSubLevel
					break;
				}
				
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					out.println(array[0]); // x
					out.println(array[1]); // y
					out.println(array[2]); // z
					break;
				}
			}
		}
	}
}
