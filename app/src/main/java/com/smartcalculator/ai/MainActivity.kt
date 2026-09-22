package com.smartcalculator.ai

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var calculatorDisplay: TextView
    private lateinit var history: TextView

    private lateinit var calculateButton: Button
    private lateinit var photoButton: Button
    private lateinit var clearPhotoButton: Button

    private lateinit var photoPreview: ImageView
    private lateinit var photoStatus: TextView

    private lateinit var graphView: GraphView
    private lateinit var graphButton: Button
    private lateinit var tableButton: Button
    private lateinit var graphInfo: TextView

    private val client = OkHttpClient()

    private val serverUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"

    private val imageServerUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate-image"

    private val historyList = mutableListOf<String>()

    private var selectedPhotoUri: Uri? = null

    private var calculatorValue = ""
    private var firstNumber = 0.0
    private var currentOperator = ""
    private var waitingForSecondNumber = false

    private var currentGraphFunction: ((Double) -> Double)? = null

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                selectedPhotoUri = uri

                photoPreview.setImageURI(uri)
                photoPreview.visibility = View.VISIBLE

                photoStatus.visibility = View.VISIBLE
                photoStatus.text =
                    "📷 Фото выбрано. Нажми «Рассчитать по фото»."

                photoButton.text =
                    "🤖  Рассчитать фото с AI"

                result.text =
                    "Фото готово к обработке AI."

                Toast.makeText(
                    this,
                    "Фото выбрано",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        result = findViewById(R.id.result)
        calculatorDisplay = findViewById(R.id.calculatorDisplay)
        history = findViewById(R.id.history)

        calculateButton = findViewById(R.id.calculateButton)
        photoButton = findViewById(R.id.photoButton)
        clearPhotoButton = findViewById(R.id.clearPhotoButton)

        photoPreview = findViewById(R.id.photoPreview)
        photoStatus = findViewById(R.id.photoStatus)

        graphView = findViewById(R.id.graphView)
        graphButton = findViewById(R.id.graphButton)
        tableButton = findViewById(R.id.tableButton)
        graphInfo = findViewById(R.id.graphInfo)

        setupCalculator()
        setupAI()
        setupPhoto()
        setupGraph()
        loadHistory()
    }

    // =========================================================
    // AI
    // =========================================================

    private fun setupAI() {

        calculateButton.setOnClickListener {

            val text = input.text.toString().trim()

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
    }

    private fun sendToAI(text: String) {

        calculateButton.isEnabled = false

        result.text = "⏳ AI решает задачу..."

        calculatorDisplay.text =
            "Пожалуйста, подождите..."

        val json = JSONObject()

        json.put("text", text)

        val body = json
            .toString()
            .toRequestBody(
                "application/json".toMediaType()
            )

        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        calculateButton.isEnabled = true

                        result.text =
                            "❌ Ошибка соединения с сервером"

                        calculatorDisplay.text =
                            "Проверь интернет-соединение."

                        Toast.makeText(
                            this@MainActivity,
                            "Не удалось подключиться к AI",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: okhttp3.Response
                ) {

                    response.use {

                        val responseText =
                            it.body?.string().orEmpty()

                        if (!it.isSuccessful) {

                            runOnUiThread {

                                calculateButton.isEnabled = true

                                result.text =
                                    "❌ Ошибка AI\nКод: ${it.code}"

                                calculatorDisplay.text =
                                    responseText.take(500)
                            }

                            return
                        }

                        try {

                            val jsonResponse =
                                JSONObject(responseText)

                            val answer =
                                jsonResponse.optString(
                                    "result",
                                    "Нет результата"
                                )

                            val explanation =
                                jsonResponse.optString(
                                    "explanation",
                                    "Решение выполнено AI."
                                )

                            runOnUiThread {

                                calculateButton.isEnabled = true

                                result.text =
                                    "ИТОГ:\n$answer\n\nОБЪЯСНЕНИЕ:\n$explanation"

                                calculatorDisplay.text =
                                    createStepByStepSolution(text, answer, explanation)

                                addHistory(
                                    "$text → $answer"
                                )

                                tryBuildGraphFromText(text)
                            }

                        } catch (e: Exception) {

                            runOnUiThread {

                                calculateButton.isEnabled = true

                                result.text =
                                    "❌ Не удалось обработать ответ AI"

                                calculatorDisplay.text =
                                    responseText.take(1000)
                            }
                        }
                    }
                }
            }
        )
    }

    // =========================================================
    // PHOTO
    // =========================================================

    private fun setupPhoto() {

        photoButton.setOnClickListener {

            val uri = selectedPhotoUri

            if (uri == null) {

                photoPicker.launch("image/*")

            } else {

                sendPhotoToAI(uri)
            }
        }

        clearPhotoButton.setOnClickListener {

            clearPhoto()
        }
    }

    private fun sendPhotoToAI(uri: Uri) {

        photoButton.isEnabled = false
        clearPhotoButton.isEnabled = false

        photoStatus.visibility = View.VISIBLE

        photoStatus.text =
            "⏳ Распознаваем пример..."

        result.text =
            "AI анализирует фотографию..."

        Thread {

            try {

                val bytes =
                    contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes() }

                if (bytes == null) {

                    runOnUiThread {

                        photoButton.isEnabled = true
                        clearPhotoButton.isEnabled = true

                        photoStatus.text =
                            "❌ Не удалось прочитать фото"
                    }

                    return@Thread
                }

                val mimeType =
                    contentResolver
                        .getType(uri)
                        ?: "image/jpeg"

                val fileName =
                    getFileName(uri)
                        ?: "photo.jpg"

                val requestBody =
                    bytes.toRequestBody(
                        mimeType.toMediaType()
                    )

                val multipartBody =
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            fileName,
                            requestBody
                        )
                        .build()

                val request =
                    Request.Builder()
                        .url(imageServerUrl)
                        .post(multipartBody)
                        .build()

                client.newCall(request).enqueue(
                    object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {

                            runOnUiThread {

                                photoButton.isEnabled = true
                                clearPhotoButton.isEnabled = true

                                photoStatus.text =
                                    "❌ Ошибка соединения"

                                result.text =
                                    "Не удалось отправить фото на AI."
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: okhttp3.Response
                        ) {

                            response.use {

                                val responseText =
                                    it.body?.string().orEmpty()

                                if (!it.isSuccessful) {

                                    runOnUiThread {

                                        photoButton.isEnabled = true
                                        clearPhotoButton.isEnabled = true

                                        photoStatus.text =
                                            "❌ Ошибка AI"

                                        result.text =
                                            "Код: ${it.code}\n\n" +
                                                    responseText.take(500)
                                    }

                                    return
                                }

                                try {

                                    val json =
                                        JSONObject(responseText)

                                    val recognized =
                                        json.optString(
                                            "recognized",
                                            ""
                                        )

                                    val answer =
                                        json.optString(
                                            "result",
                                            "Нет результата"
                                        )

                                    val explanation =
                                        json.optString(
                                            "explanation",
                                            ""
                                        )

                                    runOnUiThread {

                                        photoButton.isEnabled = true
                                        clearPhotoButton.isEnabled = true

                                        photoStatus.text =
                                            "✅ Фото успешно обработано AI"

                                        result.text =
                                            "РАСПОЗНАНО:\n" +
                                                    recognized +
                                                    "\n\nИТОГ:\n" +
                                                    answer +
                                                    "\n\nОБЪЯСНЕНИЕ:\n" +
                                                    explanation

                                        calculatorDisplay.text =
                                            createStepByStepSolution(
                                                recognized,
                                                answer,
                                                explanation
                                            )

                                        addHistory(
                                            "Фото: $recognized → $answer"
                                        )

                                        tryBuildGraphFromText(
                                            recognized
                                        )
                                    }

                                } catch (e: Exception) {

                                    runOnUiThread {

                                        photoButton.isEnabled = true
                                        clearPhotoButton.isEnabled = true

                                        photoStatus.text =
                                            "❌ Ошибка обработки ответа"

                                        result.text =
                                            responseText.take(1000)
                                    }
                                }
                            }
                        }
                    }
                )

            } catch (e: Exception) {

                runOnUiThread {

                    photoButton.isEnabled = true
                    clearPhotoButton.isEnabled = true

                    photoStatus.text =
                        "❌ Ошибка чтения фотографии"

                    result.text =
                        e.message ?: "Неизвестная ошибка"
                }
            }

        }.start()
    }

    private fun getFileName(uri: Uri): String? {

        var name: String? = null

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        return name
    }

    private fun clearPhoto() {

        selectedPhotoUri = null

        photoPreview.setImageDrawable(null)

        photoPreview.visibility =
            View.GONE

        photoStatus.visibility =
            View.GONE

        photoButton.text =
            "📷  Рассчитать по фото"

        result.text =
            "Здесь появится результат"

        calculatorDisplay.text =
            "Введите задачу выше, и AI покажет решение."
    }

    // =========================================================
    // GRAPH
    // =========================================================

    private fun setupGraph() {

        graphButton.setOnClickListener {

            val text =
                input.text.toString().trim()

            if (text.isEmpty()) {

                Toast.makeText(
                    this,
                    "Введите функцию",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (!tryBuildGraphFromText(text)) {

                Toast.makeText(
                    this,
                    "Попробуйте: y = x² - 4x + 3",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        tableButton.setOnClickListener {

            val text =
                input.text.toString().trim()

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

                Toast.makeText(
                    this,
                    "Не удалось определить функцию",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            showValueTable(function)
        }
    }

    private fun tryBuildGraphFromText(
        text: String
    ): Boolean {

        val parsed =
            parseFunction(text)
                ?: return false

        currentGraphFunction =
            parsed.function

        graphView.setFunction(
            parsed.function
        )

        graphInfo.text =
            "📈 Функция:\n${parsed.normalized}\n\n" +
                    "Диапазон X: -10 ... 10\n" +
                    "График построен автоматически."

        return true
    }

    private data class ParsedFunction(
        val function: (Double) -> Double,
        val normalized: String
    )

    private fun parseFunction(
        originalText: String
    ): ParsedFunction? {

        var text =
            originalText
                .lowercase(Locale.getDefault())
                .replace(" ", "")
                .replace("−", "-")
                .replace("–", "-")
                .replace("×", "*")
                .replace("²", "^2")
                .replace("³", "^3")

        if (text.startsWith("y=")) {
            text = text.substring(2)
        }

        if (text.startsWith("y:")) {
            text = text.substring(2)
        }

        if (text.startsWith("f(x)=")) {
            text = text.substring(5)
        }

        text =
            text.replace("**", "^")

        if (!text.contains("x")) {
            return null
        }

        // -----------------------------------------------------
        // y = x²
        // y = x^2
        // -----------------------------------------------------

        if (
            text == "x^2" ||
            text == "x2"
        ) {

            return ParsedFunction(
                function = { x -> x * x },
                normalized = "y = x²"
            )
        }

        // -----------------------------------------------------
        // y = x
        // -----------------------------------------------------

        if (text == "x") {

            return ParsedFunction(
                function = { x -> x },
                normalized = "y = x"
            )
        }

        // -----------------------------------------------------
        // y = 2x
        // y = -3x
        // -----------------------------------------------------

        val linearRegex =
            Regex(
                "^([+-]?\\d*\\.?\\d*)x([+-]\\d*\\.?\\d+)?$"
            )

        val linearMatch =
            linearRegex.matchEntire(text)

        if (linearMatch != null) {

            val aText =
                linearMatch.groupValues[1]

            val bText =
                linearMatch.groupValues[2]

            val a =
                when (aText) {
                    "", "+" -> 1.0
                    "-" -> -1.0
                    else -> aText.toDoubleOrNull()
                        ?: return null
                }

            val b =
                if (bText.isEmpty()) {
                    0.0
                } else {
                    bText.toDoubleOrNull()
                        ?: return null
                }

            return ParsedFunction(
                function = { x ->
                    a * x + b
                },
                normalized =
                    "y = ${formatNumber(a)}x " +
                            "${if (b >= 0) "+" else "-"} " +
                            "${formatNumber(abs(b))}"
            )
        }

        // -----------------------------------------------------
        // y = ax² + bx + c
        // -----------------------------------------------------

        val quadraticRegex =
            Regex(
                "^([+-]?\\d*\\.?\\d*)x\\^2" +
                        "([+-]\\d*\\.?\\d*)x?" +
                        "([+-]\\d*\\.?\\d+)?$"
            )

        val quadraticMatch =
            quadraticRegex.matchEntire(text)

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
                    else -> aText.toDoubleOrNull()
                        ?: return null
                }

            val b =
                if (bText.isEmpty()) {
                    0.0
                } else {
                    bText.toDoubleOrNull()
                        ?: return null
                }

            val c =
                if (cText.isEmpty()) {
                    0.0
                } else {
                    cText.toDoubleOrNull()
                        ?: return null
                }

            return ParsedFunction(
                function = { x ->
                    a * x * x + b * x + c
                },
                normalized =
                    buildQuadraticText(
                        a,
                        b,
                        c
                    )
            )
        }

        // -----------------------------------------------------
        // Особый случай: x² - 4x + 3
        // -----------------------------------------------------

        if (
            text.contains("x^2") &&
            text.contains("x")
        ) {

            val expression =
                text

            val function:
                    (Double) -> Double =
                { x ->

                    evaluateSimpleFunction(
                        expression,
                        x
                    )
                }

            return ParsedFunction(
                function = function,
                normalized =
                    "y = $expression"
            )
        }

        return null
    }

    private fun evaluateSimpleFunction(
        expression: String,
        x: Double
    ): Double {

        var text = expression

        text =
            text.replace(
                "x^2",
                "(${x * x})"
            )

        text =
            text.replace(
                "x",
                "($x)"
            )

        return evaluateBasicExpression(
            text
        )
    }

    private fun evaluateBasicExpression(
        expression: String
    ): Double {

        val clean =
            expression
                .replace("(", "")
                .replace(")", "")

        val parts =
            Regex(
                "([+-]?\\d*\\.?\\d+)"
            ).findAll(clean)

        var total = 0.0

        for (match in parts) {

            val value =
                match.value.toDoubleOrNull()
                    ?: continue

            total += value
        }

        return total
    }

    private fun buildQuadraticText(
        a: Double,
        b: Double,
        c: Double
    ): String {

        val aText =
            if (a == 1.0) {
                ""
            } else if (a == -1.0) {
                "-"
            } else {
                formatNumber(a)
            }

        val bText =
            if (b == 0.0) {
                ""
            } else {
                if (b > 0) {
                    "+ ${formatNumber(b)}x"
                } else {
                    "- ${formatNumber(abs(b))}x"
                }
            }

        val cText =
            if (c == 0.0) {
                ""
            } else {
                if (c > 0) {
                    "+ ${formatNumber(c)}"
                } else {
                    "- ${formatNumber(abs(c))}"
                }
            }

        return "y = ${aText}x² $bText $cText"
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    // =========================================================
    // TABLE
    // =========================================================

    private fun showValueTable(
        parsedFunction: ParsedFunction
    ) {

        val values = StringBuilder()

        values.append(
            "📊 ТАБЛИЦА ЗНАЧЕНИЙ\n\n"
        )

        values.append(
            "x\t\t y\n"
        )

        values.append(
            "────────────\n"
        )

        for (x in -5..5) {

            val y =
                try {
                    parsedFunction.function(
                        x.toDouble()
                    )
                } catch (_: Exception) {
                    Double.NaN
                }

            if (y.isFinite()) {

                values.append(
                    "$x\t\t${formatNumber(y)}\n"
                )
            }
        }

        result.text =
            values.toString()

        calculatorDisplay.text =
            "Для функции:\n" +
                    parsedFunction.normalized +
                    "\n\nТаблица построена для x от -5 до 5."

        addHistory(
            "Таблица: ${parsedFunction.normalized}"
        )
    }

    // =========================================================
    // STEP BY STEP
    // =========================================================

    private fun createStepByStepSolution(
        question: String,
        answer: String,
        explanation: String
    ): String {

        val normalized =
            question
                .lowercase(Locale.getDefault())
                .replace(" ", "")
                .replace("²", "^2")
                .replace("−", "-")
                .replace("–", "-")

        val quadratic =
            parseQuadraticEquation(
                normalized
            )

        if (quadratic != null) {

            val a = quadratic.first
            val b = quadratic.second
            val c = quadratic.third

            val d =
                b * b - 4.0 * a * c

            val text =
                StringBuilder()

            text.append(
                "1. Приводим уравнение к виду:\n"
            )

            text.append(
                "${formatNumber(a)}x² " +
                        "${if (b >= 0) "+" else "-"} " +
                        "${formatNumber(abs(b))}x " +
                        "${if (c >= 0) "+" else "-"} " +
                        "${formatNumber(abs(c))} = 0\n\n"
            )

            text.append(
                "2. Находим дискриминант:\n"
            )

            text.append(
                "D = b² − 4ac\n"
            )

            text.append(
                "D = ${formatNumber(b)}² − " +
                        "4 × ${formatNumber(a)} × " +
                        "${formatNumber(c)}\n"
            )

            text.append(
                "D = ${formatNumber(d)}\n\n"
            )

            if (d > 0) {

                val x1 =
                    (-b + sqrt(d)) /
                            (2.0 * a)

                val x2 =
                    (-b - sqrt(d)) /
                            (2.0 * a)

                text.append(
                    "3. Находим корни:\n"
                )

                text.append(
                    "x₁ = ${formatNumber(x1)}\n"
                )

                text.append(
                    "x₂ = ${formatNumber(x2)}\n\n"
                )

                text.append(
                    "4. Ответ:\n"
                )

                text.append(
                    "x₁ = ${formatNumber(x1)}, " +
                            "x₂ = ${formatNumber(x2)}"
                )

            } else if (d == 0.0) {

                val x =
                    -b / (2.0 * a)

                text.append(
                    "3. Один корень:\n"
                )

                text.append(
                    "x = ${formatNumber(x)}\n\n"
                )

                text.append(
                    "4. Ответ:\n"
                )

                text.append(
                    "x = ${formatNumber(x)}"
                )

            } else {

                text.append(
                    "3. Дискриминант меньше нуля.\n\n"
                )

                text.append(
                    "4. Действительных корней нет."
                )
            }

            return text.toString()
        }

        val linear =
            parseLinearEquation(
                normalized
            )

        if (linear != null) {

            val a = linear.first
            val b = linear.second

            if (a != 0.0) {

                val x =
                    -b / a

                return """
1. Переносим свободный член.

2. Получаем:
${formatNumber(a)}x = ${formatNumber(-b)}

3. Делим обе части на ${formatNumber(a)}.

4. Ответ:
x = ${formatNumber(x)}
                """.trimIndent()
            }
        }

        return """
РЕШЕНИЕ

Задача:
$question

Ответ:
$answer

Объяснение AI:
$explanation
        """.trimIndent()
    }

    private fun parseQuadraticEquation(
        text: String
    ): Triple<Double, Double, Double>? {

        var clean =
            text

        clean =
            clean.replace("=0", "")

        val regex =
            Regex(
                "^([+-]?\\d*\\.?\\d*)x\\^2" +
                        "([+-]\\d*\\.?\\d*)x" +
                        "([+-]\\d*\\.?\\d+)?$"
            )

        val match =
            regex.matchEntire(clean)
                ?: return null

        val aText =
            match.groupValues[1]

        val bText =
            match.groupValues[2]

        val cText =
            match.groupValues[3]

        val a =
            when (aText) {
                "", "+" -> 1.0
                "-" -> -1.0
                else -> aText.toDoubleOrNull()
                    ?: return null
            }

        val b =
            bText.toDoubleOrNull()
                ?: return null

        val c =
            if (cText.isEmpty()) {
                0.0
            } else {
                cText.toDoubleOrNull()
                    ?: return null
            }

        return Triple(a, b, c)
    }

    private fun parseLinearEquation(
        text: String
    ): Pair<Double, Double>? {

        val clean =
            text.replace("=0", "")

        val regex =
            Regex(
                "^([+-]?\\d*\\.?\\d*)x" +
                        "([+-]\\d*\\.?\\d+)?$"
            )

        val match =
            regex.matchEntire(clean)
                ?: return null

        val aText =
            match.groupValues[1]

        val bText =
            match.groupValues[2]

        val a =
            when (aText) {
                "", "+" -> 1.0
                "-" -> -1.0
                else -> aText.toDoubleOrNull()
                    ?: return null
            }

        val b =
            if (bText.isEmpty()) {
                0.0
            } else {
                bText.toDoubleOrNull()
                    ?: return null
            }

        return Pair(a, b)
    }

    // =========================================================
    // Обычный калькулятор
    // =========================================================

    private fun setupCalculator() {

        val numberButtons =
            mapOf(
                R.id.button0 to "0",
                R.id.button1 to "1",
                R.id.button2 to "2",
                R.id.button3 to "3",
                R.id.button4 to "4",
                R.id.button5 to "5",
                R.id.button6 to "6",
                R.id.button7 to "7",
                R.id.button8 to "8",
                R.id.button9 to "9"
            )

        for ((id, number) in numberButtons) {

            findViewById<Button>(id)
                .setOnClickListener {

                    addNumber(number)
                }
        }

        findViewById<Button>(
            R.id.buttonDot
        ).setOnClickListener {

            addNumber(".")
        }

        findViewById<Button>(
            R.id.buttonPlus
        ).setOnClickListener {

            chooseOperator("+")
        }

        findViewById<Button>(
            R.id.buttonMinus
        ).setOnClickListener {

            chooseOperator("-")
        }

        findViewById<Button>(
            R.id.buttonMultiply
        ).setOnClickListener {

            chooseOperator("*")
        }

        findViewById<Button>(
            R.id.buttonDivide
        ).setOnClickListener {

            chooseOperator("/")
        }

        findViewById<Button>(
            R.id.buttonEquals
        ).setOnClickListener {

            calculate()
        }

        findViewById<Button>(
            R.id.buttonClear
        ).setOnClickListener {

            clearCalculator()
        }

        updateCalculatorDisplay()
    }

    private fun addNumber(value: String) {

        if (waitingForSecondNumber) {

            calculatorValue = ""

            waitingForSecondNumber = false
        }

        if (
            value == "." &&
            calculatorValue.contains(".")
        ) {
            return
        }

        calculatorValue += value

        updateCalculatorDisplay()
    }

    private fun chooseOperator(
        operator: String
    ) {

        if (calculatorValue.isEmpty()) {
            return
        }

        firstNumber =
            calculatorValue.toDoubleOrNull()
                ?: return

        currentOperator =
            operator

        waitingForSecondNumber = true
    }

    private fun calculate() {

        if (
            currentOperator.isEmpty() ||
            calculatorValue.isEmpty()
        ) {
            return
        }

        val secondNumber =
            calculatorValue.toDoubleOrNull()
                ?: return

        val answer =
            when (currentOperator) {

                "+" ->
                    firstNumber + secondNumber

                "-" ->
                    firstNumber - secondNumber

                "*" ->
                    firstNumber * secondNumber

                "/" -> {

                    if (secondNumber == 0.0) {

                        result.text =
                            "На ноль делить нельзя."

                        return
                    }

                    firstNumber / secondNumber
                }

                else -> 0.0
            }

        val expression =
            "${formatNumber(firstNumber)} " +
                    "$currentOperator " +
                    "${formatNumber(secondNumber)}"

        val answerText =
            formatNumber(answer)

        calculatorValue =
            answerText

        currentOperator = ""

        waitingForSecondNumber = true

        updateCalculatorDisplay()

        result.text =
            "ИТОГ:\n$answerText\n\n" +
                    "РЕШЕНИЕ:\n" +
                    "$expression = $answerText"

        calculatorDisplay.text =
            "$expression = $answerText"

        addHistory(
            "$expression = $answerText"
        )
    }

    private fun clearCalculator() {

        calculatorValue = ""

        firstNumber = 0.0

        currentOperator = ""

        waitingForSecondNumber = false

        updateCalculatorDisplay()
    }

    private fun updateCalculatorDisplay() {

        if (calculatorValue.isEmpty()) {

            calculatorDisplay.text =
                "0"

        } else {

            calculatorDisplay.text =
                calculatorValue
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private fun addHistory(
        text: String
    ) {

        historyList.add(
            0,
            text
        )

        if (historyList.size > 20) {
            historyList.removeAt(
                historyList.lastIndex
            )
        }

        saveHistory()

        updateHistoryView()
    }

    private fun updateHistoryView() {

        if (historyList.isEmpty()) {

            history.text =
                "История пока пуста"

            return
        }

        history.text =
            historyList.joinToString(
                separator = "\n\n"
            ) { item ->
                "• $item"
            }
    }

    private fun saveHistory() {

        val preferences =
            getSharedPreferences(
                "calculator",
                MODE_PRIVATE
            )

        preferences.edit()
            .putString(
                "history",
                historyList.joinToString("\n")
            )
            .apply()
    }

    private fun loadHistory() {

        val preferences =
            getSharedPreferences(
                "calculator",
                MODE_PRIVATE
            )

        val saved =
            preferences.getString(
                "history",
                ""
            )

        if (!saved.isNullOrEmpty()) {

            historyList.clear()

            historyList.addAll(
                saved.lines()
                    .filter { it.isNotBlank() }
            )
        }

        updateHistoryView()
    }

    // =========================================================
    // FORMAT
    // =========================================================

    private fun formatNumber(
        value: Double
    ): String {

        if (value.isNaN()) {
            return "NaN"
        }

        if (value.isInfinite()) {
            return "∞"
        }

        if (value == value.toLong().toDouble()) {

            return value
                .toLong()
                .toString()
        }

        return String.format(
            Locale.US,
            "%.6f",
            value
        )
            .trimEnd('0')
            .trimEnd('.')
    }
}
