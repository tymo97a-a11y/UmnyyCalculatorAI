package com.smartcalculator.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    // =========================================================
    // СЕРВЕР
    // =========================================================

    private val serverUrl =
        "https://umnyy-calculator-ai-server.onrender.com"

    private val client =
        OkHttpClient()

    // =========================================================
    // ИСТОРИЯ
    // =========================================================

    private val historyList =
        mutableListOf<String>()

    // =========================================================
    // ТЕКУЩИЙ РЕЗУЛЬТАТ
    // =========================================================

    private var currentTask = ""

    private var currentAnswer = ""

    private var currentExplanation = ""

    // =========================================================
    // ЦВЕТА
    // =========================================================

    private val bgColor =
        Color.rgb(7, 11, 20)

    private val cardColor =
        Color.rgb(17, 26, 43)

    private val blueColor =
        Color.rgb(36, 107, 253)

    private val textColor =
        Color.WHITE

    private val secondaryColor =
        Color.rgb(180, 195, 215)

    // =========================================================
    // DP
    // =========================================================

    private fun dp(value: Int): Int {

        return (
            value *
                    resources.displayMetrics.density
            ).toInt()
    }

    // =========================================================
    // СОЗДАНИЕ
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        loadHistory()

        showMainMenu()
    }

    // =========================================================
    // ГЛАВНОЕ МЕНЮ
    // =========================================================

    private fun showMainMenu() {

        setContentView(
            R.layout.activity_main
        )

        findViewById<View>(
            R.id.menuAI
        ).setOnClickListener {

            openAIScreen()
        }

        findViewById<View>(
            R.id.menuCalculator
        ).setOnClickListener {

            openCalculatorScreen()
        }

        findViewById<View>(
            R.id.menuGraph
        ).setOnClickListener {

            openGraphScreen()
        }

        findViewById<View>(
            R.id.menuPhoto
        ).setOnClickListener {

            openPhotoScreen()
        }

        findViewById<View>(
            R.id.menuTable
        ).setOnClickListener {

            openTableScreen()
        }

        findViewById<View>(
            R.id.menuHistory
        ).setOnClickListener {

            openHistoryScreen()
        }
    }

    // =========================================================
    // ROOT
    // =========================================================

    private fun createRoot():
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(30)
            )

            setBackgroundColor(
                bgColor
            )
        }
    }

    // =========================================================
    // ЭКРАН
    // =========================================================

    private fun setScreen(
        view: View
    ) {

        val scroll =
            ScrollView(this)

        scroll.setBackgroundColor(
            bgColor
        )

        scroll.addView(view)

        setContentView(scroll)
    }

    // =========================================================
    // НАЗАД
    // =========================================================

    private fun addBackButton(
        container: LinearLayout,
        onBack: (() -> Unit)? = null
    ) {

        val button =
            Button(this)

        button.text =
            "← Назад"

        button.setTextColor(
            textColor
        )

        button.setBackgroundColor(
            Color.TRANSPARENT
        )

        button.textSize =
            16f

        button.minHeight =
            dp(50)

        button.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(5)
        )

        button.setOnClickListener {

            if (onBack != null) {

                onBack()

            } else {

                showMainMenu()
            }
        }

        container.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )
    }

    // =========================================================
    // СБРОС
    // =========================================================

    private fun addResetButton(
        container: LinearLayout
    ) {

        val button =
            Button(this)

        button.text =
            "🗑 Сбросить"

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            Color.rgb(
                170,
                45,
                55
            )
        )

        button.textSize =
            16f

        button.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(5)
        )

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )

        params.setMargins(
            0,
            dp(10),
            0,
            dp(15)
        )

        container.addView(
            button,
            params
        )

        button.setOnClickListener {

            currentTask = ""

            currentAnswer = ""

            currentExplanation = ""

            showMainMenu()
        }
    }

    // =========================================================
    // ЗАГОЛОВОК
    // =========================================================

    private fun makeTitle(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text =
                text

            setTextColor(
                textColor
            )

            textSize =
                24f

            gravity =
                Gravity.CENTER

            setPadding(
                dp(10),
                dp(20),
                dp(10),
                dp(20)
            )
        }
    }

    // =========================================================
    // ЗАГОЛОВОК КАРТОЧКИ
    // =========================================================

    private fun makeCardTitle(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text =
                text

            setTextColor(
                textColor
            )

            textSize =
                20f

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(10)
            )
        }
    }

    // =========================================================
    // ТЕКСТ КАРТОЧКИ
    // =========================================================

    private fun makeCardText(
        text: String
    ): TextView {

        return TextView(this).apply {

            this.text =
                text

            setTextColor(
                secondaryColor
            )

            textSize =
                16f

            setPadding(
                dp(20),
                dp(5),
                dp(20),
                dp(20)
            )
        }
    }

    // =========================================================
    // КАРТОЧКА
    // =========================================================

    private fun makeCard():
            LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setBackgroundColor(
                cardColor
            )

            setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
            )

            val params =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

            params.setMargins(
                0,
                dp(10),
                0,
                dp(10)
            )

            layoutParams =
                params
        }
    }

    // =========================================================
    // AI ЭКРАН
    // =========================================================

    private fun openAIScreen() {

        val root =
            createRoot()

        addBackButton(root)

        root.addView(
            makeTitle(
                "🤖 Рассчитать с AI"
            )
        )

        root.addView(
            makeCardText(
                "Опиши задачу обычными словами"
            )
        )

        val input =
            EditText(this)

        input.hint =
            "Например: 15% от 8400"

        input.setTextColor(
            textColor
        )

        input.setHintTextColor(
            Color.GRAY
        )

        input.setBackgroundColor(
            cardColor
        )

        input.textSize =
            18f

        input.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        // =====================================================
        // РАССЧИТАТЬ
        // =====================================================

        val button =
            Button(this)

        button.text =
            "🤖 Рассчитать с AI"

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            blueColor
        )

        button.textSize =
            17f

        button.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(5)
        )

        root.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        // =====================================================
        // ОЧИСТИТЬ
        // =====================================================

        val clearButton =
            Button(this)

        clearButton.text =
            "🗑 Очистить"

        clearButton.setTextColor(
            Color.WHITE
        )

        clearButton.setBackgroundColor(
            Color.rgb(
                80,
                90,
                110
            )
        )

        clearButton.textSize =
            16f

        root.addView(
            clearButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        clearButton.setOnClickListener {

            input.setText("")

            input.requestFocus()
        }

        button.setOnClickListener {

            val text =
                input.text
                    .toString()
                    .trim()

            if (text.isEmpty()) {

                Toast.makeText(
                    this,
                    "Введите задачу",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            sendToAI(text)
        }

        setScreen(root)
    }

    // =========================================================
    // ОТПРАВКА В AI
    // =========================================================

    private fun sendToAI(
        text: String
    ) {

        Toast.makeText(
            this,
            "AI решает задачу...",
            Toast.LENGTH_SHORT
        ).show()

        val json =
            org.json.JSONObject()

        json.put(
            "text",
            text
        )

        val body =
            json.toString()
                .toRequestBody(
                    "application/json"
                        .toMediaType()
                )

        val request =
            Request.Builder()
                .url(
                    "$serverUrl/v1/calculate"
                )
                .post(body)
                .build()

        client.newCall(
            request
        ).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка подключения к AI",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val responseText =
                        response.body
                            ?.string()
                            ?: ""

                    if (
                        !response.isSuccessful
                    ) {

                        runOnUiThread {

                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка сервера: ${response.code}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        return
                    }

                    try {

                        val jsonResponse =
                            org.json.JSONObject(
                                responseText
                            )

                        val answer =
                            jsonResponse.optString(
                                "result",
                                "Ответ не получен"
                            )

                        val explanation =
                            jsonResponse.optString(
                                "explanation",
                                ""
                            )

                        addHistory(
                            text,
                            answer
                        )

                        runOnUiThread {

                            currentTask =
                                text

                            currentAnswer =
                                answer

                            currentExplanation =
                                explanation

                            openAIResultScreen(
                                text,
                                answer,
                                explanation
                            )
                        }

                    } catch (_: Exception) {

                        runOnUiThread {

                            Toast.makeText(
                                this@MainActivity,
                                "Не удалось обработать ответ AI",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )
    }

    // =========================================================
    // РЕЗУЛЬТАТ AI
    // =========================================================

    private fun openAIResultScreen(
        task: String,
        answer: String,
        explanation: String
    ) {

        currentTask =
            task

        currentAnswer =
            answer

        currentExplanation =
            explanation

        val root =
            createRoot()

        // =====================================================
        // НАЗАД → AI
        // =====================================================

        addBackButton(root) {

            openAIScreen()
        }

        root.addView(
            makeTitle(
                "🤖 Результат AI"
            )
        )

        // =====================================================
        // СБРОС
        // =====================================================

        addResetButton(root)

        // =====================================================
        // ЗАДАЧА
        // =====================================================

        val taskCard =
            makeCard()

        taskCard.addView(
            makeCardTitle(
                "📝 Задача"
            )
        )

        taskCard.addView(
            makeCardText(
                task
            )
        )

        root.addView(
            taskCard
        )

        // =====================================================
        // ОТВЕТ
        // =====================================================

        val answerCard =
            makeCard()

        answerCard.addView(
            makeCardTitle(
                "✅ ОТВЕТ"
            )
        )

        val answerText =
            TextView(this)

        answerText.text =
            answer

        answerText.setTextColor(
            Color.WHITE
        )

        answerText.textSize =
            28f

        answerText.gravity =
            Gravity.CENTER

        answerText.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(30)
        )

        answerCard.addView(
            answerText
        )

        root.addView(
            answerCard
        )

        // =====================================================
        // ПОШАГОВОЕ РЕШЕНИЕ
        // =====================================================

        val explanationCard =
            makeCard()

        explanationCard.addView(
            makeCardTitle(
                "📚 Пошаговое решение"
            )
        )

        explanationCard.addView(
            makeCardText(
                if (
                    explanation.isBlank()
                ) {
                    "AI не предоставил дополнительное объяснение."
                } else {
                    explanation
                }
            )
        )

        root.addView(
            explanationCard
        )

        // =====================================================
        // ПРОВЕРКА ФУНКЦИИ
        // =====================================================

        val function =
            parseFunction(task)

        if (
            function != null
        ) {

            // =================================================
            // ГРАФИК
            // =================================================

            val graphButton =
                Button(this)

            graphButton.text =
                "📈 Открыть график"

            graphButton.setTextColor(
                Color.WHITE
            )

            graphButton.setBackgroundColor(
                blueColor
            )

            graphButton.textSize =
                17f

            root.addView(
                graphButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(70)
                )
            )

            graphButton.setOnClickListener {

                openGraphScreen(
                    task
                )
            }

            // =================================================
            // ТАБЛИЦА
            // =================================================

            val tableButton =
                Button(this)

            tableButton.text =
                "📊 Открыть таблицу"

            tableButton.setTextColor(
                Color.WHITE
            )

            tableButton.setBackgroundColor(
                blueColor
            )

            tableButton.textSize =
                17f

            val tableParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(70)
                )

            tableParams.setMargins(
                0,
                dp(10),
                0,
                dp(10)
            )

            root.addView(
                tableButton,
                tableParams
            )

            tableButton.setOnClickListener {

                openTableScreen(
                    task
                )
            }

        } else {

            val infoCard =
                makeCard()

            infoCard.addView(
                makeCardTitle(
                    "💡 Подсказка"
                )
            )

            infoCard.addView(
                makeCardText(
                    "Для автоматического графика и таблицы " +
                            "используй функцию, например:\n\n" +
                            "y = x²\n" +
                            "y = 2x + 5\n" +
                            "y = x² - 4x + 3"
                )
            )

            root.addView(
                infoCard
            )
        }

        // =====================================================
        // НОВАЯ ЗАДАЧА
        // =====================================================

        val newTaskButton =
            Button(this)

        newTaskButton.text =
            "🤖 Решить новую задачу"

        newTaskButton.setTextColor(
            Color.WHITE
        )

        newTaskButton.setBackgroundColor(
            blueColor
        )

        newTaskButton.textSize =
            17f

        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(75)
            )

        params.setMargins(
            0,
            dp(20),
            0,
            dp(20)
        )

        root.addView(
            newTaskButton,
            params
        )

        newTaskButton.setOnClickListener {

            currentTask = ""

            currentAnswer = ""

            currentExplanation = ""

            openAIScreen()
        }

        setScreen(root)
    }

    // =========================================================
    // СОЗДАНИЕ ТАБЛИЦЫ
    // =========================================================

    private fun createFunctionTable(
        function: (Double) -> Double
    ): TableLayout {

        val table =
            TableLayout(this)

        table.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(20)
        )

        table.setStretchAllColumns(
            true
        )

        addTableRow(
            table,
            "x",
            "y"
        )

        for (
            i in -5..5
        ) {

            val x =
                i.toDouble()

            val y =
                try {

                    function(x)

                } catch (
                    _: Exception
                ) {

                    Double.NaN
                }

            val yText =
                if (
                    y.isFinite()
                ) {

                    formatNumber(y)

                } else {

                    "—"
                }

            addTableRow(
                table,
                formatNumber(x),
                yText
            )
        }

        return table
    }

    // =========================================================
    // СТРОКА ТАБЛИЦЫ
    // =========================================================

    private fun addTableRow(
        table: TableLayout,
        first: String,
        second: String
    ) {

        val row =
            TableRow(this)

        val firstText =
            TextView(this)

        firstText.text =
            first

        firstText.setTextColor(
            Color.WHITE
        )

        firstText.textSize =
            17f

        firstText.gravity =
            Gravity.CENTER

        firstText.setPadding(
            dp(10),
            dp(15),
            dp(10),
            dp(15)
        )

        val secondText =
            TextView(this)

        secondText.text =
            second

        secondText.setTextColor(
            Color.WHITE
        )

        secondText.textSize =
            17f

        secondText.gravity =
            Gravity.CENTER

        secondText.setPadding(
            dp(10),
            dp(15),
            dp(10),
            dp(15)
        )

        row.addView(
            firstText
        )

        row.addView(
            secondText
        )

        table.addView(
            row
        )
    }

    // =========================================================
    // ФОРМАТ ЧИСЛА
    // =========================================================

    private fun formatNumber(
        number: Double
    ): String {

        if (
            !number.isFinite()
        ) {

            return "—"
        }

        return if (
            number % 1.0 == 0.0
        ) {

            number
                .toLong()
                .toString()

        } else {

            String.format(
                Locale.US,
                "%.2f",
                number
            )
        }
    }

    // =========================================================
    // КАЛЬКУЛЯТОР
    // =========================================================

    private fun openCalculatorScreen() {

        val root =
            createRoot()

        addBackButton(root)

        root.addView(
            makeTitle(
                "🧮 Калькулятор"
            )
        )

        val display =
            TextView(this)

        display.text =
            "0"

        display.setTextColor(
            Color.WHITE
        )

        display.textSize =
            32f

        display.gravity =
            Gravity.RIGHT or
                    Gravity.CENTER_VERTICAL

        display.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        display.setBackgroundColor(
            cardColor
        )

        root.addView(
            display,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(100)
            )
        )

        val grid =
            GridLayout(this)

        grid.columnCount =
            4

        val buttons =
            arrayOf(
                "AC",
                "⌫",
                "%",
                "÷",
                "(",
                ")",
                "×",
                "−",
                "7",
                "8",
                "9",
                "+",
                "4",
                "5",
                "6",
                "=",
                "1",
                "2",
                "3",
                ".",
                "0"
            )

        for (
            text in buttons
        ) {

            val button =
                Button(this)

            button.text =
                text

            button.setTextColor(
                Color.WHITE
            )

            button.textSize =
                20f

            button.minHeight =
                0

            button.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
            )

            if (
                text == "+" ||
                text == "−" ||
                text == "×" ||
                text == "÷" ||
                text == "=" ||
                text == "%" ||
                text == "AC"
            ) {

                button.setBackgroundResource(
                    R.drawable.button_operator
                )

            } else {

                button.setBackgroundResource(
                    R.drawable.button_number
                )
            }

            val params =
                GridLayout.LayoutParams()

            params.width =
                0

            params.height =
                dp(85)

            params.columnSpec =
                if (
                    text == "0"
                ) {

                    GridLayout.spec(
                        GridLayout.UNDEFINED,
                        2,
                        1f
                    )

                } else {

                    GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1,
                        1f
                    )
                }

            params.setMargins(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
            )

            grid.addView(
                button,
                params
            )

            button.setOnClickListener {

                when (text) {

                    // =========================================
                    // AC
                    // =========================================

                    "AC" -> {

                        display.text =
                            "0"
                    }

                    // =========================================
                    // BACKSPACE
                    // =========================================

                    "⌫" -> {

                        val value =
                            display.text
                                .toString()

                        display.text =
                            if (
                                value.length <= 1
                            ) {

                                "0"

                            } else {

                                value.dropLast(1)
                            }
                    }

                    // =========================================
                    // =
                    // =========================================

                    "=" -> {

                        val expression =
                            display.text
                                .toString()

                        display.text =
                            calculateExpression(
                                expression
                            )
                    }

                    // =========================================
                    // ОСТАЛЬНЫЕ
                    // =========================================

                    else -> {

                        if (
                            display.text == "0"
                        ) {

                            display.text =
                                text

                        } else {

                            display.append(
                                text
                            )
                        }
                    }
                }
            }
        }

        root.addView(
            grid
        )

        // =====================================================
        // СБРОС К ГЛАВНОМУ МЕНЮ
        // =====================================================

        val resetButton =
            Button(this)

        resetButton.text =
            "🗑 Сбросить калькулятор"

        resetButton.setTextColor(
            Color.WHITE
        )

        resetButton.setBackgroundColor(
            Color.rgb(
                170,
                45,
                55
            )
        )

        resetButton.textSize =
            16f

        val resetParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )

        resetParams.setMargins(
            0,
            dp(15),
            0,
            dp(10)
        )

        root.addView(
            resetButton,
            resetParams
        )

        resetButton.setOnClickListener {

            showMainMenu()
        }

        setScreen(root)
    }

    // =========================================================
    // ВЫЧИСЛЕНИЕ
    // =========================================================

    private fun calculateExpression(
        expression: String
    ): String {

        return try {

            val clean =
                expression
                    .replace(
                        "×",
                        "*"
                    )
                    .replace(
                        "÷",
                        "/"
                    )
                    .replace(
                        "−",
                        "-"
                    )

            evaluateSimpleExpression(
                clean
            )

        } catch (
            _: Exception
        ) {

            "Ошибка"
        }
    }

    // =========================================================
    // ПРОСТОЕ ВЫЧИСЛЕНИЕ
    // =========================================================

    private fun evaluateSimpleExpression(
        expression: String
    ): String {

        val tokens =
            mutableListOf<String>()

        var current =
            ""

        for (
            char in expression
        ) {

            if (
                char.isDigit() ||
                char == '.'
            ) {

                current += char

            } else {

                if (
                    current.isNotEmpty()
                ) {

                    tokens.add(
                        current
                    )

                    current =
                        ""
                }

                tokens.add(
                    char.toString()
                )
            }
        }

        if (
            current.isNotEmpty()
        ) {

            tokens.add(
                current
            )
        }

        if (
            tokens.isEmpty()
        ) {

            return "0"
        }

        // =====================================================
        // УМНОЖЕНИЕ И ДЕЛЕНИЕ
        // =====================================================

        var i =
            1

        while (
            i < tokens.size - 1
        ) {

            val op =
                tokens[i]

            if (
                op == "*" ||
                op == "/"
            ) {

                val left =
                    tokens[i - 1]
                        .toDouble()

                val right =
                    tokens[i + 1]
                        .toDouble()

                if (
                    op == "/" &&
                    right == 0.0
                ) {

                    return "Ошибка"
                }

                val value =
                    if (
                        op == "*"
                    ) {

                        left * right

                    } else {

                        left / right
                    }

                tokens[i - 1] =
                    value.toString()

                tokens.removeAt(i)

                tokens.removeAt(i)

            } else {

                i += 2
            }
        }

        // =====================================================
        // СЛОЖЕНИЕ И ВЫЧИТАНИЕ
        // =====================================================

        var result =
            tokens[0]
                .toDouble()

        i =
            1

        while (
            i < tokens.size - 1
        ) {

            val op =
                tokens[i]

            val number =
                tokens[i + 1]
                    .toDouble()

            when (op) {

                "+" -> {

                    result +=
                        number
                }

                "-" -> {

                    result -=
                        number
                }
            }

            i += 2
        }

        return formatNumber(
            result
        )
    }

    // =========================================================
    // ГРАФИК
    // =========================================================

    private fun openGraphScreen(
        prefill: String? = null
    ) {

        val root =
            createRoot()

        // =====================================================
        // НАЗАД
        // =====================================================

        addBackButton(root) {

            if (
                currentTask.isNotBlank() &&
                currentAnswer.isNotBlank()
            ) {

                openAIResultScreen(
                    currentTask,
                    currentAnswer,
                    currentExplanation
                )

            } else {

                showMainMenu()
            }
        }

        root.addView(
            makeTitle(
                "📈 График функции"
            )
        )

        // =====================================================
        // СБРОС ГРАФИКА
        // =====================================================

        val resetButton =
            Button(this)

        resetButton.text =
            "🗑 Сбросить график"

        resetButton.setTextColor(
            Color.WHITE
        )

        resetButton.setBackgroundColor(
            Color.rgb(
                170,
                45,
                55
            )
        )

        resetButton.textSize =
            16f

        root.addView(
            resetButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        // =====================================================
        // ВВОД
        // =====================================================

        val input =
            EditText(this)

        input.hint =
            "Например: y = x² - 4x + 3"

        input.setTextColor(
            Color.WHITE
        )

        input.setHintTextColor(
            Color.GRAY
        )

        input.setBackgroundColor(
            cardColor
        )

        input.textSize =
            17f

        input.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        if (
            !prefill.isNullOrBlank()
        ) {

            input.setText(
                prefill
            )
        }

        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(130)
            )
        )

        // =====================================================
        // КНОПКА
        // =====================================================

        val button =
            Button(this)

        button.text =
            "📈 Построить график"

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            blueColor
        )

        button.textSize =
            17f

        root.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        // =====================================================
        // GRAPH VIEW
        // =====================================================

        val graphView =
            GraphView(this)

        root.addView(
            graphView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(650)
            )
        )

        // =====================================================
        // ПОСТРОИТЬ
        // =====================================================

        button.setOnClickListener {

            val function =
                parseFunction(
                    input.text
                        .toString()
                )

            if (
                function == null
            ) {

                Toast.makeText(
                    this,
                    "Не удалось распознать функцию",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                graphView.setFunction(
                    function
                )
            }
        }

        // =====================================================
        // СБРОС
        // =====================================================

        resetButton.setOnClickListener {

            input.setText("")

            graphView.clearGraph()
        }

        // =====================================================
        // АВТОМАТИЧЕСКОЕ ПОСТРОЕНИЕ
        // =====================================================

        if (
            !prefill.isNullOrBlank()
        ) {

            val function =
                parseFunction(
                    prefill
                )

            if (
                function != null
            ) {

                graphView.setFunction(
                    function
                )
            }
        }

        setScreen(root)
    }

    // =========================================================
    // РАСПОЗНАВАНИЕ ФУНКЦИИ
    // =========================================================

    private fun parseFunction(
        original: String
    ): ((Double) -> Double)? {

        var expression =
            original
                .trim()
                .lowercase()
                .replace(
                    "²",
                    "^2"
                )
                .replace(
                    "−",
                    "-"
                )
                .replace(
                    "×",
                    "*"
                )

        if (
            expression.isEmpty()
        ) {

            return null
        }

        expression =
            expression
                .replace(
                    "y=",
                    ""
                )
                .replace(
                    "y =",
                    ""
                )
                .replace(
                    "f(x)=",
                    ""
                )
                .replace(
                    "f(x) =",
                    ""
                )
                .trim()

        if (
            expression.endsWith(
                "=0"
            )
        ) {

            expression =
                expression.dropLast(
                    2
                )
        }

        expression =
            expression.replace(
                " ",
                ""
            )

        // =====================================================
        // x²
        // =====================================================

        if (
            expression == "x^2" ||
            expression == "x**2"
        ) {

            return { x ->

                x.pow(2)
            }
        }

        // =====================================================
        // КВАДРАТНАЯ ФУНКЦИЯ
        // ax² + bx + c
        // =====================================================

        val quadratic =
            Regex(
                """([+-]?\d*\.?\d*)x\^2([+-]?\d*\.?\d*)x([+-]?\d*\.?\d*)"""
            )

        val quadraticMatch =
            quadratic.matchEntire(
                expression
            )

        if (
            quadraticMatch != null
        ) {

            val aText =
                quadraticMatch
                    .groupValues[1]

            val bText =
                quadraticMatch
                    .groupValues[2]

            val cText =
                quadraticMatch
                    .groupValues[3]

            val a =
                when (aText) {

                    "",
                    "+" -> 1.0

                    "-" -> -1.0

                    else ->
                        aText.toDouble()
                }

            val b =
                when (bText) {

                    "",
                    "+" -> 1.0

                    "-" -> -1.0

                    else ->
                        bText.toDouble()
                }

            val c =
                if (
                    cText.isBlank()
                ) {

                    0.0

                } else {

                    cText.toDouble()
                }

            return { x ->

                a * x.pow(2) +
                        b * x +
                        c
            }
        }

        // =====================================================
        // ax² + c
        // =====================================================

        val quadraticSimple =
            Regex(
                """([+-]?\d*\.?\d*)x\^2([+-]\d*\.?\d+)"""
            )

        val simpleMatch =
            quadraticSimple.matchEntire(
                expression
            )

        if (
            simpleMatch != null
        ) {

            val aText =
                simpleMatch
                    .groupValues[1]

            val cText =
                simpleMatch
                    .groupValues[2]

            val a =
                when (aText) {

                    "",
                    "+" -> 1.0

                    "-" -> -1.0

                    else ->
                        aText.toDouble()
                }

            val c =
                cText.toDouble()

            return { x ->

                a * x.pow(2) +
                        c
            }
        }

        // =====================================================
        // ЛИНЕЙНАЯ ФУНКЦИЯ
        // ax + b
        // =====================================================

        val linear =
            Regex(
                """([+-]?\d*\.?\d*)x([+-]\d*\.?\d+)?"""
            )

        val linearMatch =
            linear.matchEntire(
                expression
            )

        if (
            linearMatch != null
        ) {

            val aText =
                linearMatch
                    .groupValues[1]

            val bText =
                linearMatch
                    .groupValues[2]

            val a =
                when (aText) {

                    "",
                    "+" -> 1.0

                    "-" -> -1.0

                    else ->
                        aText.toDouble()
                }

            val b =
                if (
                    bText.isBlank()
                ) {

                    0.0

                } else {

                    bText.toDouble()
                }

            return { x ->

                a * x +
                        b
            }
        }

        return null
    }

    // =========================================================
    // ТАБЛИЦА ОТДЕЛЬНЫМ ЭКРАНОМ
    // =========================================================

    private fun openTableScreen(
        prefill: String? = null
    ) {

        val root =
            createRoot()

        // =====================================================
        // НАЗАД
        // =====================================================

        addBackButton(root) {

            if (
                currentTask.isNotBlank() &&
                currentAnswer.isNotBlank()
            ) {

                openAIResultScreen(
                    currentTask,
                    currentAnswer,
                    currentExplanation
                )

            } else {

                showMainMenu()
            }
        }

        root.addView(
            makeTitle(
                "📊 Таблица значений"
            )
        )

        // =====================================================
        // СБРОС
        // =====================================================

        val resetButton =
            Button(this)

        resetButton.text =
            "🗑 Сбросить таблицу"

        resetButton.setTextColor(
            Color.WHITE
        )

        resetButton.setBackgroundColor(
            Color.rgb(
                170,
                45,
                55
            )
        )

        resetButton.textSize =
            16f

        root.addView(
            resetButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        // =====================================================
        // INPUT
        // =====================================================

        val input =
            EditText(this)

        input.hint =
            "Например: y = x²"

        input.setTextColor(
            Color.WHITE
        )

        input.setHintTextColor(
            Color.GRAY
        )

        input.setBackgroundColor(
            cardColor
        )

        input.textSize =
            17f

        input.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        if (
            !prefill.isNullOrBlank()
        ) {

            input.setText(
                prefill
            )
        }

        root.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(120)
            )
        )

        // =====================================================
        // КНОПКА
        // =====================================================

        val button =
            Button(this)

        button.text =
            "📊 Создать таблицу"

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            blueColor
        )

        button.textSize =
            17f

        root.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        // =====================================================
        // КОНТЕЙНЕР
        // =====================================================

        val tableContainer =
            LinearLayout(this)

        tableContainer.orientation =
            LinearLayout.VERTICAL

        root.addView(
            tableContainer
        )

        // =====================================================
        // СОЗДАТЬ
        // =====================================================

        button.setOnClickListener {

            tableContainer.removeAllViews()

            val function =
                parseFunction(
                    input.text
                        .toString()
                )

            if (
                function == null
            ) {

                Toast.makeText(
                    this,
                    "Не удалось распознать функцию",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            tableContainer.addView(
                createFunctionTable(
                    function
                )
            )
        }

        // =====================================================
        // СБРОС
        // =====================================================

        resetButton.setOnClickListener {

            input.setText("")

            tableContainer.removeAllViews()
        }

        // =====================================================
        // АВТОМАТИЧЕСКАЯ ТАБЛИЦА
        // =====================================================

        if (
            !prefill.isNullOrBlank()
        ) {

            val function =
                parseFunction(
                    prefill
                )

            if (
                function != null
            ) {

                tableContainer.addView(
                    createFunctionTable(
                        function
                    )
                )
            }
        }

        setScreen(root)
    }

    // =========================================================
    // ФОТО
    // =========================================================

    private fun openPhotoScreen() {

        val root =
            createRoot()

        addBackButton(root)

        root.addView(
            makeTitle(
                "📷 Решить по фото"
            )
        )

        root.addView(
            makeCardText(
                "Сфотографируй пример или выбери изображение."
            )
        )

        // =====================================================
        // ВЫБРАТЬ ФОТО
        // =====================================================

        val button =
            Button(this)

        button.text =
            "📷 Выбрать фото"

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            blueColor
        )

        button.textSize =
            17f

        root.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(80)
            )
        )

        // =====================================================
        // ОЧИСТИТЬ / СБРОСИТЬ
        // =====================================================

        val resetButton =
            Button(this)

        resetButton.text =
            "🗑 Сбросить"

        resetButton.setTextColor(
            Color.WHITE
        )

        resetButton.setBackgroundColor(
            Color.rgb(
                170,
                45,
                55
            )
        )

        resetButton.textSize =
            16f

        root.addView(
            resetButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        button.setOnClickListener {

            val intent =
                Intent(
                    Intent.ACTION_GET_CONTENT
                )

            intent.type =
                "image/*"

            startActivityForResult(
                intent,
                1001
            )
        }

        resetButton.setOnClickListener {

            showMainMenu()
        }

        setScreen(root)
    }

    // =========================================================
    // РЕЗУЛЬТАТ ВЫБОРА ФОТО
    // =========================================================

    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == 1001 &&
            resultCode ==
            Activity.RESULT_OK
        ) {

            val uri =
                data?.data
                    ?: return

            uploadPhoto(uri)
        }
    }

    // =========================================================
    // ЗАГРУЗКА ФОТО
    // =========================================================

    private fun uploadPhoto(
        uri: Uri
    ) {

        Toast.makeText(
            this,
            "AI анализирует фото...",
            Toast.LENGTH_SHORT
        ).show()

        try {

            val inputStream =
                contentResolver
                    .openInputStream(uri)
                    ?: return

            val bytes =
                inputStream.readBytes()

            inputStream.close()

            val requestBody =
                bytes.toRequestBody(
                    "image/*"
                        .toMediaType()
                )

            val multipart =
                MultipartBody.Builder()
                    .setType(
                        MultipartBody.FORM
                    )
                    .addFormDataPart(
                        "file",
                        "photo.jpg",
                        requestBody
                    )
                    .build()

            val request =
                Request.Builder()
                    .url(
                        "$serverUrl/v1/calculate-image"
                    )
                    .post(
                        multipart
                    )
                    .build()

            client.newCall(
                request
            ).enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        runOnUiThread {

                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка загрузки фото",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        val responseText =
                            response.body
                                ?.string()
                                ?: ""

                        if (
                            !response.isSuccessful
                        ) {

                            runOnUiThread {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Ошибка сервера: ${response.code}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            return
                        }

                        try {

                            val json =
                                org.json.JSONObject(
                                    responseText
                                )

                            val recognized =
                                json.optString(
                                    "recognized",
                                    ""
                                )

                            val result =
                                json.optString(
                                    "result",
                                    ""
                                )

                            val explanation =
                                json.optString(
                                    "explanation",
                                    ""
                                )

                            runOnUiThread {

                                currentTask =
                                    if (
                                        recognized.isBlank()
                                    ) {

                                        "Задача по фото"

                                    } else {

                                        recognized
                                    }

                                currentAnswer =
                                    result

                                currentExplanation =
                                    explanation

                                addHistory(
                                    currentTask,
                                    result
                                )

                                openAIResultScreen(
                                    currentTask,
                                    result,
                                    explanation
                                )
                            }

                        } catch (
                            _: Exception
                        ) {

                            runOnUiThread {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Ошибка обработки ответа",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )

        } catch (
            _: Exception
        ) {

            Toast.makeText(
                this,
                "Не удалось прочитать фото",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // ИСТОРИЯ
    // =========================================================

    private fun addHistory(
        task: String,
        answer: String
    ) {

        historyList.add(
            "$task\nОтвет: $answer"
        )

        if (
            historyList.size > 50
        ) {

            historyList.removeAt(0)
        }

        saveHistory()
    }

    // =========================================================
    // СОХРАНИТЬ ИСТОРИЮ
    // =========================================================

    private fun saveHistory() {

        val prefs =
            getSharedPreferences(
                "calculator",
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putString(
                "history",
                historyList.joinToString(
                    "\n---\n"
                )
            )
            .apply()
    }

    // =========================================================
    // ЗАГРУЗИТЬ ИСТОРИЮ
    // =========================================================

    private fun loadHistory() {

        val prefs =
            getSharedPreferences(
                "calculator",
                Context.MODE_PRIVATE
            )

        val history =
            prefs.getString(
                "history",
                ""
            ) ?: ""

        if (
            history.isNotBlank()
        ) {

            historyList.clear()

            historyList.addAll(
                history.split(
                    "\n---\n"
                )
            )
        }
    }

    // =========================================================
    // ЭКРАН ИСТОРИИ
    // =========================================================

    private fun openHistoryScreen() {

        val root =
            createRoot()

        addBackButton(root)

        root.addView(
            makeTitle(
                "📚 История решений"
            )
        )

        if (
            historyList.isEmpty()
        ) {

            root.addView(
                makeCardText(
                    "История пока пустая."
                )
            )

        } else {

            historyList
                .asReversed()
                .forEach { item ->

                    val card =
                        makeCard()

                    card.addView(
                        makeCardText(
                            item
                        )
                    )

                    root.addView(
                        card
                    )
                }
        }

        // =====================================================
        // ОЧИСТИТЬ ИСТОРИЮ
        // =====================================================

        val clearButton =
            Button(this)

        clearButton.text =
            "🗑 Очистить историю"

        clearButton.setTextColor(
            Color.WHITE
        )

        clearButton.setBackgroundColor(
            blueColor
        )

        clearButton.textSize =
            16f

        root.addView(
            clearButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        clearButton.setOnClickListener {

            historyList.clear()

            saveHistory()

            openHistoryScreen()
        }

        setScreen(root)
    }
}
