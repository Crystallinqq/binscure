package com.binclub;

import java.text.MessageFormat;

public class Main {
	private static final Test[] tests = new Test[]{
		new IfStatementTest()
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

interface Test {
	void execute();
};

class IfStatementTest implements Test {
	int field = 0;
	
	@Override
	public void execute() {
		int first = 0;
		int second = 2;
		if (first + second == second && first + second == 2) {
			if (Thread.currentThread() != null) {
				if (Runtime.getRuntime() != null) {
					long currentMillis = System.currentTimeMillis();
					if (currentMillis < 0) {
						throw new AssertionError();
					} else {
						if (second == 2) {
							switch(field) {
								case 0:
									StringBuilder sb = new StringBuilder();
									for (int i = 0; i < 10; i++) {
										sb.append(i);
									}
									if (sb.toString().equals("0123456789")) {
										return;
									}
									throw new AssertionError(sb.toString());
								case 1:
									if (System.console() != null) {
										throw new AssertionError();
									}
									throw new NullPointerException();
								case 3:
									System.out.println("What?");
								case 4:
								default:
									throw new AssertionError();
							}
						}
						throw new AssertionError();
					}
				}
				throw new AssertionError();
			}
			throw new AssertionError();
		}
		throw new AssertionError();
	}
}
