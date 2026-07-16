package com.vva.buildings.ui

import ch.qos.logback.classic.spi.ILoggingEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TextAreaAppenderTest {

    @AfterEach
    fun tearDown() {
        TextAreaAppender.logConsumer = null
    }

    private fun event(message: String): ILoggingEvent {
        val event = mock(ILoggingEvent::class.java)
        `when`(event.formattedMessage).thenReturn(message)
        return event
    }

    @Test
    fun `append - передає formattedMessage у зареєстрований logConsumer`() {
        val received = mutableListOf<String>()
        TextAreaAppender.logConsumer = { received.add(it) }

        TextAreaAppender().apply { start() }.doAppend(event("Тестове повідомлення"))

        assertEquals(listOf("Тестове повідомлення"), received)
    }

    @Test
    fun `append - нічого не кидає якщо logConsumer не зареєстрований`() {
        TextAreaAppender.logConsumer = null
        TextAreaAppender().apply { start() }.doAppend(event("Повідомлення без слухача"))
    }

    @Test
    fun `append - кожен виклик передається поточному logConsumer`() {
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        TextAreaAppender.logConsumer = { first.add(it) }
        TextAreaAppender().apply { start() }.doAppend(event("Перше"))

        TextAreaAppender.logConsumer = { second.add(it) }
        TextAreaAppender().apply { start() }.doAppend(event("Друге"))

        assertEquals(listOf("Перше"), first)
        assertEquals(listOf("Друге"), second)
    }
}
