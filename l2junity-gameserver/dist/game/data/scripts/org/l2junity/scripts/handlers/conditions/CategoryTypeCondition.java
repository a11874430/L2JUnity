/*
 * Copyright (C) 2004-2016 L2J Unity
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
package org.l2junity.scripts.handlers.conditions;

import java.util.List;

import org.l2junity.gameserver.enums.CategoryType;
import org.l2junity.gameserver.handler.ConditionHandler;
import org.l2junity.gameserver.handler.IConditionHandler;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.scripting.annotations.ConditionScript;

/**
 * @author Sdw
 */
public class CategoryTypeCondition implements IConditionHandler
{
	private final List<CategoryType> _categoryTypes;
	
	public CategoryTypeCondition(StatsSet params)
	{
		_categoryTypes = params.getEnumList("category", CategoryType.class);
	}
	
	@Override
	public boolean test(Creature creature, WorldObject target)
	{
		return _categoryTypes.stream().anyMatch(creature::isInCategory);
	}
	
	@ConditionScript
	public static void main()
	{
		ConditionHandler.getInstance().registerHandler("CategoryType", CategoryTypeCondition::new);
	}
}
