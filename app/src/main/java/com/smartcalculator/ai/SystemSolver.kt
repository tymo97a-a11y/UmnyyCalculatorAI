package com.smartcalculator.ai

import kotlin.math.abs

/** Solves two linear equations locally with Cramer's rule. */
object SystemSolver {
    private const val EPSILON = 1e-9

    data class Solution(
        val result: String,
        val explanation: String
    )

    fun solve(firstEquation: String, secondEquation: String): Solution {
        val first = parseEquation(firstEquation)
        val second = parseEquation(secondEquation)

        val determinant = first.a * second.b - second.a * first.b
        val determinantX = first.c * second.b - second.c * first.b
        val determinantY = first.a * second.c - second.a * first.c
        val steps = "D = a₁b₂ − a₂b₁ = ${number(determinant)}\n" +
            "Dx = c₁b₂ − c₂b₁ = ${number(determinantX)}\n" +
            "Dy = a₁c₂ − a₂c₁ = ${number(determinantY)}"

        if (near(determinant)) {
            return if (near(determinantX) && near(determinantY)) {
                Solution(
                    "Бесконечно много решений",
                    "$steps\n\nD = Dx = Dy = 0, поэтому уравнения задают одну прямую."
                )
            } else {
                Solution(
                    "Нет решений",
                    "$steps\n\nD = 0, но хотя бы один из Dx или Dy не равен 0."
                )
            }
        }

        val x = determinantX / determinant
        val y = determinantY / determinant
        return Solution(
            "✅ Ответ\nx = ${number(x)}\ny = ${number(y)}",
            "📚 Пошагово:\n$steps\n" +
                "x = Dx / D = ${number(determinantX)} / ${number(determinant)} = ${number(x)}\n" +
                "y = Dy / D = ${number(determinantY)} / ${number(determinant)} = ${number(y)}"
        )
    }

    private fun parseEquation(source: String): LinearEquation {
        val parts = source.replace("−", "-").replace("×", "*").replace(" ", "").split('=')
        if (parts.size != 2 || parts.any { it.isBlank() }) {
            error("Введите уравнение вида 2x + y = 7.")
        }
        val left = parseSide(parts[0])
        val right = parseSide(parts[1])
        return LinearEquation(left.x - right.x, left.y - right.y, right.constant - left.constant)
    }

    private fun parseSide(source: String): Coefficients {
        val prepared = if (source.startsWith('+') || source.startsWith('-')) source else "+$source"
        val terms = Regex("([+-])([^+-]+)").findAll(prepared).toList()
        if (terms.isEmpty() || terms.joinToString("") { it.value } != prepared) {
            error("Используйте только линейные слагаемые с x и y.")
        }

        var x = 0.0
        var y = 0.0
        var constant = 0.0
        terms.forEach { match ->
            val sign = if (match.groupValues[1] == "-") -1.0 else 1.0
            val term = match.groupValues[2]
            val xCount = term.count { it == 'x' }
            val yCount = term.count { it == 'y' }
            if (xCount + yCount > 1) error("Каждое слагаемое может содержать только x или y.")

            when {
                xCount == 1 -> x += sign * coefficient(term.removeSuffix("x"))
                yCount == 1 -> y += sign * coefficient(term.removeSuffix("y"))
                else -> constant += sign * numberValue(term)
            }
        }
        return Coefficients(x, y, constant)
    }

    private fun coefficient(source: String): Double {
        val value = source.removeSuffix("*").removePrefix("*")
        return if (value.isEmpty()) 1.0 else numberValue(value)
    }

    private fun numberValue(source: String): Double = source.toDoubleOrNull()
        ?: error("Не удалось распознать коэффициент '$source'.")

    private fun near(value: Double) = abs(value) < EPSILON
    private fun number(value: Double): String =
        if (near(value - value.toLong())) value.toLong().toString()
        else "%.6f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')

    private data class Coefficients(val x: Double, val y: Double, val constant: Double)
    private data class LinearEquation(val a: Double, val b: Double, val c: Double)
}
