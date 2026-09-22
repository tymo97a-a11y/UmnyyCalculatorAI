package com.smartcalculator.ai

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private val serverUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"

    private val imageServerUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate-image"

    private lateinit var rootContainer: LinearLayout

    private var selectedPhotoUri: Uri? = null

    private var calculatorExpression = ""

    private var firstNumber = 0.0

    private var operator = ""

    private val historyList = mutableListOf<String>()

    private var currentGraphFunction: ((Double) -> Double)? = null

    private lateinit var graphView: GraphView

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                selectedPhotoUri = uri
                openPhotoScreen(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        rootContainer =
            findViewById<View>(R.id.menuAI).parent as LinearLayout

        setupMainMenu()

        loadHistory()
    }

    // =========================================================
    // ГЛАВНОЕ МЕНЮ
    // =========================================================

    private fun setupMainMenu() {

        findViewById<View>(R.id.menuAI).setOnClickListener {
            openAIScreen()
        }

        findViewById<View>(R.id.menuCalculator).setOnClickListener {
            openCalculatorScreen()
        }

        findViewById<View>(R.id.menuGraph).setOnClickListener {
            openGraphScreen()
        }

        findViewById<View>(R.id.menuPhoto).setOnClickListener {
            openPhotoScreen(null)
        }

        findViewById<View>(R.id.menuTable).setOnClickListener {
            openTableScreen()
        }

        findViewById<View>(R.id.menuHistory).setOnClickListener {
            openHistoryScreen()
        }
    }

    // =========================================================
    // ОБЩИЕ ФУНКЦИИ
    // =========================================================

    private fun clearScreen() {

        rootContainer.removeAllViews()

        rootContainer.orientation =
            LinearLayout.VERTICAL

        rootContainer.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        rootContainer.setBackgroundColor(
            Color.rgb(7, 11, 20)
        )
    }

    private fun addBackButton() {

        val button = Button(this)

        button.text = "← Назад"

        button.setTextColor(Color.WHITE)

        button.setOnClickListener {

            setContentView(
                R.layout.activity_main
            )

            rootContainer =
                findViewById<View>(
                    R.id.menuAI
                ).parent as LinearLayout

            setupMainMenu()
        }

        rootContainer.addView(
            button,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )
    }

    private fun addTitle(
        title: String,
        subtitle: String
    ) {

        val titleView = TextView(this)

        titleView.text = title

        titleView.setTextColor(
            Color.WHITE
        )

        titleView.textSize = 27f

        titleView.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        titleView.setPadding(
            0,
            dp(18),
            0,
            dp(5)
        )

        rootContainer.addView(
            titleView
        )

        val subtitleView = TextView(this)

        subtitleView.text = subtitle

        subtitleView.setTextColor(
            Color.rgb(145, 160, 184)
        )

        subtitleView.textSize = 14f

        subtitleView.setPadding(
            0,
            0,
            0,
            dp(20)
        )

        rootContainer.addView(
            subtitleView
        )
    }

    private fun addScrollContent(): LinearLayout {

        val scroll =
            ScrollView(this)

        scroll.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )

        val content =
            LinearLayout(this)

        content.orientation =
            LinearLayout.VERTICAL

        content.setPadding(
            0,
            0,
            0,
            dp(20)
        )

        scroll.addView(content)

        rootContainer.addView(scroll)

        return content
    }

    private fun makeEditText(
        hint: String
    ): EditText {

        val editText =
            EditText(this)

        editText.hint = hint

        editText.setHintTextColor(
            Color.rgb(120, 135, 160)
        )

        editText.setTextColor(
            Color.WHITE
        )

        editText.textSize = 17f

        editText.setPadding(
            dp(16),
            dp(14),
            dp(16),
            dp(14)
        )

        editText.setBackgroundColor(
            Color.rgb(17, 26, 43)
        )

        editText.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            ).apply {
                bottomMargin = dp(12)
            }

        return editText
    }

    private fun makeButton(
        text: String
    ): Button {

        val button =
            Button(this)

        button.text = text

        button.textSize = 16f

        button.setTextColor(
            Color.WHITE
        )

        button.setBackgroundColor(
            Color.rgb(36, 107, 253)
        )

        button.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                dp(58)
            ).apply {
                bottomMargin = dp(10)
            }

        return button
    }

    private fun makeResultText(): TextView {

        val text =
            TextView(this)

        text.setTextColor(
            Color.WHITE
        )

        text.textSize = 17f

        text.setPadding(
            dp(16),
            dp(16),
            dp(16),
            dp(16)
        )

        text.setBackgroundColor(
            Color.rgb(17, 26, 43)
        )

        text.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(15)
            }

        return text
    }

    // =========================================================
    // AI
    // =========================================================

    private fun openAIScreen() {

        clearScreen()

        addBackButton()

        addTitle(
            "🤖 Рассчитать с AI",
            "Опиши задачу обычными словами"
        )

        val content =
            addScrollContent()

        val input =
            makeEditText(
                "Например: 15% от 8400"
            )

        input.minLines = 3

        input.gravity =
            Gravity.TOP

        content.addView(input)

        val calculateButton =
            makeButton(
                "🤖 Рассчитать с AI"
            )

        content.addView(
            calculateButton
        )

        val result =
            makeResultText()

        result.text =
            "Здесь появится результат AI"

        content.addView(result)

        calculateButton.setOnClickListener {

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

            calculateButton.isEnabled =
                false

            result.text =
                "⏳ AI решает задачу..."

            sendToAI(
                text,
                result,
                calculateButton
            )
        }
    }

    private fun sendToAI(
        text: String,
        resultView: TextView,
        button: Button
    ) {

        val json =
            JSONObject()

        json.put(
            "text",
            text
        )

        val body =
            json.toString()
                .toRequestBody(
                    "application/json".toMediaType()
                )

        val request =
            Request.Builder()
                .url(serverUrl)
                .post(body)
                .build()

        client.newCall(request)
            .enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        runOnUiThread {

                            button.isEnabled =
                                true

                            resultView.text =
                                "❌ Ошибка соединения\n\n${e.message}"
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: okhttp3.Response
                    ) {

                        val responseText =
                            response.body?.string()
                                ?: ""

                        runOnUiThread {

                            button.isEnabled =
                                true

                            if (!response.isSuccessful) {

                                resultView.text =
                                    "❌ Ошибка сервера ${response.code}\n\n$responseText"

                                return@runOnUiThread
                            }

                            try {

                                val jsonResponse =
                                    JSONObject(
                                        responseText
                                    )

                                val answer =
                                    jsonResponse.optString(
                                        "result",
                                        "Нет результата"
                                    )

                                val explanation =
                                    jsonResponse.optString(
                                        "explanation",
                                        ""
                                    )

                                resultView.text =
                                    "ИТОГ:\n$answer\n\nОБЪЯСНЕНИЕ:\n$explanation"

                                addHistory(
                                    "$text\n→ $answer"
                                )

                            } catch (e: Exception) {

                                resultView.text =
                                    responseText
                            }
                        }
                    }
                }
            )
    }

    // =========================================================
    // КАЛЬКУЛЯТОР
    // =========================================================

    private fun openCalculatorScreen() {

        clearScreen()

        addBackButton()

        addTitle(
            "🧮 Калькулятор",
            "Обычные математические расчёты"
        )

        val display =
            TextView(this)

        display.text = "0"

        display.setTextColor(
            Color.WHITE
        )

        display.textSize = 30f

        display.gravity =
            Gravity.CENTER_VERTICAL or
                    Gravity.END

        display.setPadding(
            dp(16),
            0,
            dp(16),
            0
        )

        display.setBackgroundColor(
            Color.rgb(17, 26, 43)
        )

        rootContainer.addView(
            display,
            LinearLayout.LayoutParams(
                -1,
                dp(75)
            ).apply {
                bottomMargin = dp(12)
            }
        )

        val grid =
            android.widget.GridLayout(
                this
            )

        grid.columnCount = 4

        rootContainer.addView(
            grid,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val buttons =
            listOf(
                "AC", "⌫", "%", "÷",
                "7", "8", "9", "×",
                "4", "5", "6", "−",
                "1", "2", "3", "+",
                "0", ".", "="
            )

        for (value in buttons) {

            val button =
                Button(this)

            button.text = value

            button.textSize = 20f

            button.setTextColor(
                Color.WHITE
            )

            if (
                value == "÷" ||
                value == "×" ||
                value == "−" ||
                value == "+" ||
                value == "="
            ) {

                button.setBackgroundColor(
                    Color.rgb(36, 107, 253)
                )

            } else {

                button.setBackgroundColor(
                    Color.rgb(17, 26, 43)
                )
            }

            val params =
                android.widget.GridLayout.LayoutParams()

            params.width = 0

            params.height = dp(65)

            params.columnSpec =
                android.widget.GridLayout.spec(
                    android.widget.GridLayout.UNDEFINED,
                    1f
                )

            params.setMargins(
                dp(4),
                dp(4),
                dp(4),
                dp(4)
            )

            if (value == "0") {

                params.columnSpec =
                    android.widget.GridLayout.spec(
                        android.widget.GridLayout.UNDEFINED,
                        2
                    )
            }

            grid.addView(
                button,
                params
            )

            button.setOnClickListener {

                calculatorButton(
                    value,
                    display
                )
            }
        }
    }

    private fun calculatorButton(
        value: String,
        display: TextView
    ) {

        if (value == "AC") {

            calculatorExpression = ""

            firstNumber = 0.0

            operator = ""

            display.text = "0"

            return
        }

        if (value == "⌫") {

            if (
                calculatorExpression.isNotEmpty()
            ) {

                calculatorExpression =
                    calculatorExpression.dropLast(1)

                display.text =
                    if (
                        calculatorExpression.isEmpty()
                    )
                        "0"
                    else
                        calculatorExpression
            }

            return
        }

        if (
            value == "+" ||
            value == "−" ||
            value == "×" ||
            value == "÷"
        ) {

            if (
                calculatorExpression.isNotEmpty()
            ) {

                firstNumber =
                    calculatorExpression
                        .toDoubleOrNull()
                        ?: 0.0

                operator = value

                calculatorExpression = ""
            }

            return
        }

        if (value == "=") {

            val second =
                calculatorExpression
                    .toDoubleOrNull()

            if (
                second != null &&
                operator.isNotEmpty()
            ) {

                val answer =
                    when (operator) {

                        "+" ->
                            firstNumber + second

                        "−" ->
                            firstNumber - second

                        "×" ->
                            firstNumber * second

                        "÷" ->
                            if (second == 0.0)
                                Double.NaN
                            else
                                firstNumber / second

                        else ->
                            second
                    }

                val formatted =
                    formatNumber(
                        answer
                    )

                display.text =
                    formatted

                addHistory(
                    "$firstNumber $operator $second = $formatted"
                )

                calculatorExpression =
                    formatted

                operator = ""
            }

            return
        }

        if (value == "%") {

            val number =
                calculatorExpression
                    .toDoubleOrNull()

            if (number != null) {

                val answer =
                    number / 100.0

                calculatorExpression =
                    formatNumber(answer)

                display.text =
                    calculatorExpression
            }

            return
        }

        if (value == ".") {

            if (
                !calculatorExpression.contains(".")
            ) {

                calculatorExpression +=
                    if (
                        calculatorExpression.isEmpty()
                    )
                        "0."
                    else
                        "."
            }

        } else {

            calculatorExpression += value
        }

        display.text =
            calculatorExpression
    }

    // =========================================================
    // ГРАФИК
    // =========================================================

    private fun openGraphScreen() {

        clearScreen()

        addBackButton()

        addTitle(
            "📈 График функции",
            "Введите функцию и постройте график"
        )

        val content =
            addScrollContent()

        val input =
            makeEditText(
                "Например: y = x^2 - 4x + 3"
            )

        content.addView(input)

        val graphButton =
            makeButton(
                "📈 Построить график"
            )

        content.addView(
            graphButton
        )

        graphView =
            GraphView(this)

        graphView.setBackgroundColor(
            Color.rgb(14, 23, 40)
        )

        content.addView(
            graphView,
            LinearLayout.LayoutParams(
                -1,
                dp(380)
            ).apply {
                bottomMargin = dp(12)
            }
        )

        val info =
            makeResultText()

        info.text =
            "Введите функцию и нажмите «Построить график»"

        content.addView(info)

        graphButton.setOnClickListener {

            val text =
                input.text
                    .toString()
                    .trim()

            if (text.isEmpty()) {

                Toast.makeText(
                    this,
                    "Введите функцию",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val function =
                parseFunction(text)

            if (function == null) {

                info.text =
                    """
                    ❌ Не удалось распознать функцию.

                    Попробуйте:

                    y = x^2
                    y = x^2 - 4x + 3
                    y = 2x + 5
                    x^2 - 9
                    """.trimIndent()

                graphView.clearGraph()

                return@setOnClickListener
            }

            currentGraphFunction =
                function

            graphView.setFunction(
                function,
                -10.0,
                10.0,
                -10.0,
                10.0
            )

            info.text =
                "✅ График построен\n\nФункция: $text"
        }
    }

    // =========================================================
    // РАСПОЗНАВАНИЕ ФУНКЦИИ
    // =========================================================

    private fun parseFunction(
        original: String
    ): ((Double) -> Double)? {

        var text =
            original
                .lowercase(Locale.getDefault())
                .replace(" ", "")
                .replace("²", "^2")
                .replace("−", "-")
                .replace("×", "*")

        if (text.startsWith("y=")) {
            text =
                text.substring(2)
        }

        if (text.startsWith("f(x)=")) {
            text =
                text.substring(5)
        }

        if (text.endsWith("=0")) {
            text =
                text.dropLast(2)
        }

        if (
            text == "x^2" ||
            text == "x*x"
        ) {

            return { x ->
                x.pow(2)
            }
        }

        if (text == "x") {

            return { x ->
                x
            }
        }

        if (text == "-x") {

            return { x ->
                -x
            }
        }

        val quadratic =
            Regex(
                """^([+-]?\d*\.?\d*)x\^2([+-]\d*\.?\d*)x([+-]\d*\.?\d+)?$"""
            )

        val quadraticMatch =
            quadratic.matchEntire(text)

        if (quadraticMatch != null) {

            val aText =
                quadraticMatch.groupValues[1]

            val bText =
                quadraticMatch.groupValues[2]

            val cText =
                quadraticMatch.groupValues[3]

            val a =
                when (aText) {
                    "", "+" -> 1.0
                    "-" -> -1.0
                    else ->
                        aText.toDoubleOrNull()
                            ?: return null
                }

            val b =
                when (bText) {
                    "", "+" -> 1.0
                    "-" -> -1.0
                    else ->
                        bText.toDoubleOrNull()
                            ?: return null
                }

            val c =
                if (cText.isEmpty())
                    0.0
                else
                    cText.toDoubleOrNull()
                        ?: return null

            return { x ->

                a * x * x +
                        b * x +
                        c
            }
        }

        val linear =
            Regex(
                """^([+-]?\d*\.?\d*)x([+-]\d*\.?\d+)?$"""
            )

        val linearMatch =
            linear.matchEntire(text)

        if (linearMatch != null) {

            val aText =
                linearMatch.groupValues[1]

            val bText =
                linearMatch.groupValues[2]

            val a =
                when (aText) {
                    "", "+" -> 1.0
                    "-" -> -1.0
                    else ->
                        aText.toDoubleOrNull()
                            ?: return null
                }

            val b =
                if (bText.isEmpty())
                    0.0
                else
                    bText.toDoubleOrNull()
                        ?: return null

            return { x ->
                a * x + b
            }
        }

        return createSimpleFunction(text)
    }

    private fun createSimpleFunction(
        text: String
    ): ((Double) -> Double)? {

        if (!text.contains("x")) {

            val number =
                text.toDoubleOrNull()
                    ?: return null

            return {
                number
            }
        }

        return { x ->

            try {

                val expression =
                    text
                        .replace(
                            "x^2",
                            "${x * x}"
                        )
                        .replace(
                            "x",
                            "($x)"
                        )

                evaluateExpression(
                    expression
                )

            } catch (_: Exception) {

                Double.NaN
            }
        }
    }

    private fun evaluateExpression(
        expression: String
    ): Double {

        val parts =
            expression.split(
                Regex("(?=[+-])")
            )

        var result = 0.0

        for (part in parts) {

            if (part.isBlank()) {
                continue
            }

            result +=
                part.toDouble()
        }

        return result
    }

    // =========================================================
    // ФОТО
    // =========================================================

    private fun openPhotoScreen(
        uri: Uri?
    ) {

        clearScreen()

        addBackButton()

        addTitle(
            "📷 Решить по фото",
            "Сфотографируй или выбери математическую задачу"
        )

        val content =
            addScrollContent()

        val chooseButton =
            makeButton(
                "📷 Выбрать фото"
            )

        content.addView(
            chooseButton
        )

        val preview =
            ImageView(this)

        preview.adjustViewBounds =
            true

        preview.setPadding(
            dp(5),
            dp(5),
            dp(5),
            dp(5)
        )

        content.addView(
            preview,
            LinearLayout.LayoutParams(
                -1,
                dp(280)
            ).apply {
                bottomMargin = dp(10)
            }
        )

        val result =
            makeResultText()

        result.text =
            "Выберите фотографию задачи"

        content.addView(result)

        chooseButton.setOnClickListener {

            photoPicker.launch(
                "image/*"
            )
        }

        if (uri != null) {

            preview.setImageURI(uri)

            result.text =
                "⏳ Распознавание задачи..."

            sendPhotoToAI(
                uri,
                result
            )
        }
    }

    private fun sendPhotoToAI(
        uri: Uri,
        resultView: TextView
    ) {

        try {

            val inputStream =
                contentResolver
                    .openInputStream(uri)
                    ?: throw IOException(
                        "Не удалось открыть изображение"
                    )

            val bytes =
                inputStream.use {
                    it.readBytes()
                }

            val fileName =
                getFileName(uri)

            val imageBody =
                bytes.toRequestBody(
                    "image/jpeg".toMediaType()
                )

            val multipart =
                MultipartBody.Builder()
                    .setType(
                        MultipartBody.FORM
                    )
                    .addFormDataPart(
                        "file",
                        fileName,
                        imageBody
                    )
                    .build()

            val request =
                Request.Builder()
                    .url(imageServerUrl)
                    .post(multipart)
                    .build()

            client.newCall(request)
                .enqueue(
                    object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {

                            runOnUiThread {

                                resultView.text =
                                    "❌ Ошибка соединения\n\n${e.message}"
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: okhttp3.Response
                        ) {

                            val responseText =
                                response.body?.string()
                                    ?: ""

                            runOnUiThread {

                                if (!response.isSuccessful) {

                                    resultView.text =
                                        "❌ Ошибка ${response.code}\n\n$responseText"

                                    return@runOnUiThread
                                }

                                try {

                                    val json =
                                        JSONObject(
                                            responseText
                                        )

                                    val recognized =
                                        json.optString(
                                            "recognized",
                                            ""
                                        )

                                    val answer =
                                        json.optString(
                                            "result",
                                            ""
                                        )

                                    val explanation =
                                        json.optString(
                                            "explanation",
                                            ""
                                        )

                                    resultView.text =
                                        """
                                        📝 РАСПОЗНАНО:

                                        $recognized

                                        ✅ ИТОГ:

                                        $answer

                                        💡 ОБЪЯСНЕНИЕ:

                                        $explanation
                                        """.trimIndent()

                                    addHistory(
                                        "📷 $recognized\n→ $answer"
                                    )

                                } catch (
                                    e: Exception
                                ) {

                                    resultView.text =
                                        responseText
                                }
                            }
                        }
                    }
                )

        } catch (e: Exception) {

            resultView.text =
                "❌ Не удалось отправить фото\n\n${e.message}"
        }
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var name =
            "photo.jpg"

        val cursor =
            contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )

        cursor?.use {

            val index =
                it.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

            if (
                index >= 0 &&
                it.moveToFirst()
            ) {

                name =
                    it.getString(index)
            }
        }

        return name
    }

    // =========================================================
    // ТАБЛИЦА
    // =========================================================

    private fun openTableScreen() {

        clearScreen()

        addBackButton()

        addTitle(
            "📊 Таблица значений",
            "Значения выбранной функции"
        )

        val content =
            addScrollContent()

        val input =
            makeEditText(
                "Например: y = x^2 - 4x + 3"
            )

        content.addView(input)

        val button =
            makeButton(
                "📊 Построить таблицу"
            )

        content.addView(button)

        val table =
            makeResultText()

        table.text =
            "Введите функцию"

        content.addView(table)

        button.setOnClickListener {

            val function =
                parseFunction(
                    input.text.toString()
                )

            if (function == null) {

                table.text =
                    "❌ Не удалось распознать функцию"

                return@setOnClickListener
            }

            val builder =
                StringBuilder()

            builder.append(
                "      X              Y\n"
            )

            builder.append(
                "-------------------------\n"
            )

            for (x in -5..5) {

                val y =
                    function(
                        x.toDouble()
                    )

                builder.append(
                    String.format(
                        Locale.US,
                        "%7d     %10.3f\n",
                        x,
                        y
                    )
                )
            }

            table.text =
                builder.toString()
        }
    }

    // =========================================================
    // ИСТОРИЯ
    // =========================================================

    private fun openHistoryScreen() {

        clearScreen()

        addBackButton()

        addTitle(
            "📚 История решений",
            "Ваши последние расчёты"
        )

        val content =
            addScrollContent()

        if (historyList.isEmpty()) {

            val empty =
                makeResultText()

            empty.text =
                "История пока пустая."

            content.addView(empty)

            return
        }

        historyList
            .reversed()
            .forEachIndexed { index, item ->

                val card =
                    makeResultText()

                card.text =
                    "${index + 1}. $item"

                content.addView(card)
            }
    }

    private fun addHistory(
        text: String
    ) {

        historyList.add(text)

        while (
            historyList.size > 50
        ) {

            historyList.removeAt(0)
        }

        saveHistory()
    }

    private fun saveHistory() {

        getPreferences(
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "history",
                historyList.joinToString(
                    "\n---ITEM---\n"
                )
            )
            .apply()
    }

    private fun loadHistory() {

        val saved =
            getPreferences(
                MODE_PRIVATE
            )
                .getString(
                    "history",
                    ""
                )
                ?: ""

        if (saved.isNotEmpty()) {

            historyList.clear()

            historyList.addAll(
                saved.split(
                    "\n---ITEM---\n"
                )
            )
        }
    }

    // =========================================================
    // ВСПОМОГАТЕЛЬНОЕ
    // =========================================================

    private fun formatNumber(
        value: Double
    ): String {

        if (value.isNaN()) {
            return "Ошибка"
        }

        if (value.isInfinite()) {
            return "∞"
        }

        return if (
            value % 1.0 == 0.0
        ) {

            value
                .toLong()
                .toString()

        } else {

            String.format(
                Locale.US,
                "%.8f",
                value
            )
                .trimEnd('0')
                .trimEnd('.')
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }
}
