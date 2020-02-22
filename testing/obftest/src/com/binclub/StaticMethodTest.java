package com.binclub;

/**
 * @author cookiedragon234 22/Feb/2020
 */
public class StaticMethodTest implements Test {
	@Override
	public void execute() {
		if (doOne("hello") && !doTwo("byebye") && doThree("goodbye")) {
			return;
		}
		throw new AssertionError();
	}
	
	public static boolean doOne(String thing) {
		try {
			if (thing.equals("hello")) {
				if (thing.charAt(0) == 'h') {
					return true;
				}
			}
		} finally {
			return false;
		}
	}
	
	public static boolean doTwo(String thing) {
		try {
			if (thing.equals("goodbye")) {
				if (thing.charAt(0) == 'g') {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return false;
		}
	}
	
	public static boolean doThree(String thing) {
		if (thing.equals("goodbye")) {
			if (thing.charAt(0) == 'g') {
				return true;
			}
		}
		return false;
	}
}
