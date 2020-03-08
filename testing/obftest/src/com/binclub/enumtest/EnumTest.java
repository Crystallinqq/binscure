package com.binclub.enumtest;

import com.binclub.Test;

/**
 * @author cookiedragon234 07/Mar/2020
 */
public class EnumTest implements Test {
	@Override
	public void execute() {
		StringBuilder sb = new StringBuilder();
		for (TestEnum testEnum : TestEnum.values()) {
			sb.append(testEnum.name());
		}
		
		if (!sb.toString().equals("ONETWOTHREEFOURFIVE")) {
			throw new AssertionError(sb.toString());
		}
		
		sb = new StringBuilder();
		for (TestEnum2 testEnum : TestEnum2.values()) {
			sb.append(testEnum.value);
		}
		
		if (!sb.toString().equals("12345")) {
			throw new AssertionError(sb.toString());
		}
	}
}
