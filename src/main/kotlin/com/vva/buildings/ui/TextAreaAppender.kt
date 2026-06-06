package com.vva.buildings.ui

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

class TextAreaAppender : AppenderBase<ILoggingEvent>() {
    companion object {
        @Volatile
        var logConsumer: ((String) -> Unit)? = null
    }

    override fun append(event: ILoggingEvent) {
        logConsumer?.invoke(event.formattedMessage)
    }
}
