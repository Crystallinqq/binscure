package com.binclub;

import java.util.HashMap;
import java.util.Map;

/**
 * @author cookiedragon234 08/Mar/2020
 */
public class NumberTest implements Test {
	private Map<Number, String> numbers = new HashMap<Number, String>() {{
		put(0, "0");
		put(0f, "0.0");
		put(0.0, "0.0");
		put(0L, "0");
		put(5, "5");
		put(5.5, "5.5");
		put(5.5f, "5.5");
		put(5L, "5");
		put(0.019299219, "0.019299219");
		put(0.12321412412312222, "0.12321412412312222");
	}};
	
	@Override
	public void execute() {
		for (Map.Entry<Number, String> entry : numbers.entrySet()) {
			String toString = entry.getKey().toString();
			String expected = entry.getValue();
			if (!toString.equals(expected)) {
				throw new AssertionError(toString + " does not equal " + expected);
			}
		}
	}
}
