package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_months")
data class BudgetMonth(
    @PrimaryKey
    val monthKey: String, // e.g. "2026-08"
    val startingAmount: Double = 10000.0,
    val dailyNotepadText: String = "",
    val currencySymbol: String = "₹"
)
