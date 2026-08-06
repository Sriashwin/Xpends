package com.example.utils

import java.time.LocalDate
import java.time.YearMonth

data class DayExpenseItem(
    val day: Int,
    val amount: Double
)

data class DayStatusItem(
    val day: Int,
    val limit: Double,
    val spent: Double,
    val diff: Double, // limit - spent (positive = saved, negative = overspent)
    val isLogged: Boolean
)

data class BudgetSummary(
    val startingAmount: Double,
    val totalDailyExpenses: Double,
    val totalOtherExpenses: Double,
    val dailySpendingPool: Double,
    val totalExpenses: Double,
    val remainingBalance: Double,
    val daysInMonth: Int,
    val daysRemaining: Int,
    val safeDailyLimit: Double,
    val todayDate: Int,
    val todayExpense: Double,
    val todayBudget: Double,
    val todaySaved: Double,
    val isOverbudget: Boolean,
    val dayStatuses: List<DayStatusItem>
)

object BudgetCalculator {

    // Matches explicit day prefixes like "01 -", "1:", "1.", "1)", "Day 1:", "#1 -"
    private val explicitDayRegex = Regex("""^(?i)(?:day\s*|#\s*)?(0[1-9]|[12][0-9]|3[01]|[1-9])\s*[:.\-=)]\s*(.*)$""")
    private val leadingZeroDayRegex = Regex("""^(0[1-9]|[12][0-9]|3[01])\s+(.*)$""")
    private val numberRegex = Regex("""[\d,]+(?:\.\d+)?""")

    fun parseDailyExpenses(notepadText: String): Map<Int, Double> {
        val resultMap = mutableMapOf<Int, Double>()
        if (notepadText.isBlank()) return resultMap

        val lines = notepadText.split("\n")
        var lineIndex = 0

        for (rawLine in lines) {
            lineIndex++
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            var dayNumber: Int? = null
            var remainder: String = line

            val match = explicitDayRegex.find(line)
            if (match != null) {
                val day = match.groupValues[1].toIntOrNull()
                if (day != null && day in 1..31) {
                    dayNumber = day
                    remainder = match.groupValues[2]
                }
            }

            if (dayNumber == null) {
                val leadingZeroMatch = leadingZeroDayRegex.find(line)
                if (leadingZeroMatch != null) {
                    dayNumber = leadingZeroMatch.groupValues[1].toIntOrNull()
                    remainder = leadingZeroMatch.groupValues[2]
                }
            }

            if (dayNumber == null) {
                if (lineIndex in 1..31) {
                    dayNumber = lineIndex
                    remainder = line
                } else {
                    continue
                }
            }

            val amounts = extractNumbers(remainder)
            if (amounts.isNotEmpty()) {
                val lineTotal = amounts.sum()
                resultMap[dayNumber] = (resultMap[dayNumber] ?: 0.0) + lineTotal
            }
        }
        return resultMap
    }

    private fun extractNumbers(text: String): List<Double> {
        return numberRegex.findAll(text).mapNotNull { m ->
            val clean = m.value.replace(",", "")
            clean.toDoubleOrNull()
        }.filter { it > 0.0 }.toList()
    }

    fun updateDayInNotepad(notepadText: String, dayToUpdate: Int, newAmount: Double, daysInMonth: Int): String {
        val lines = if (notepadText.isBlank()) emptyList() else notepadText.split("\n")
        val updatedLines = mutableListOf<String>()
        val processedDays = mutableSetOf<Int>()

        var lineIndex = 0
        for (line in lines) {
            lineIndex++
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                updatedLines.add(line)
                continue
            }

            val match = explicitDayRegex.find(trimmed)
            val day = match?.groupValues[1]?.toIntOrNull()
                ?: leadingZeroDayRegex.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()
                ?: if (lineIndex in 1..31 && extractNumbers(trimmed).isNotEmpty()) lineIndex else null

            if (day == dayToUpdate) {
                if (newAmount > 0) {
                    val formattedDay = String.format("%02d", day)
                    val formattedAmount = if (newAmount % 1.0 == 0.0) newAmount.toLong().toString() else String.format("%.2f", newAmount)
                    updatedLines.add("$formattedDay - $formattedAmount")
                }
                processedDays.add(dayToUpdate)
                continue
            } else if (day != null && day in 1..31) {
                processedDays.add(day)
            }
            updatedLines.add(line)
        }

