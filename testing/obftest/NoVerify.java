package com.bingait.binscure.runtime;

import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author cookiedragon234 10/Feb/2020
 */
public class NoVerify {
	public static void main(String[] args) throws Throwable {
		JarFile jarFile = new JarFile(new File(args[0]));
		Unsafe unsafe = getUnsafe();
		Enumeration<JarEntry> entries = jarFile.entries();
		
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().endsWith(".class")) {
				byte[] bytes = getBytesFromInputStream(jarFile.getInputStream(entry));
				String className = entry.getName().substring(0, entry.getName().length() - ".class".length());
				System.out.println(className);
				unsafe.defineClass(className, bytes, 0, bytes.length, null, null);
			}
		}
		
		Manifest manifest = jarFile.getManifest();
		if (manifest != null) {
			Attributes attributes = manifest.getMainAttributes();
			if (attributes.containsKey(Attributes.Name.MAIN_CLASS)) {
				String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
				Class<?> aClass = Class.forName(mainClass);
				aClass.getDeclaredMethod("main", String[].class).invoke(aClass, (Object) new String[0]);
				System.out.println("invoked");
			}
		}
		System.out.println("done");
	}
	
	public static Unsafe getUnsafe() throws Exception {
		Field f = Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		return (Unsafe) f.get(null);
	}
	
	public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];
		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}
}
