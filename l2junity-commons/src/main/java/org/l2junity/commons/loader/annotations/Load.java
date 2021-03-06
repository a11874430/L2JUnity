package org.l2junity.commons.loader.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.l2junity.commons.loader.ILoadGroup;

/**
 * @author NosKun
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Load
{
	Class<? extends ILoadGroup> group();
	
	Dependency[] dependencies() default {};
}
