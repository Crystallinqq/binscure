package me.tongfei.progressbar

import me.tongfei.progressbar.wrapped.ProgressBarWrappedIterator

/**
 * @author cookiedragon234 23/Jan/2020
 */
class ProgressBarIterable<T>(val underlying: Iterable<T>, val pb: ProgressBar): Iterable<T> {
	override operator fun iterator(): Iterator<T> {
		val it = underlying.iterator()
		return ProgressBarWrappedIterator<T>(
			it,
			pb.maxHint(underlying.spliterator().exactSizeIfKnown)
		)
	}
}
