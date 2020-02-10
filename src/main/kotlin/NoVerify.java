import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
		Enumeration<JarEntry> entries = jarFile.entries();
		
		CustomClassLoader classLoader = new CustomClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().endsWith(".class")) {
				byte[] bytes = getBytesFromInputStream(jarFile.getInputStream(entry));
				String className = entry.getName().substring(0, entry.getName().length() - ".class".length());
				System.out.println(className);
				classLoader.classes.put(className, bytes);
			}
		}
		
		Manifest manifest = jarFile.getManifest();
		if (manifest != null) {
			Attributes attributes = manifest.getMainAttributes();
			if (attributes.containsKey(Attributes.Name.MAIN_CLASS)) {
				String mainClass = attributes.getValue(Attributes.Name.MAIN_CLASS);
				Class<?> aClass = Class.forName(mainClass, false, classLoader);
				aClass.getDeclaredMethod("main", String[].class).invoke(aClass, (Object) new String[0]);
				System.out.println("invoked");
			}
		}
		System.out.println("done");
	}
	
	public static Unsafe getUnsafe() {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			return (Unsafe) f.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[0xFFFF];
		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}
	
	static class CustomClassLoader extends ClassLoader {
		Unsafe unsafe = getUnsafe();
		Map<String, byte[]> classes = new HashMap<>();
		Map<String, Class<?>> initClasses = new HashMap<>();
		
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			//name = name.replace('.', '/');
			System.out.println("Find: " + name);
			if (initClasses.containsKey(name)) {
				return initClasses.get(name);
			}
			return super.findClass(name);
		}
		
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			//name = name.replace('.', '/');
			System.out.println("Load: " + name);
			if (classes.containsKey(name)) {
				byte[] bytes = classes.get(name.replace('.', '/'));
				//Class<?> clazz = unsafe.defineClass(name, bytes, 0, bytes.length, null, null);
				Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
				initClasses.put(name, clazz);
				return clazz;
			}
			return super.loadClass(name);
		}
		
		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			//name = name.replace('.', '/');
			System.out.println("Load2: " + name);
			if (classes.containsKey(name)) {
				byte[] bytes = classes.get(name.replace('.', '/'));
				//Class<?> clazz = unsafe.defineClass(name, bytes, 0, bytes.length, null, null);
				Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
				this.resolveClass(clazz);
				initClasses.put(name, clazz);
				return clazz;
			}
			return super.loadClass(name, resolve);
		}
	}
}
