package me.tongfei.progressbar

import dev.binclub.binscure.kotlin.clampStart
import java.lang.StringBuilder
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant

/**
 * @author cookiedragon234 23/Jan/2020
 */
internal class CustomProcessRenderer(
	val style: ProgressBarStyle,
	unitName: String,
	unitSize: Long,
	isSpeedShown: Boolean,
	speedFormat: DecimalFormat)
	: DefaultProgressBarRenderer(style, unitName, unitSize, isSpeedShown, speedFormat) {
	
	val animation = arrayOf('|', '/', '-', '\\')
	var animationStage = 0
	
	override fun render(progress: ProgressState, maxLength: Int): String {
		val out = StringBuilder()
			.append(progress.task)
			.append(' ')
			.append(percentage(progress))
			.append(' ')
			.append(style.leftBracket)
		
		val elapsed = Duration.between(progress.startTime, Instant.now())
		
		val suffix = StringBuilder()
			.append(style.rightBracket)
			.append(' ')
			.append(progress.current)
			.append('/')
			.append(progress.max)
			.append(' ')
			.append(formatDuration(elapsed))
			.append(' ')
			.append(getAnimationChar(progress))
		
		val length = maxLength - out.length - suffix.length
		
		out.append(Util.repeat('#', progressIntegralPart(progress, length)))
		if (progress.current < progress.max) {
			out.append(style.fractionSymbols[progressFractionalPart(progress, length)])
			out.append(Util.repeat('-', length - progressIntegralPart(progress, length) - 1))
		}
		
		out.append(suffix)
		
		return out.toString()
	}
	
	private fun getAnimationChar(progress: ProgressState): Char {
		if (progress.current >= progress.max) {
			return ' '
		}
		return animation[animationStage++ % animation.size]
	}
	
	private fun percentage(progress: ProgressState): String {
		val res: String = if (progress.max <= 0 || progress.indefinite)
			"? %"
		else
			Math.floor(100.0 * progress.current / progress.max).toInt().toString() + "%"
		return Util.repeat(' ', 4 - res.length) + res
	}
	
	// Number of full blocks
	private fun progressIntegralPart(progress: ProgressState, length: Int): Int {
		return (progress.normalizedProgress * length).toInt()
	}
	
	private fun progressFractionalPart(progress: ProgressState, length: Int): Int {
		val p = progress.normalizedProgress * length
		val fraction = (p - Math.floor(p)) * style.fractionSymbols.length
		return Math.floor(fraction).toInt()
	}
	
	fun formatDuration(d: Duration): String {
		return "${(d.toMillis() / 1000f).toString().clampStart(5)}s"
		//val s = d.seconds
		//return String.format("%02d:%02d:%03d", s % 3600 / 60, s % 60, d.toMillis() - (s * 1000))
	}
}
