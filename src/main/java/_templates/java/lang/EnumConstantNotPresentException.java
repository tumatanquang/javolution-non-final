/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.java.lang;
public class EnumConstantNotPresentException extends RuntimeException {
	private final Class _enumType;
	private final String _constantName;
	public EnumConstantNotPresentException(Class enumType,
			String constantName) {
		super(enumType.getName() + "." + constantName);
		_enumType = enumType;
		_constantName = constantName;
	}
	public Class enumType() {
		return _enumType;
	}
	public String constantName() {
		return _constantName;
	}
}