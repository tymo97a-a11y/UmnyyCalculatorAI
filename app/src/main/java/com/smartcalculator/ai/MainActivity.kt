package com.smartcalculator.ai

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

class MainActivity : AppCompatActivity() {

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var calculatorDisplay: TextView
    private lateinit var history: TextView

    private lateinit var calculateButton: Button
    private lateinit var photoButton: Button
    private lateinit var clearPhotoButton: Button
    private lateinit var photoStatus: TextView

    private val client = OkHttpClient()

    private val serverUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"

    private val imageServerUrl =
        "https://umnyy-calculator-ai-server.onrender.com/v1/calculate-image"

    private val historyList = mutableListOf<String>()

    private var selectedPhotoUri: Uri? = null

    /*
     * Выбор фотографии
     */
    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                selectedPhotoUri = uri

                photoStatus.visibility = TextView.VISIBLE

                photoStatus.text =
                    "📷 Фото выбрано.\n" +
                    "Нажми «Рассчитать по фото»."

                result.text =
                    "📷 Фото готово к обработке AI."

                photoButton.text =
                    "🤖 Рассчитать фото с AI"

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

        calculatorDisplay =
            findViewById(R.id.calculatorDisplay)

        history =
            findViewById(R.id.history)

        calculateButton =
            findViewById(R.id.calculateButton)

        photoButton =
            findViewById(R.id.photoButton)

        clearPhotoButton =
            findViewById(R.id.clearPhotoButton)

        photoStatus =
            findViewById(R.id.photoStatus)


        setupCalculator()

        setupAI()

        setupPhoto()

        setupClearPhoto()

