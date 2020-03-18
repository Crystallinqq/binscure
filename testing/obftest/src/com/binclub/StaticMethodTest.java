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
		System.out.println(doOne("hello") +"."+ !doTwo("byebye") +"."+ doThree("goodbye"));
		throw new AssertionError();
	}
	
	public static boolean doOne(String thing) {
		if (thing.equals("hello")) {
			if (thing.charAt(0) == 'h') {
				return true;
			}
		}
		throw new AssertionError(thing + " != " + "hello");
	}
	
	public static boolean doTwo(String thing) {
		if (thing.equals("goodbye")) {
			if (thing.charAt(0) == 'g') {
				throw new AssertionError(thing + " == " + "goodbye");
			}
		}
		return false;
	}
	
	public static boolean doThree(String thing) {
		if (thing.equals("goodbye")) {
			if (thing.charAt(0) == 'g') {
				return true;
			}
		}
		throw new AssertionError(thing + " != " + "goodbye");
	}
}
