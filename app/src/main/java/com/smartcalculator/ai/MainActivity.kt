package com.smartcalculator.ai

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.input)
        val calculateButton = findViewById<Button>(R.id.calculateButton)
        val result = findViewById<TextView>(R.id.result)

        calculateButton.setOnClickListener {

            val text = input.text.toString().trim()

            if (text.isEmpty()) {
                result.text = "Введите вопрос или пример"
                return@setOnClickListener
            }

            result.text = "AI считает..."

            sendToAI(text, result)
        }
    }

    private fun sendToAI(
        text: String,
        result: TextView
    ) {

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
                            "Ошибка подключения к AI:\n${e.message}"
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
                                "Ошибка AI: ${response.code}\n$responseText"

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

                            result.text =
                                if (explanation.isNotEmpty()) {
                                    "$answer\n\n$explanation"
                                } else {
                                    answer
                                }

                        } catch (e: Exception) {

                            result.text =
                                "Не удалось обработать ответ AI."
                        }
                    }
                }
            }
        )
    }
}