        if (!processedDays.contains(dayToUpdate) && newAmount > 0) {
            val formattedDay = String.format("%02d", dayToUpdate)
            val formattedAmount = if (newAmount % 1.0 == 0.0) newAmount.toLong().toString() else String.format("%.2f", newAmount)
            updatedLines.add("$formattedDay - $formattedAmount")
        }

        return updatedLines.joinToString("\n")
    }

    fun generateDefaultNotepad(daysInMonth: Int): String {
        return (1..daysInMonth).joinToString("\n") { day ->
            String.format("%02d - ", day)
        }
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        return try {
            YearMonth.of(year, month).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }

    fun computeSummary(
        startingAmount: Double,
        notepadText: String,
        otherExpensesTotal: Double,
        year: Int,
        month: Int,
        todayDate: LocalDate = LocalDate.now()
    ): BudgetSummary {
        val daysInMonth = getDaysInMonth(year, month)
        val dailyExpensesMap = parseDailyExpenses(notepadText)
        val totalDailyExpenses = dailyExpensesMap.values.sum()
        val totalExpenses = totalDailyExpenses + otherExpensesTotal
        val remainingBalance = startingAmount - totalExpenses
        val dailySpendingPool = startingAmount - otherExpensesTotal

        // Compute dynamic daily limits & status for each day in month
        val dayStatuses = mutableListOf<DayStatusItem>()
        var runningPool = dailySpendingPool

        for (d in 1..daysInMonth) {
            val daysRemainingFromD = daysInMonth - d + 1
            val dayLimit = if (daysRemainingFromD > 0) maxOf(0.0, runningPool / daysRemainingFromD) else 0.0
            val daySpent = dailyExpensesMap[d] ?: 0.0
            val isLogged = dailyExpensesMap.containsKey(d) || daySpent > 0
            val diff = dayLimit - daySpent

            dayStatuses.add(
                DayStatusItem(
                    day = d,
                    limit = dayLimit,
                    spent = daySpent,
                    diff = diff,
                    isLogged = isLogged
                )
            )

            runningPool -= daySpent
        }

        val isCurrentMonth = todayDate.year == year && todayDate.monthValue == month
        val isPastMonth = todayDate.year > year || (todayDate.year == year && todayDate.monthValue > month)
        val currentDayNum = if (isCurrentMonth) todayDate.dayOfMonth else 1

        val daysRemaining = when {
            isPastMonth -> 0
            isCurrentMonth -> maxOf(1, daysInMonth - currentDayNum + 1)
            else -> daysInMonth
        }

        // Today's dynamic daily budget allocated before today's spending
        val todayStatus = dayStatuses.find { it.day == currentDayNum }
        val todayBudget = if (isCurrentMonth) (todayStatus?.limit ?: 0.0) else 0.0
        val todayExpense = if (isCurrentMonth) (dailyExpensesMap[currentDayNum] ?: 0.0) else 0.0
        val todaySaved = todayBudget - todayExpense

        // Dynamic Safe Daily Limit going forward:
        // Calculates how much the user can safely spend per day for the remaining days of the month
        val safeDailyLimit = when {
            remainingBalance <= 0 -> 0.0
            isPastMonth -> 0.0
            isCurrentMonth -> {
                if (todayExpense == 0.0) {
                    val daysLeftIncToday = maxOf(1, daysInMonth - currentDayNum + 1)
                    maxOf(0.0, remainingBalance / daysLeftIncToday)
                } else {
                    val daysLeftAfterToday = daysInMonth - currentDayNum
                    if (daysLeftAfterToday > 0) {
                        maxOf(0.0, remainingBalance / daysLeftAfterToday)
                    } else {
                        maxOf(0.0, remainingBalance)
                    }
                }
            }
            else -> {
                maxOf(0.0, remainingBalance / daysInMonth)
            }
        }

        return BudgetSummary(
            startingAmount = startingAmount,
            totalDailyExpenses = totalDailyExpenses,
            totalOtherExpenses = otherExpensesTotal,
            dailySpendingPool = dailySpendingPool,
            totalExpenses = totalExpenses,
            remainingBalance = remainingBalance,
            daysInMonth = daysInMonth,
            daysRemaining = daysRemaining,
            safeDailyLimit = safeDailyLimit,
            todayDate = currentDayNum,
            todayExpense = todayExpense,
            todayBudget = todayBudget,
            todaySaved = todaySaved,
            isOverbudget = remainingBalance < 0,
            dayStatuses = dayStatuses
        )
    }
}
