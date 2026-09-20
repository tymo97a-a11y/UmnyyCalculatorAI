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


    // =========================================================
    // ВЫБОР ФОТО
    // =========================================================

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                selectedPhotoUri = uri

                // Показываем миниатюру
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


    // =========================================================
    // ON CREATE
    // =========================================================

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

        photoPreview =
            findViewById(R.id.photoPreview)

        photoStatus =
            findViewById(R.id.photoStatus)


        setupCalculator()

        setupAI()

        setupPhoto()

        loadHistory()
    }


    // =========================================================
    // ОБЫЧНЫЙ AI
    // =========================================================

    private fun setupAI() {

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


            sendToAI(text)
        }
    }


    private fun sendToAI(text: String) {

        calculateButton.isEnabled = false

        calculateButton.text = "Считаю..."

        result.text =
            "🤖 AI анализирует задачу..."


        val json = JSONObject()

        json.put(
            "text",
            text
        )


        val body =
            json.toString()
                .toRequestBody(
                    "application/json; charset=utf-8"
                        .toMediaType()
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
                                it.body
                                    ?.string()
                                    .orEmpty()


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
                                    JSONObject(
                                        responseText
                                    )


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


    // =========================================================
    // ФОТО
    // =========================================================

    private fun setupPhoto() {

        photoButton.setOnClickListener {

            val uri =
                selectedPhotoUri


            if (uri == null) {

                // Фото ещё нет — открываем галерею

                photoPicker.launch(
                    "image/*"
                )

            } else {

                // Фото уже выбрано —
                // отправляем его AI

                sendPhotoToAI(uri)
            }
        }


        clearPhotoButton.setOnClickListener {

            clearPhoto()
        }
    }


    // =========================================================
    // ОТПРАВКА ФОТО AI
    // =========================================================

    private fun sendPhotoToAI(
        uri: Uri
    ) {

        photoButton.isEnabled = false

        clearPhotoButton.isEnabled = false

        photoButton.text =
            "🤖  AI обрабатывает фото..."

        photoStatus.visibility =
            View.VISIBLE

        photoStatus.text =
            "⏳ Распознаваем пример на фотографии..."

        result.text =
            "🤖 AI анализирует фотографию..."


        Thread {

            try {

                val inputStream =
                    contentResolver.openInputStream(
                        uri
                    )
                        ?: throw IOException(
                            "Не удалось открыть фотографию"
                        )


                val bytes =
                    inputStream.use {
                        it.readBytes()
                    }


                if (bytes.isEmpty()) {

                    throw IOException(
                        "Фотография пустая"
                    )
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
                            getFileName(uri),
                            requestBody
                        )
                        .build()


                val request =
                    Request.Builder()
                        .url(imageServerUrl)
                        .post(
                            multipartBody
                        )
                        .build()


                client.newCall(request)
                    .execute()
                    .use { response ->

                        val responseText =
                            response.body
                                ?.string()
                                .orEmpty()


                        if (!response.isSuccessful) {

                            runOnUiThread {

                                photoButton.isEnabled =
                                    true

                                clearPhotoButton.isEnabled =
                                    true

                                photoButton.text =
                                    "🤖  Рассчитать фото с AI"

                                photoStatus.text =
                                    "❌ Ошибка обработки фотографии"

                                result.text =
                                    "❌ Сервер вернул ошибку.\n\n" +
                                    "Код: ${response.code}\n\n" +
                                    responseText.take(500)
                            }

                            return@use
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
                                    "🤖  Рассчитать фото с AI"

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
                                    "🤖  Рассчитать фото с AI"

                                photoStatus.text =
                                    "❌ Ошибка ответа AI"

                                result.text =
                                    "❌ Не удалось обработать ответ сервера."
                            }
                        }
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
                        "🤖  Рассчитать фото с AI"

                    photoStatus.text =
                        "❌ Ошибка обработки фотографии"

                    result.text =
                        "❌ Не удалось отправить фотографию.\n\n" +
                        e.message
                }
            }

        }.start()
    }


    // =========================================================
    // ИМЯ ФАЙЛА
    // =========================================================

    private fun getFileName(
        uri: Uri
    ): String {

        var name = "calculator_photo.jpg"


        val cursor =
            contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )


        cursor?.use {

            val nameIndex =
                it.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )


            if (
                nameIndex >= 0
                && it.moveToFirst()
            ) {

                name =
                    it.getString(
                        nameIndex
                    )
            }
        }


        return name
    }


    // =========================================================
    // ОЧИСТКА ФОТО
    // =========================================================

    private fun clearPhoto() {

        selectedPhotoUri = null

        photoPreview.setImageDrawable(
            null
        )

        photoPreview.visibility =
            View.GONE

        photoStatus.text = ""

        photoStatus.visibility =
            View.GONE

        photoButton.text =
            "📷  Рассчитать по фото"

        result.text =
            "Ответ AI появится здесь"

        Toast.makeText(
            this,
            "Фото очищено",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =========================================================
    // ОБЫЧНЫЙ КАЛЬКУЛЯТОР
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

                R.id.button9 to "9",

                R.id.buttonDot to "."
            )


        for (
            entry in numberButtons
        ) {

            findViewById<Button>(
                entry.key
            ).setOnClickListener {

                appendNumber(
                    entry.value
                )
            }
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


        updateCalculatorDisplay()
    }


    private fun appendNumber(
        value: String
    ) {

        if (
            waitingForSecondNumber
        ) {

            calculatorValue = ""

            waitingForSecondNumber = false
        }


        if (
            value == "."
            && calculatorValue.contains(".")
        ) {

            return
        }


        if (
            calculatorValue == "0"
            && value != "."
        ) {

            calculatorValue = ""
        }


        calculatorValue += value

        updateCalculatorDisplay()
    }


    private fun chooseOperator(
        operator: String
    ) {

        if (
            calculatorValue.isEmpty()
        ) {

            return
        }


        firstNumber =
            calculatorValue.toDoubleOrNull()
                ?: return


        currentOperator =
            operator


        waitingForSecondNumber =
            true
    }


    private fun calculateResult() {

        if (
            currentOperator.isEmpty()
            || calculatorValue.isEmpty()
        ) {

            return
        }


        val secondNumber =
            calculatorValue.toDoubleOrNull()
                ?: return


        val calculated: Double?


        calculated =
            when (currentOperator) {

                "+" ->
                    firstNumber + secondNumber

                "-" ->
                    firstNumber - secondNumber

                "*" ->
                    firstNumber * secondNumber

                "/" ->
                    if (
                        secondNumber == 0.0
                    ) {

                        Toast.makeText(
                            this,
                            "На ноль делить нельзя",
                            Toast.LENGTH_SHORT
                        ).show()

                        return

                    } else {

                        firstNumber /
                            secondNumber
                    }

                else ->
                    null
            }


        if (
            calculated == null
        ) {

            return
        }


        val formatted =
            formatCalculatorNumber(
                calculated
            )


        calculatorValue =
            formatted


        calculatorDisplay.text =
            formatted


        addHistory(
            "$firstNumber " +
                "$currentOperator " +
                "$secondNumber = " +
                formatted
        )


        currentOperator = ""

        waitingForSecondNumber = true
    }


    private fun clearCalculator() {

        calculatorValue = ""

        firstNumber = 0.0

        currentOperator = ""

        waitingForSecondNumber = false

        calculatorDisplay.text =
            "0"
    }


    private fun updateCalculatorDisplay() {

        calculatorDisplay.text =
            if (
                calculatorValue.isEmpty()
            ) {

                "0"

            } else {

                calculatorValue
            }
    }


    private fun formatCalculatorNumber(
        value: Double
    ): String {

        if (
            abs(
                value -
                    value.toLong()
                        .toDouble()
            ) < 0.000000001
        ) {

            return value
                .toLong()
                .toString()
        }


        return String.format(
            Locale.US,
            "%.10f",
            value
        )
            .trimEnd('0')
            .trimEnd('.')
    }


    // =========================================================
    // ИСТОРИЯ
    // =========================================================

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

        updateHistory()
    }


    private fun updateHistory() {

        if (
            historyList.isEmpty()
        ) {

            history.text =
                "История расчётов появится здесь"

            return
        }


        history.text =
            historyList.joinToString(
                "\n\n"
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
                    "\n|||HISTORY|||\n"
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
                .orEmpty()


        if (
            saved.isNotBlank()
        ) {

            historyList.clear()

            historyList.addAll(
                saved.split(
                    "\n|||HISTORY|||\n"
                )
            )
        }


        updateHistory()
    }
}
