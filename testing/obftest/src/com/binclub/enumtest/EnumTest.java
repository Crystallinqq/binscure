package com.binclub.enumtest;

import com.binclub.Test;

import java.util.Objects;

/**
 * @author cookiedragon234 07/Mar/2020
 */
public class EnumTest implements Test {
	@Override
	public void execute() {
		StringBuilder sb = new StringBuilder();
		for (TestEnum testEnum : TestEnum.values()) {
			sb.append(testEnum.name());
			
			try {
				Objects.requireNonNull(TestEnum.valueOf(testEnum.name()));
			} catch (Throwable t) {
				throw new AssertionError(t);
			}
		}
		
		if (!sb.toString().equals("ONETWOTHREEFOURFIVE")) {
			throw new AssertionError(sb.toString());
		}
		
		sb = new StringBuilder();
		for (TestEnum2 testEnum : TestEnum2.values()) {
			sb.append(testEnum.value);
			
			try {
				Objects.requireNonNull(TestEnum2.valueOf(testEnum.name()));
			} catch (Throwable t) {
				throw new AssertionError(t);
			}
		}
		
		if (!sb.toString().equals("12345")) {
			throw new AssertionError(sb.toString());
		}
	}
}
