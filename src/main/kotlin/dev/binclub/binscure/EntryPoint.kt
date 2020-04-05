package dev.binclub.binscure

import dev.binclub.binscure.EntryPoint.Companion.license
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.experimental.xor

/**
 * @author cookiedragon234 04/Apr/2020
 */
class EntryPoint : ClassLoader(this::class.java.classLoader) {
	override fun loadClass(name: String, resolve: Boolean): Class<*> {
		return try {
			val bytes = parent.getResourceAsStream("${name.replace('.', '/')}.class").readBytes()
			try {
				defineClass(name, bytes, 0, bytes.size)
			} catch (t: Throwable) {
				defineClass(name, bytes.encryptFile(key), 0, bytes.size)
			}
		} catch (t: Throwable) {
			this.parent.loadClass(name)
		}
	}
	
	override fun findClass(name: String): Class<*> = loadClass(name, false)
	
	companion object {
		var license = try {
			this::class.java.classLoader.getResourceAsStream("license")?.let {
				it.readBytes().toString(Charsets.UTF_8).split('\n')
			}!!
		} catch (t: Throwable) {
			arrayListOf<String>()
		}
		
		val key
			get() = (license[0] + license[1])
		
		val stamp
			get() = (license[2])
		
		@JvmStatic
		fun main(args: Array<String>) {
			val scanner = Scanner(System.`in`)
			while (!isLicenseValid()) {
				println("Invalid License. Please ensure internet access and then enter your email and password")
				print("Email: ")
				val email = URLEncoder.encode(scanner.next(), Charsets.UTF_8.name())
				print("Password: ")
				val password = URLEncoder.encode(scanner.next(), Charsets.UTF_8.name())
				val product = URLEncoder.encode("1", Charsets.UTF_8.name())
				
				try {
					val conn = URL(
						"http://192.168.1.65/api/license?username=$email&password=$password&product=$product"
					).openConnection() as HttpURLConnection
					conn.requestMethod = "GET"
					conn.setRequestProperty("User-Agent", "Mozilla/5.0")
					val text = conn.inputStream.readBytes().toString(Charsets.UTF_8)
					license = text.split('\n')
					
					try {
						File(this::class.java.classLoader.getResource("license")!!.path).writeText(text)
					} catch (t: Throwable) {
						t.printStackTrace()
					}
				} catch (t: Throwable) {
					t.printStackTrace()
				}
			}
			
			EntryPoint().also { cl ->
				Thread.currentThread().contextClassLoader = cl
				cl.loadClass("dev.binclub.binscure.EntryPoint2", false).apply {
					getMethod("start", Array<String>::class.java)(null, args)
				}
			}
		}
	}
}

class EntryPoint2 {
	companion object {
		@JvmStatic
		fun start(args: Array<String>) {
			Main.main(args)
		}
	}
}

fun generateNewLicense() {

}

inline fun isLicenseValid(): Boolean {
	return if (license.size < 3) false
	else generateUserKey(license[0], license[1]) == license[2]
}

inline fun checkLicense() {
	if (!isLicenseValid()) {
		System.err.println("Invalid License")
		System.exit(0)
	}
}

inline fun generateUserKey(username: String, password: String): String {
	val timestamp = generateTruncuatedTimestamp()
	
	val str = username + password + timestamp.toString()
	
	return MessageDigest.getInstance("SHA-256").digest(str.toByteArray().encryptFile("asdfasdfas31fs")).toString(Charsets.UTF_8)
}

inline fun generateTruncuatedTimestamp(): Long {
	return ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
		.with(TemporalAdjusters.firstDayOfMonth())
		.truncatedTo(ChronoUnit.DAYS)
		.toEpochSecond()
}

inline fun ByteArray.encryptFile(key: String): ByteArray = this.apply {
	for (i in this.indices) {
		this[i] = this[i] xor key[i % key.length].toByte()
	}
}
