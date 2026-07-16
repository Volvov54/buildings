package com.vva.buildings.ui

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Component
import java.awt.Container
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.SwingUtilities

/**
 * MainFrame будується поза EDT (на відміну від реального застосунку, де це відбувається
 * всередині SwingUtilities.invokeLater у main()) - для Swing-компонентів, які ще не показані
 * (isVisible=true не викликається), це безпечно. Асинхронні переходи статусу після doClick()
 * перевіряються через коротке опитування (EDT обробляє чергу незалежно від потоку тесту).
 */
class MainFrameTest {

    private val createdFrames = mutableListOf<MainFrame>()

    private fun newFrame(onStart: () -> Unit): MainFrame =
        MainFrame(onStart).also { createdFrames.add(it) }

    @AfterEach
    fun tearDown() {
        TextAreaAppender.logConsumer = null
        createdFrames.forEach { it.dispose() }
        createdFrames.clear()
    }

    private fun <T : Component> findComponent(container: Container, type: Class<T>): T =
        findComponentOrNull(container, type)
            ?: throw NoSuchElementException("${type.simpleName} не знайдено у дереві компонентів")

    private fun <T : Component> findComponentOrNull(container: Container, type: Class<T>): T? {
        for (component in container.components) {
            if (type.isInstance(component)) {
                @Suppress("UNCHECKED_CAST")
                return component as T
            }
            if (component is Container) {
                findComponentOrNull(component, type)?.let { return it }
            }
        }
        return null
    }

    private fun awaitEdt() {
        SwingUtilities.invokeAndWait {}
    }

    private fun awaitLabelText(label: JLabel, expected: String, timeoutMs: Long = 3000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline && label.text != expected) {
            Thread.sleep(20)
        }
        assertEquals(expected, label.text)
    }

    @Test
    fun `конструктор ініціалізує заголовок, кнопку, статус та лог-поле`() {
        val frame = newFrame {}

        assertEquals("Обробка будівель", frame.title)

        val button = findComponent(frame.contentPane, JButton::class.java)
        assertEquals("Старт", button.text)
        assertTrue(button.isEnabled)

        val statusLabel = findComponent(frame.contentPane, JLabel::class.java)
        assertEquals("Готово до запуску", statusLabel.text)

        val logArea = findComponent(frame.contentPane, JTextArea::class.java)
        assertFalse(logArea.isEditable)
    }

    @Test
    fun `конструктор реєструє appendLog як logConsumer TextAreaAppender`() {
        val frame = newFrame {}

        TextAreaAppender.logConsumer?.invoke("Тестове повідомлення")
        awaitEdt()

        val logArea = findComponent(frame.contentPane, JTextArea::class.java)
        assertTrue(logArea.text.contains("Тестове повідомлення"))
    }

    @Test
    fun `appendLog додає рядок з переносом та переміщує каретку в кінець`() {
        val frame = newFrame {}

        frame.appendLog("Перший рядок")
        frame.appendLog("Другий рядок")
        awaitEdt()

        val logArea = findComponent(frame.contentPane, JTextArea::class.java)
        assertEquals("Перший рядок\nДругий рядок\n", logArea.text)
        assertEquals(logArea.document.length, logArea.caretPosition)
    }

    @Test
    fun `клік на Старт викликає onStart і після завершення оновлює статус на Завершено успішно`() {
        val started = CountDownLatch(1)
        val frame = newFrame { started.countDown() }
        val button = findComponent(frame.contentPane, JButton::class.java)
        val statusLabel = findComponent(frame.contentPane, JLabel::class.java)

        button.doClick()

        assertFalse(button.isEnabled)
        assertEquals("Виконується...", statusLabel.text)
        assertTrue(started.await(2, TimeUnit.SECONDS), "onStart мав бути викликаний")

        awaitLabelText(statusLabel, "Завершено успішно")
        assertTrue(button.isEnabled)
    }

    @Test
    fun `клік на Старт при винятку в onStart оновлює статус на Помилка і логує повідомлення`() {
        val frame = newFrame { throw RuntimeException("Щось пішло не так") }
        val button = findComponent(frame.contentPane, JButton::class.java)
        val statusLabel = findComponent(frame.contentPane, JLabel::class.java)

        button.doClick()
        awaitLabelText(statusLabel, "Помилка!")

        val logArea = findComponent(frame.contentPane, JTextArea::class.java)
        assertTrue(logArea.text.contains("ПОМИЛКА: Щось пішло не так"))
        assertTrue(button.isEnabled)
    }
}
