package com.binclub;

import com.binclub.annotationtest.AnnotationTest;
import com.binclub.enumtest.EnumTest;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;

public class Main {
	private static final Test[] tests = new Test[]{
		new IfStatementTest(),
		new StaticMethodTest(),
		new EnumTest(),
		new StaticFieldTest(),
		new NumberTest(),
		new ThrowableTest(),
		new AnnotationTest()
	};
	
    public static void main(String[] args) {
		Instant start = Instant.now();
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
		System.out.println("Took " + Duration.between(start, Instant.now()).toMillis() + "ms");
    }
	
	private static String padRight(String s, int n) {
		return String.format("%-" + n + "s", s);
	}
}