        loadHistory()
    }


    /*
     * =========================
     * AI Обычный текст
     * =========================
     */

    private fun setupAI() {

        calculateButton.setOnClickListener {

            val text =
                input.text.toString().trim()

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

        calculateButton.text =
            "Считаю..."

        result.text =
            "🤖 AI анализирует задачу..."


        val json = JSONObject()

        json.put(
            "text",
            text
        )


        val body =
            json.toString().toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )


        val request =
            Request.Builder()
                .url(serverUrl)
                .post(body)
                .addHeader(
                    "Content-Type",
                    "application/json"
                )
                .build()


        client.newCall(request)
            .enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        runOnUiThread {

                            calculateButton.isEnabled =
                                true

                            calculateButton.text =
                                "Рассчитать с AI"

                            result.text =
                                "❌ Не удалось подключиться к AI.\n\n" +
                                "Проверь интернет-соединение и попробуй ещё раз."

                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка подключения к серверу",
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

                                    calculateButton.isEnabled =
                                        true

                                    calculateButton.text =
                                        "Рассчитать с AI"

                                    result.text =
                                        "❌ Сервер вернул ошибку.\n\n" +
                                        "Код: ${it.code}"
                                }

                                return
                            }


                            try {

                                val jsonResponse =
                                    JSONObject(responseText)


                                val aiResult =
                                    jsonResponse.optString(
                                        "result",
                                        "Результат не получен"
                                    )


                                val explanation =
                                    jsonResponse.optString(
                                        "explanation",
                                        ""
                                    )


                                val finalText =
                                    buildString {

                                        append("ИТОГ:\n")

                                        append(aiResult)


                                        if (
                                            explanation.isNotBlank()
                                        ) {

                                            append("\n\n")

                                            append(
                                                "ОБЪЯСНЕНИЕ:\n"
                                            )

                                            append(
                                                explanation
                                            )
                                        }
                                    }


                                runOnUiThread {

                                    calculateButton.isEnabled =
                                        true

                                    calculateButton.text =
                                        "Рассчитать с AI"

                                    result.text =
                                        finalText


                                    addHistory(
                                        "🤖 $text\n→ $aiResult"
                                    )
                                }

                            } catch (
                                e: Exception
                            ) {

                                runOnUiThread {

                                    calculateButton.isEnabled =
                                        true

                                    calculateButton.text =
                                        "Рассчитать с AI"

                                    result.text =
                                        "❌ Не удалось обработать ответ AI."
                                }
                            }
                        }
                    }
                }
            )
    }


    /*
     * =========================
     * Фото
     * =========================
     */

    private fun setupPhoto() {

        photoButton.setOnClickListener {

            val uri =
                selectedPhotoUri


            if (uri == null) {

                photoPicker.launch("image/*")

            } else {

                sendPhotoToAI(uri)
            }
        }
    }


    /*
     * Отправка фотографии на сервер
     */

    private fun sendPhotoToAI(uri: Uri) {

        photoButton.isEnabled = false

        clearPhotoButton.isEnabled = false

        photoButton.text =
            "🤖 AI анализирует..."


        photoStatus.visibility =
            TextView.VISIBLE

        photoStatus.text =
            "⏳ Отправляю фотографию AI..."


        result.text =
            "🤖 AI распознаёт задачу на фотографии..."


        try {

            val inputStream =
                contentResolver.openInputStream(uri)


            if (inputStream == null) {

                showPhotoError(
                    "Не удалось открыть фотографию."
                )

                return
            }


            val bytes =
                inputStream.use {
                    it.readBytes()
                }


            if (bytes.isEmpty()) {

                showPhotoError(
                    "Фотография пустая."
                )

                return
            }


            val mimeType =
                contentResolver
                    .getType(uri)
                    ?: "image/jpeg"


            val requestBody =
                bytes.toRequestBody(
                    mimeType.toMediaType()
                )


            val multipartBody =
                MultipartBody.Builder()
                    .setType(
                        MultipartBody.FORM
                    )
                    .addFormDataPart(
                        "file",
                        "calculator_photo.jpg",
                        requestBody
                    )
                    .build()


            val request =
                Request.Builder()
                    .url(imageServerUrl)
                    .post(multipartBody)
                    .build()


            client.newCall(request)
                .enqueue(
                    object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {

                            runOnUiThread {

                                photoButton.isEnabled =
                                    true

                                clearPhotoButton.isEnabled =
                                    true

                                photoButton.text =
                                    "🤖 Рассчитать фото с AI"


                                photoStatus.text =
                                    "❌ Ошибка подключения к серверу."


                                result.text =
                                    "Не удалось отправить фотографию AI.\n\n" +
                                    "Проверь интернет и попробуй ещё раз."
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

                                        photoButton.isEnabled =
                                            true

                                        clearPhotoButton.isEnabled =
                                            true

                                        photoButton.text =
                                            "🤖 Рассчитать фото с AI"


                                        photoStatus.text =
                                            "❌ Сервер вернул ошибку."


                                        result.text =
                                            "Ошибка обработки фотографии.\n\n" +
                                            "Код: ${it.code}"
                                    }

                                    return
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


                                    val aiResult =
                                        json.optString(
                                            "result",
                                            "Результат не получен"
                                        )


                                    val explanation =
                                        json.optString(
                                            "explanation",
                                            ""
                                        )


                                    val finalText =
                                        buildString {

                                            if (
                                                recognized.isNotBlank()
                                            ) {

                                                append(
                                                    "РАСПОЗНАНО:\n"
                                                )

                                                append(
                                                    recognized
                                                )

                                                append(
                                                    "\n\n"
                                                )
                                            }


                                            append(
                                                "ИТОГ:\n"
                                            )

                                            append(
                                                aiResult
                                            )


                                            if (
                                                explanation.isNotBlank()
                                            ) {

                                                append(
                                                    "\n\n"
                                                )

                                                append(
                                                    "ОБЪЯСНЕНИЕ:\n"
                                                )

                                                append(
                                                    explanation
                                                )
                                            }
                                        }


                                    runOnUiThread {

                                        photoButton.isEnabled =
                                            true

                                        clearPhotoButton.isEnabled =
                                            true

                                        photoButton.text =
                                            "🤖 Рассчитать фото с AI"


                                        photoStatus.text =
                                            "✅ Фото успешно обработано AI"


                                        result.text =
                                            finalText


                                        addHistory(
                                            "📷 $recognized\n→ $aiResult"
                                        )
                                    }

                                } catch (
                                    e: Exception
                                ) {

                                    runOnUiThread {

                                        photoButton.isEnabled =
                                            true

                                        clearPhotoButton.isEnabled =
                                            true

                                        photoButton.text =
                                            "🤖 Рассчитать фото с AI"


                                        photoStatus.text =
                                            "❌ Не удалось обработать ответ AI."


                                        result.text =
                                            "Сервер ответил, но приложение не смогло прочитать результат."
                                    }
                                }
                            }
                        }
                    }
                )

        } catch (
            e: Exception
        ) {

            showPhotoError(
                "Не удалось прочитать фотографию."
            )
        }
    }


    /*
     * Ошибка фотографии
     */

    private fun showPhotoError(
        message: String
    ) {

        photoButton.isEnabled =
            true

        clearPhotoButton.isEnabled =
            true

        photoButton.text =
            "🤖 Рассчитать фото с AI"


        photoStatus.visibility =
            TextView.VISIBLE

        photoStatus.text =
            "❌ $message"


        result.text =
            message
    }


    /*
     * =========================
     * Очистка фотографии
     * =========================
     */

    private fun setupClearPhoto() {

        clearPhotoButton.setOnClickListener {

            selectedPhotoUri = null


            photoStatus.text = ""

            photoStatus.visibility =
                TextView.GONE


            result.text =
                "Ответ AI появится здесь"


            photoButton.text =
                "📷 Рассчитать по фото"


            photoButton.isEnabled =
                true


            clearPhotoButton.isEnabled =
                true


            Toast.makeText(
                this,
                "Фото очищено",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /*
     * =========================
     * Калькулятор
     * =========================
     */

    private var calculatorValue = ""

    private var firstNumber = 0.0

    private var currentOperator = ""

    private var shouldResetDisplay = false


    private fun setupCalculator() {

        val numberButtons =
            listOf(
                R.id.button0,
                R.id.button1,
                R.id.button2,
                R.id.button3,
                R.id.button4,
                R.id.button5,
                R.id.button6,
                R.id.button7,
                R.id.button8,
                R.id.button9
            )


        numberButtons.forEach { id ->

            findViewById<Button>(id)
                .setOnClickListener {

                    val button =
                        it as Button

                    appendNumber(
                        button.text.toString()
                    )
                }
        }


        findViewById<Button>(
            R.id.buttonDot
        ).setOnClickListener {

            appendNumber(".")
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

            calculateResult()
        }


        findViewById<Button>(
            R.id.buttonClear
        ).setOnClickListener {

            clearCalculator()
        }
    }


    private fun appendNumber(
        value: String
    ) {

        if (shouldResetDisplay) {

            calculatorValue = ""

            shouldResetDisplay = false
        }


        if (
            value == "." &&
            calculatorValue.contains(".")
        ) {

            return
        }


        calculatorValue += value


        if (
            calculatorValue.startsWith(".")
        ) {

            calculatorValue =
                "0$calculatorValue"
        }


        calculatorDisplay.text =
            calculatorValue
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


        shouldResetDisplay =
            true
    }


    private fun calculateResult() {

        if (
            currentOperator.isEmpty() ||
            calculatorValue.isEmpty()
        ) {

            return
        }


        val secondNumber =
            calculatorValue.toDoubleOrNull()
                ?: return


        val resultValue: Double?


        resultValue =
            when (currentOperator) {

                "+" ->
                    firstNumber + secondNumber

                "-" ->
                    firstNumber - secondNumber

                "*" ->
                    firstNumber * secondNumber

                "/" -> {

                    if (
                        secondNumber == 0.0
                    ) {

                        Toast.makeText(
                            this,
                            "На ноль делить нельзя",
                            Toast.LENGTH_SHORT
                        ).show()

                        return
                    }

                    firstNumber / secondNumber
                }

                else ->
                    null
            }


        if (resultValue == null) {

            return
        }


        val formatted =
            formatCalculatorNumber(
                resultValue
            )


        calculatorDisplay.text =
            formatted


        addHistory(
            "$firstNumber $currentOperator $secondNumber = $formatted"
        )


        calculatorValue =
            formatted


        currentOperator =
            ""


        shouldResetDisplay =
            true
    }


    private fun clearCalculator() {

        calculatorValue = ""

        firstNumber = 0.0

        currentOperator = ""

        shouldResetDisplay = false

        calculatorDisplay.text =
            "0"
    }


    private fun formatCalculatorNumber(
        value: Double
    ): String {

        if (value.isNaN() ||
            value.isInfinite()
        ) {

            return "Ошибка"
        }


        return if (
            value % 1.0 == 0.0
        ) {

            value.toLong().toString()

        } else {

            String.format(
                Locale.US,
                "%.10f",
                value
            )
                .trimEnd('0')
                .trimEnd('.')
        }
    }


    /*
     * =========================
     * История
     * =========================
     */

    private fun addHistory(
        text: String
    ) {

        historyList.add(
            0,
            text
        )


        if (
            historyList.size > 20
        ) {

            historyList.removeAt(
                historyList.lastIndex
            )
        }


        saveHistory()

        updateHistoryView()
    }


    private fun updateHistoryView() {

        if (
            historyList.isEmpty()
        ) {

            history.text =
                "История расчётов появится здесь"

            return
        }


        history.text =
            historyList.joinToString(
                separator = "\n\n"
            )
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
                historyList.joinToString(
                    "\n|||HISTORY||| \n"
                )
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


        if (
            !saved.isNullOrBlank()
        ) {

            historyList.clear()

            historyList.addAll(
                saved.split(
                    "\n|||HISTORY||| \n"
                )
            )
        }


        updateHistoryView()
    }
}
