package com.binclub.annotationtest;

import com.binclub.Test;
import com.binclub.enumtest.EnumTest;
import com.binclub.enumtest.TestEnum;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author cookiedragon234 24/Mar/2020
 */
@TestAnnotation(first = "ghi", second = "jkl", third = false, fourth = @Deprecated(), fifth = TestEnum.FOUR)
public class AnnotationTest implements Test {
	@Override
	public void execute() {
		TestAnnotation annotation;
		a: {
			annotation = AnnotationTest.class.getAnnotation(TestAnnotation.class);
			if (annotation.first().equals("ghi")) {
				if (annotation.second().equals("jkl")) {
					if (annotation.third() == false) {
						if (annotation.fourth() != null) {
							if (annotation.fifth().name().equals("FOUR")) {
								break a;
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
		b: {
			annotation = TestAnnotation.class.getAnnotation(TestAnnotation.class);
			if (annotation.first().equals("abc")) {
				if (annotation.second().equals("def")) {
					if (annotation.third() == true) {
						if (annotation.fourth() != null) {
							if (annotation.fifth().name().equals("FIVE")) {
								break b;
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
}

@Retention(RetentionPolicy.RUNTIME)
@TestAnnotation(first = "abc", second = "def", third = true, fourth = @Deprecated(), fifth = TestEnum.FIVE)
@interface TestAnnotation {
	String first();
	String second();
	boolean third();
	Deprecated fourth();
	TestEnum fifth();
}
