package com.smartcalculator.ai

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    private lateinit var input: EditText
    private lateinit var result: TextView
    private lateinit var history: TextView

    private val preferencesName = "calculator_history"
    private val historyKey = "history"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.input)
        val calculateButton = findViewById<Button>(R.id.calculateButton)
        val clearButton = findViewById<Button>(R.id.clearButton)
        result = findViewById(R.id.result)
        history = findViewById(R.id.history)

        loadHistory()

        calculateButton.setOnClickListener {

            val text = input.text.toString().trim()

            if (text.isEmpty()) {
                result.text = "Введите пример или вопрос"
                return@setOnClickListener
            }

            result.text = "🤖 AI считает..."

            sendToAI(text)
        }

        clearButton.setOnClickListener {

            input.text.clear()

            result.text = "Ответ появится здесь"

            clearHistory()
        }
    }

    private fun sendToAI(text: String) {

        val json = JSONObject()
        json.put("text", text)

        val mediaType =
            "application/json; charset=utf-8".toMediaType()

        val body =
            json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(
                "https://umnyy-calculator-ai-server.onrender.com/v1/calculate"
            )
            .post(body)
            .addHeader(
                "Content-Type",
                "application/json"
            )
            .build()

        client.newCall(request).enqueue(
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {
                        result.text =
                            "❌ Ошибка подключения к AI\n\n${e.message}"
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val responseText =
                        response.body?.string()

                    runOnUiThread {

                        if (!response.isSuccessful) {

                            result.text =
                                "❌ Ошибка AI: ${response.code}"

                            return@runOnUiThread
                        }

                        try {

                            val jsonResponse =
                                JSONObject(responseText ?: "{}")

                            val answer =
                                jsonResponse.optString(
                                    "result",
                                    ""
                                )

                            val explanation =
                                jsonResponse.optString(
                                    "explanation",
                                    ""
                                )

                            val finalAnswer =
                                if (explanation.isNotEmpty()) {
                                    "$answer\n\n$explanation"
                                } else {
                                    answer
                                }

                            result.text = finalAnswer

                            addToHistory(
                                text,
                                finalAnswer
                            )

                        } catch (e: Exception) {

                            result.text =
                                "❌ Не удалось обработать ответ AI"
                        }
                    }
                }
            }
        )
    }

    private fun addToHistory(
        question: String,
        answer: String
    ) {

        val preferences =
            getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        val oldHistory =
            preferences.getString(
                historyKey,
                ""
            ) ?: ""

        val newEntry =
            "Вопрос: $question\nОтвет: $answer"

        val newHistory =
            if (oldHistory.isEmpty()) {
                newEntry
            } else {
                "$newEntry\n\n$oldHistory"
            }

        val limitedHistory =
            newHistory
                .split("\n\n")
                .take(10)
                .joinToString("\n\n")

        preferences.edit()
            .putString(
                historyKey,
                limitedHistory
            )
            .apply()

        history.text = limitedHistory
    }

    private fun loadHistory() {

        val preferences =
            getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        val savedHistory =
            preferences.getString(
                historyKey,
                ""
            ) ?: ""

        history.text =
            if (savedHistory.isEmpty()) {
                "История расчётов пока пуста"
            } else {
                savedHistory
            }
    }

    private fun clearHistory() {

        val preferences =
            getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE
            )

        preferences.edit()
            .remove(historyKey)
            .apply()

        history.text =
            "История расчётов пока пуста"
    }
}
