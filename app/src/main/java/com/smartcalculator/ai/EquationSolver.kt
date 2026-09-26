package com.smartcalculator.ai

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Local parser and solver used by the equations screen. It never calls the network. */
object EquationSolver {

    data class Solution(val result: String, val explanation: String)

    fun compileFunction(source: String): (Double) -> Double {
        val expression = normalizeFunction(source)
        if (expression.isBlank()) error("Введите функцию.")
        return Parser(expression).parse()
    }

    /** Evaluates a numeric expression locally, without x and without network access. */
    fun evaluateExpression(source: String): Double {
        val expression = normalizeFunction(source)
        if (expression.isBlank()) error("Введите выражение.")
        val value = Parser(expression).parse()(0.0)
        if (!value.isFinite()) error("Результат не является конечным числом.")
        return value
    }

    fun solve(source: String): Solution {
        val parts = source.replace("−", "-").split('=')
        if (parts.size != 2) error("Введите уравнение со знаком =, например x² - 5x + 6 = 0.")

        val left = compileFunction(parts[0])
        val right = compileFunction(parts[1])
        val value: (Double) -> Double = { x -> left(x) - right(x) }
        val c = value(0.0)
        val atOne = value(1.0)
        val atMinusOne = value(-1.0)
        val a = (atOne + atMinusOne - 2.0 * c) / 2.0
        val b = (atOne - atMinusOne) / 2.0

        listOf(2.0, -2.0, 3.0).forEach { x ->
            if (!close(value(x), a * x * x + b * x + c)) {
                error("Для решения доступны линейные и квадратные уравнения с x.")
            }
        }

        return when {
            near(a) && near(b) && near(c) -> Solution(
                "Любое x",
                "После упрощения получаем 0 = 0, поэтому подходят все значения x."
            )
            near(a) && near(b) -> Solution(
                "Нет решений",
                "После упрощения получаем ${number(c)} = 0 — это неверное равенство."
            )
            near(a) -> {
                val x = -c / b
                Solution(
                    "x = ${number(x)}",
                    "1. Приводим к виду ${number(b)}x + ${number(c)} = 0.\n" +
                        "2. Переносим свободный член: ${number(b)}x = ${number(-c)}.\n" +
                        "3. Делим на ${number(b)}: x = ${number(x)}."
                )
            }
            else -> quadratic(a, b, c)
        }
    }

    private fun quadratic(a: Double, b: Double, c: Double): Solution {
        val discriminant = b * b - 4.0 * a * c
        val header = "1. Приводим к виду ${number(a)}x² + ${number(b)}x + ${number(c)} = 0.\n" +
            "2. D = b² − 4ac = ${number(b)}² − 4 · ${number(a)} · ${number(c)} = ${number(discriminant)}.\n"
        return when {
            discriminant < -EPSILON -> Solution("Нет действительных корней", header + "3. D < 0, действительных корней нет.")
            near(discriminant) -> {
                val x = -b / (2.0 * a)
                Solution("x = ${number(x)}", header + "3. D = 0, поэтому x = −b / 2a = ${number(x)}.")
            }
            else -> {
                val root = sqrt(discriminant)
                val x1 = (-b + root) / (2.0 * a)
                val x2 = (-b - root) / (2.0 * a)
                Solution(
                    "x₁ = ${number(x1)}, x₂ = ${number(x2)}",
                    header + "3. x₁ = (−b + √D) / 2a = ${number(x1)}.\n" +
                        "4. x₂ = (−b − √D) / 2a = ${number(x2)}."
                )
            }
        }
    }

    private fun normalizeFunction(source: String): String = source.trim().lowercase()
        .replace("²", "^2").replace("−", "-").replace("×", "*").replace("÷", "/")
        .replace("π", "pi").replace("√", "sqrt").replace(" ", "")
        .removePrefix("y=").removePrefix("f(x)=")

    private fun near(value: Double) = abs(value) < EPSILON
    private fun close(first: Double, second: Double) = abs(first - second) < EPSILON * maxOf(1.0, abs(first))
    private fun number(value: Double): String = if (near(value - value.toLong())) value.toLong().toString() else "%.6f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    private const val EPSILON = 1e-8

    private class Parser(private val input: String) {
        private var position = 0

        fun parse(): (Double) -> Double {
            val result = expression()
            if (position != input.length) error("Неизвестный символ: ${input[position]}")
            return result
        }

        private fun expression(): (Double) -> Double {
            var result = term()
            while (true) result = when {
                take('+') -> result.combine(term()) { a, b -> a + b }
                take('-') -> result.combine(term()) { a, b -> a - b }
                else -> return result
            }
        }

