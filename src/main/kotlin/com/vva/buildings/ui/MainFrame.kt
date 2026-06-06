package com.vva.buildings.ui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

class MainFrame(private val onStart: () -> Unit) : JFrame("Обробка будівель") {

    private val logArea = JTextArea()
    private val startButton = JButton("Старт")
    private val statusLabel = JLabel("Готово до запуску")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        preferredSize = Dimension(900, 600)
        layout = BorderLayout(5, 5)

        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 8))
        startButton.font = startButton.font.deriveFont(Font.BOLD, 14f)
        topPanel.add(startButton)
        topPanel.add(statusLabel)

        logArea.isEditable = false
        logArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        logArea.background = Color(30, 30, 30)
        logArea.foreground = Color(200, 230, 200)
        logArea.caretColor = Color(200, 230, 200)
        val scrollPane = JScrollPane(logArea)

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        startButton.addActionListener { runProcessing() }

        TextAreaAppender.logConsumer = { message -> appendLog(message) }

        pack()
        setLocationRelativeTo(null)
    }

    private fun runProcessing() {
        startButton.isEnabled = false
        statusLabel.text = "Виконується..."
        logArea.text = ""
        Thread {
            try {
                onStart()
                SwingUtilities.invokeLater {
                    statusLabel.text = "Завершено успішно"
                    startButton.isEnabled = true
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusLabel.text = "Помилка!"
                    appendLog("ПОМИЛКА: ${e.message}")
                    startButton.isEnabled = true
                }
            }
        }.start()
    }

    fun appendLog(text: String) {
        SwingUtilities.invokeLater {
            logArea.append("$text\n")
            logArea.caretPosition = logArea.document.length
        }
    }
}
