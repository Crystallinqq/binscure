package com.binclub;

/**
 * @author cookiedragon234 10/Mar/2020
 */
public class ThrowableTest implements Test {
	@Override
	public void execute() {
		String[] arr = new String[5];
		try {
			System.out.println(arr[6]);
		} catch (ArrayIndexOutOfBoundsException e) {
			try {
				System.out.println(arr[-1]);
			} catch (ArrayIndexOutOfBoundsException e1) {
				try {
					arr = new String[-1];
				} catch (NegativeArraySizeException e2) {
					arr = null;
					try {
						System.out.println(arr[2]);
					} catch (NullPointerException e3) {
						try {
							Class.forName("123456789012345678901234567890");
						} catch (ClassNotFoundException e4) {
							return;
						}
						throw new AssertionError();
					}
					throw new AssertionError();
				}
				throw new AssertionError();
			}
			throw new AssertionError();
		}
		throw new AssertionError();
	}
}