        private fun term(): (Double) -> Double {
            var result = power()
            while (true) result = when {
                take('*') -> result.combine(power()) { a, b -> a * b }
                take('/') -> result.combine(power()) { a, b ->
                    if (abs(b) < EPSILON) error("Деление на ноль.")
                    a / b
                }
                implicitMultiplicationStarts() -> result.combine(power()) { a, b -> a * b }
                else -> return result
            }
        }

        private fun power(): (Double) -> Double {
            var result = unary()
            if (take('^')) result = result.combine(power()) { a, b -> Math.pow(a, b) }
            return result
        }

        private fun unary(): (Double) -> Double = when {
            take('+') -> unary()
            take('-') -> unary().let { value -> { x -> -value(x) } }
            else -> postfix()
        }

        private fun postfix(): (Double) -> Double {
            var result = atom()

            while (true) {

                if (take('%')) {

                    val previous = result
                    result = { x -> previous(x) / 100.0 }

                } else if (take('!')) {

                    val previous = result
                    result = { x -> factorial(previous(x)) }

                } else {

                    return result
                }
            }
        }

        private fun factorial(value: Double): Double {
            if (!value.isFinite()) error("Нельзя вычислить факториал.")
            val rounded = kotlin.math.round(value)
            if (abs(value - rounded) > EPSILON || rounded < 0.0 || rounded > 170.0) {
                error("Факториал доступен для целых чисел от 0 до 170.")
            }

            var result = 1.0
            var i = 2L
            while (i <= rounded.toLong()) {
                result *= i.toDouble()
                i++
            }
            return result
        }

        private fun atom(): (Double) -> Double {
            if (take('(')) return expression().also { expect(')') }
            if (position < input.length && (input[position].isDigit() || input[position] == '.')) {
                val start = position
                while (position < input.length && (input[position].isDigit() || input[position] == '.')) position++
                val number = input.substring(start, position).toDoubleOrNull() ?: error("Некорректное число.")
                return { _: Double -> number }
            }
            val name = readName()
            return when (name) {
                "x" -> { x -> x }
                "pi" -> { _: Double -> PI }
                "e" -> { _: Double -> kotlin.math.E }
                "sin", "cos", "tan", "sqrt", "abs", "ln", "log", "log2",
                "exp", "asin", "acos", "atan", "sinh", "cosh", "tanh",
                "floor", "ceil", "round" -> {
                    expect('(')
                    val argument = expression()
                    expect(')')
                    when (name) {
                        "sin" -> { x -> sin(argument(x)) }
                        "cos" -> { x -> cos(argument(x)) }
                        "tan" -> { x -> tan(argument(x)) }
                        "sqrt" -> { x -> sqrt(argument(x)) }
                        "abs" -> { x -> abs(argument(x)) }
                        "ln" -> { x -> ln(argument(x)) }
                        "log" -> { x -> kotlin.math.log10(argument(x)) }
                        "log2" -> { x -> kotlin.math.log2(argument(x)) }
                        "exp" -> { x -> kotlin.math.exp(argument(x)) }
                        "asin" -> { x -> kotlin.math.asin(argument(x)) }
                        "acos" -> { x -> kotlin.math.acos(argument(x)) }
                        "atan" -> { x -> kotlin.math.atan(argument(x)) }
                        "sinh" -> { x -> kotlin.math.sinh(argument(x)) }
                        "cosh" -> { x -> kotlin.math.cosh(argument(x)) }
                        "tanh" -> { x -> kotlin.math.tanh(argument(x)) }
                        "floor" -> { x -> kotlin.math.floor(argument(x)) }
                        "ceil" -> { x -> kotlin.math.ceil(argument(x)) }
                        else -> { x -> kotlin.math.round(argument(x)) }
                    }
                }
                else -> error("Ожидались число, x или функция.")
            }
        }

        private fun readName(): String {
            val start = position
            while (position < input.length && input[position].isLetter()) position++
            return input.substring(start, position)
        }
        private fun implicitMultiplicationStarts(): Boolean {
            if (position >= input.length) return false
            val next = input[position]
            return next == '(' || next == '.' || next.isDigit() || next.isLetter()
        }
        private fun take(char: Char): Boolean = (position < input.length && input[position] == char).also { if (it) position++ }
        private fun expect(char: Char) { if (!take(char)) error("Ожидался символ '$char'.") }
        private fun ((Double) -> Double).combine(other: (Double) -> Double, operation: (Double, Double) -> Double): (Double) -> Double = { x -> operation(this(x), other(x)) }
    }
}
