package cc.wenmin92.jsonkeyfinder.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger

/**
 * Log utility class that outputs to both IDE log and console
 */
class LogUtil constructor(private val ideLogger: Logger, private val className: String) {

    fun info(message: String) {
        ideLogger.info(message)
        println("[INFO] [${className}] $message")
    }

    fun debug(message: String) {
        ideLogger.debug(message)
        println("[DEBUG] [${className}] $message")
    }

    fun warn(message: String) {
        ideLogger.warn(message)
        println("[WARN] [${className}] $message")
    }

    fun error(message: String, e: Throwable? = null) {
        ideLogger.error(message, e)
        println("[ERROR] [${className}] $message")
        e?.printStackTrace()
    }

    fun trace(message: String) {
        ideLogger.trace(message)
        println("[TRACE] [${className}] $message")
    }

    companion object {
        inline fun <reified T : Any> getLogger(): LogUtil {
            return LogUtil(logger<T>(), T::class.java.simpleName)
        }
    }
} 