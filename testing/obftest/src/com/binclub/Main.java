package com.binclub;

import com.binclub.enumtest.EnumTest;

import java.text.MessageFormat;

public class Main {
	private static final Test[] tests = new Test[]{
		new IfStatementTest(),
		new StaticMethodTest(),
		new EnumTest(),
		new StaticFieldTest(),
		new NumberTest()
	};
	
    public static void main(String[] args) {
		for (Test test : tests) {
			boolean success = false;
			try {
				test.execute();
				success = true;
			} catch (Throwable t) {
				t.printStackTrace();
			}
			
			System.out.print(MessageFormat.format("[TEST] {0} ", padRight(test.getClass().getSimpleName(), 20)));
			if (success) {
				System.out.println("[SUCCESS]");
			} else {
				System.out.println("[FAILURE]");
			}
		}
    }
	
	private static String padRight(String s, int n) {
		return String.format("%-" + n + "s", s);
	}
}
