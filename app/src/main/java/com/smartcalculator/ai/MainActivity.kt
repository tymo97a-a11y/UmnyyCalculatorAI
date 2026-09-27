package com.smartcalculator.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    // =========================================================
    // ИСТОРИЯ
    // =========================================================

    private val historyList =
        mutableListOf<String>()

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


    private fun tapFeedback() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18L)
        }
    }

    private fun updateDisplaySize(display: EditText) {
        val length = display.text.length
        display.textSize = when {
            length <= 12 -> 32f
            length <= 20 -> 28f
            length <= 30 -> 24f
            length <= 42 -> 20f
            else -> 17f
        }
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
            R.id.menuConverter
        ).setOnClickListener {

            openConverterScreen()
        }

        findViewById<View>(
            R.id.menuEquations
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EquationActivity::class.java
                )
            )
        }

        findViewById<View>(
            R.id.menuSystems
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SystemActivity::class.java
                )
            )
        }

        findViewById<View>(
            R.id.menuTable
        ).setOnClickListener {

            openTableScreen()
        }

        findViewById<View>(
            R.id.menuGeometry
        ).setOnClickListener {
            startActivity(Intent(this, GeometryActivity::class.java))
        }

        findViewById<View>(
            R.id.menuFinance
        ).setOnClickListener {
            startActivity(Intent(this, FinanceActivity::class.java))
        }

        findViewById<View>(
            R.id.menuMatrix
        ).setOnClickListener {
            startActivity(Intent(this, MatrixActivity::class.java))
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
        val root = createRoot()
        addBackButton(root)
        root.addView(makeTitle("🧮 Калькулятор"))

        val display = EditText(this).apply {
            setText("0")
            setTextColor(Color.WHITE)
            setHintTextColor(secondaryColor)
            textSize = 32f
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setSingleLine(true)
            setSelectAllOnFocus(false)
            isLongClickable = true
            setTextIsSelectable(true)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundColor(cardColor)
        }
        root.addView(display, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96)))

        display.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateDisplaySize(display)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val pasteButton = Button(this).apply {
            text = "📋 Вставить из буфера"
            setTextColor(Color.WHITE)
            setBackgroundColor(blueColor)
            textSize = 15f
            setOnClickListener {
                tapFeedback()
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val pasted = clip.getItemAt(0).coerceToText(this@MainActivity).toString()
                    if (pasted.isNotBlank()) {
                        display.setText(pasted.trim())
                        display.setSelection(display.text.length)
                    } else {
                        Toast.makeText(this@MainActivity, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(pasteButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(8) })

        fun configureButton(button: Button, text: String) {
            button.text = text
            button.setTextColor(Color.WHITE)
            button.textSize = 19f
            button.minHeight = 0
            button.minimumHeight = 0
            button.setPadding(dp(2), dp(2), dp(2), dp(2))
            val operator = text in setOf("+", "−", "×", "÷", "=", "%", "AC", "⌫", "sin", "cos", "tan", "sqrt", "ln", "log", "^", "!", "π", "e", "(", ")")
            button.setBackgroundResource(if (operator) R.drawable.button_operator else R.drawable.button_number)
        }

        fun insertText(text: String) {
            val current = display.text.toString()
            val value = if (current == "0" || current.startsWith("Ошибка:")) "" else current
            display.setText(value + text)
            display.setSelection(display.text.length)
        }

        fun handleButton(text: String) {
            tapFeedback()
            when (text) {
                "AC" -> display.setText("0")
                "⌫" -> {
                    val value = display.text.toString()
                    display.setText(if (value.length <= 1) "0" else value.dropLast(1))
                    display.setSelection(display.text.length)
                }
                "=" -> {
                    val expression = display.text.toString()
                    val result = calculateExpression(expression)
                    display.setText(result)
                    display.setSelection(display.text.length)
                    if (!result.startsWith("Ошибка:")) addHistory(expression, result)
                }
                "sin", "cos", "tan", "sqrt", "ln", "log" -> insertText("$text(")
                "π", "e" -> insertText(text)
                "^", "!", "%", "(", ")", ".", "+", "−", "×", "÷" -> {
                    if (display.text.toString().startsWith("Ошибка:")) display.setText("0")
                    if (text == "." && display.text.toString().endsWith(".")) return
                    display.append(text)
                    display.setSelection(display.text.length)
                }
                else -> {
                    val current = display.text.toString()
                    if (current.startsWith("Ошибка:") || current == "0") display.setText(text) else display.append(text)
                    display.setSelection(display.text.length)
                }
            }
        }

        val mainGrid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 5
            useDefaultMargins = false
        }
        fun addMain(text: String, row: Int, column: Int, span: Int = 1) {
            val button = Button(this)
            configureButton(button, text)
            val params = GridLayout.LayoutParams(GridLayout.spec(row, 1), GridLayout.spec(column, span, 1f))
            params.width = 0
            params.height = dp(62)
            params.setMargins(dp(4), dp(4), dp(4), dp(4))
            mainGrid.addView(button, params)
            button.setOnClickListener { handleButton(text) }
        }
        addMain("AC", 0, 0); addMain("⌫", 0, 1); addMain("%", 0, 2); addMain("÷", 0, 3)
        addMain("7", 1, 0); addMain("8", 1, 1); addMain("9", 1, 2); addMain("×", 1, 3)
        addMain("4", 2, 0); addMain("5", 2, 1); addMain("6", 2, 2); addMain("−", 2, 3)
        addMain("1", 3, 0); addMain("2", 3, 1); addMain("3", 3, 2); addMain("+", 3, 3)
        addMain("0", 4, 0, 2); addMain(".", 4, 2); addMain("=", 4, 3)
        root.addView(mainGrid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val advancedButton = Button(this).apply {
            text = "⌨ Доп. клавиши"
            setTextColor(Color.WHITE)
            setBackgroundColor(blueColor)
            textSize = 16f
        }
        root.addView(advancedButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply {
            topMargin = dp(8)
            bottomMargin = dp(4)
        })

        val advancedGrid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 3
            useDefaultMargins = false
            visibility = View.GONE
        }
        fun addAdvanced(text: String, row: Int, column: Int) {
            val button = Button(this)
            configureButton(button, text)
            val params = GridLayout.LayoutParams(GridLayout.spec(row, 1), GridLayout.spec(column, 1, 1f))
            params.width = 0
            params.height = dp(58)
            params.setMargins(dp(4), dp(4), dp(4), dp(4))
            advancedGrid.addView(button, params)
            button.setOnClickListener { handleButton(text) }
        }
        addAdvanced("sin", 0, 0); addAdvanced("cos", 0, 1); addAdvanced("tan", 0, 2); addAdvanced("sqrt", 0, 3)
        addAdvanced("ln", 1, 0); addAdvanced("log", 1, 1); addAdvanced("^", 1, 2); addAdvanced("!", 1, 3)
        addAdvanced("(", 2, 0); addAdvanced(")", 2, 1); addAdvanced("π", 2, 2); addAdvanced("e", 2, 3)
        root.addView(advancedGrid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        advancedButton.setOnClickListener {
            tapFeedback()
            val open = advancedGrid.visibility != View.VISIBLE
            advancedGrid.visibility = if (open) View.VISIBLE else View.GONE
            advancedButton.text = if (open) "⌨ Скрыть доп. клавиши" else "⌨ Доп. клавиши"
        }

        setScreen(root)
    }

    private fun calculateExpression(expression: String): String {
        return try {
            var clean = expression
                .trim()
                .replace(",", ".")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "pi")
                .replace("√", "sqrt")
                .replace("²", "^2")
                .replace(" ", "")

            if (clean.isBlank()) {
                return "0"
            }

            val value = EquationSolver.evaluateExpression(clean)
            formatNumber(value)
        } catch (exception: Exception) {
            "Ошибка: ${exception.message ?: "проверьте выражение"}"
        }
    }

    private fun openConverterScreen() {
        val root = createRoot()
        addBackButton(root)
        root.addView(makeTitle("🔄 Конвертер"))

        val categories = listOf("Длина", "Масса", "Площадь", "Объём", "Скорость", "Температура")
        val units = mapOf(
            "Длина" to listOf("метр", "километр", "сантиметр", "миллиметр", "фут", "дюйм"),
            "Масса" to listOf("килограмм", "грамм", "тонна", "фунт", "унция"),
            "Площадь" to listOf("м²", "км²", "см²", "гектар", "акр"),
            "Объём" to listOf("литр", "мл", "м³", "галлон"),
            "Скорость" to listOf("км/ч", "м/с", "миль/ч", "узел"),
            "Температура" to listOf("°C", "°F", "K")
        )

        val category = Spinner(this)
        category.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        root.addView(category, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)))

        val from = Spinner(this)
        val to = Spinner(this)
        fun setUnitAdapters(name: String) {
            val list = units[name] ?: emptyList()
            from.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
            to.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
            if (list.size > 1) to.setSelection(1)
        }
        setUnitAdapters(categories.first())
        category.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                setUnitAdapters(categories[position])
            }
        }
        root.addView(from, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(8) })

        val input = EditText(this).apply {
            hint = "Введите значение"
            setTextColor(Color.WHITE)
            setHintTextColor(secondaryColor)
            textSize = 22f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setBackgroundColor(cardColor)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70)).apply { topMargin = dp(8) })
        root.addView(to, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(8) })

        val result = TextView(this).apply {
            text = "Результат появится здесь"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(20), dp(12), dp(20))
            setBackgroundColor(cardColor)
        }
        root.addView(result, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100)).apply { topMargin = dp(12) })

        fun convert(categoryName: String, value: Double, fromUnit: String, toUnit: String): Double {
            if (categoryName == "Температура") {
                val c = when (fromUnit) { "°F" -> (value - 32) * 5 / 9; "K" -> value - 273.15; else -> value }
                return when (toUnit) { "°F" -> c * 9 / 5 + 32; "K" -> c + 273.15; else -> c }
            }
            val factors = when (categoryName) {
                "Длина" -> mapOf("метр" to 1.0, "километр" to 1000.0, "сантиметр" to 0.01, "миллиметр" to 0.001, "фут" to 0.3048, "дюйм" to 0.0254)
                "Масса" -> mapOf("килограмм" to 1.0, "грамм" to 0.001, "тонна" to 1000.0, "фунт" to 0.45359237, "унция" to 0.028349523125)
                "Площадь" -> mapOf("м²" to 1.0, "км²" to 1_000_000.0, "см²" to 0.0001, "гектар" to 10_000.0, "акр" to 4046.8564224)
                "Объём" -> mapOf("литр" to 1.0, "мл" to 0.001, "м³" to 1000.0, "галлон" to 3.785411784)
                "Скорость" -> mapOf("км/ч" to 1.0, "м/с" to 3.6, "миль/ч" to 1.609344, "узел" to 1.852)
                else -> emptyMap()
            }
            val base = value * (factors[fromUnit] ?: 1.0)
            return base / (factors[toUnit] ?: 1.0)
        }

        val convertButton = Button(this).apply {
            text = "🔄 Конвертировать"
            setTextColor(Color.WHITE)
            setBackgroundColor(blueColor)
            textSize = 17f
            setOnClickListener {
                tapFeedback()
                val value = input.text.toString().replace(',', '.').toDoubleOrNull()
                if (value == null) {
                    result.text = "Введите число"
                    return@setOnClickListener
                }
                val converted = convert(category.selectedItem.toString(), value, from.selectedItem.toString(), to.selectedItem.toString())
                result.text = formatNumber(converted)
            }
        }
        root.addView(convertButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(10) })
        setScreen(root)
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

        addBackButton(root)

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

        addBackButton(root)

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
