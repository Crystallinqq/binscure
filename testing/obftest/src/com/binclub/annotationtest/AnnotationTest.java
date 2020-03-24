package com.binclub.annotationtest;

import com.binclub.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author cookiedragon234 24/Mar/2020
 */
@TestAnnotation(first = "ghi", second = "jkl", third = false, fourth = @Deprecated())
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
		b: {
			annotation = TestAnnotation.class.getAnnotation(TestAnnotation.class);
			if (annotation.first().equals("abc")) {
				if (annotation.second().equals("def")) {
					if (annotation.third() == true) {
						if (annotation.fourth() != null) {
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
	}
}

@Retention(RetentionPolicy.RUNTIME)
@TestAnnotation(first = "abc", second = "def", third = true, fourth = @Deprecated())
@interface TestAnnotation {
	String first();
	String second();
	boolean third();
	Deprecated fourth();
}
