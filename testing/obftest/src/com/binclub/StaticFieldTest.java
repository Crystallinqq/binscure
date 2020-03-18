package com.binclub;

/**
 * @author cookiedragon234 07/Mar/2020
 */
public class StaticFieldTest implements Test {
	private static final String first = "first";
	private static String second = "second";
	private static String third = "third";
	private final String fourth = "fourth";
	private String fifth = "fifth";
	
	public StaticFieldTest() {
	}
	
	public StaticFieldTest(int dummy) {
	}
	
	
	@Override
	public void execute() {
		if (first.equals("first")) {
			if (second.equals("second")) {
				if (third.equals("third")) {
					if (fourth.equals("fourth")) {
						if (fifth.equals("fifth")) {
							return;
						}
						throw new AssertionError(fifth + "!=" + "fifth");
					}
					throw new AssertionError(fourth + "!=" + "fourth");
				}
				throw new AssertionError(third + "!=" + "third");
			}
			throw new AssertionError(second + "!=" + "second");
		}
		throw new AssertionError(first + "!=" + "first");
	}
}
