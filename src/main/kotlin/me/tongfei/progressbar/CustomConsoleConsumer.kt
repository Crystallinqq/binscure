package me.tongfei.progressbar

import java.io.IOException
import java.io.PrintStream

/**
 * @author cookiedragon234 26/Jan/2020
 */
class CustomConsoleConsumer(private val out: PrintStream = System.out): ProgressBarConsumer {
	private val consoleRightMargin = 2
	private val terminal = Util.getTerminal()
	
	override fun getMaxProgressLength(): Int {
		return Util.getTerminalWidth(terminal) - consoleRightMargin
	}
	
	override fun accept(str: String) {
		//out.print('\r')
		//out.print(str)
		
		out.print(str)
		out.print('\r')
	}
	
	override fun close() {
		out.println()
		out.flush()
		try {
			terminal.close()
		} catch (ignored: IOException) { /* noop */
		}
	}
}

