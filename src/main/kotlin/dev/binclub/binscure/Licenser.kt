@file:Suppress("NOTHING_TO_INLINE")

package dev.binclub.binscure

import java.io.File
import java.util.*

/**
 * @author cook 15/Nov/2020
 */
object Licenser {
	const val MILLIS_PER_MONTH = 2629800000
	val currentTime = System.currentTimeMillis()
	
	val home = File(System.getProperty("user.home"))
	val config = File(home, ".config").also(File::mkdir)
	val binscure = File(config, ".binscure").also(File::mkdir)
	val licenseFile = File(binscure, "license.txt").also(File::createNewFile)
	
	val license = try {
		val license = licenseFile.readText(Charsets.UTF_8)
		validateLicense(license)
	} catch (t: Throwable) {
		null
	} ?: validateLicense(genLicense())
	
	init {
		println("License $license")
	}
	
	
	private inline fun genLicense(): String {
		println("\rPlease visit https://binclub.dev/license/gen and copy the license here:")
		val input = (readLine() ?: error("Couldn't read input")).trim()
		val license = Base64.getDecoder().decode(input).toString(Charsets.UTF_8)
		licenseFile.delete()
		licenseFile.createNewFile()
		licenseFile.writeText(license, Charsets.UTF_8)
		return license
	}
	
	private inline fun validateLicense(text: String): License? {
		try {
			val split = text.split('\n')
			if (split.size != 6) {
				println("Malformed license size")
				return null
			}
			val version = split[0].toInt()
			if (version != 1) {
				println("Unsupported license version ($version), please download the latest binscure update")
				return null
			}
			val username = split[1]
			val password = split[2]
			val timestamp = split[3].toLong()
			val checksum = split[4].toInt()
			val hash = split[5]
			val hashParts = hash.split('.').map { it.toInt() }.toIntArray()
			if (hashParts.size != 12) {
				println("Malformed hash part")
				return null
			}
			
			val license = License(username, password, timestamp, hashParts)
			
			if (checksum != license.hashCode()) {
				println("Mismatched checksum")
				return null
			}
			
			if (currentTime - timestamp > MILLIS_PER_MONTH) {
				println("Outdated license")
				return null
			}
			
			val usernameHash = username.hashCode()
			val passwordHash = password.hashCode()
			val timestampHash = timestamp.hashCode()
			license.hashParts.indices.forEach { i ->
				license.hashParts[i] = license.hashParts[i] xor usernameHash xor passwordHash xor timestampHash
			}
			
			return license
		} catch (t: Throwable) {
			t.printStackTrace()
			return null
		}
	}
	
	data class License(
		val username: String,
		val password: String,
		val timestamp: Long,
		/**
		 * length = 12
		 * 0 = 0x9122
		 * 1 = -0x423
		 * 2 = 0x9219
		 * 3 = 0
		 * 4 = 0x721AB
		 * 5 = 0xFFFF
		 * 6 = -x0xFFF
		 * 7 = 0x912ED
		 * 8 = 0x91BCD
		 * 9 = -0x128D
		 * 10 = 0x99E99
		 * 11 = 0x9812
		 * 12 = -0x8232
		 */
		val hashParts: IntArray
	) {
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false
			
			other as License
			
			if (username != other.username) return false
			if (password != other.password) return false
			if (!hashParts.contentEquals(other.hashParts)) return false
			
			return true
		}
		
		override inline fun hashCode(): Int {
			var result = username.hashCode()
			result = 31 * result + password.hashCode()
			result = 31 * result + hashParts.contentHashCode()
			return result
		}
	}
}
