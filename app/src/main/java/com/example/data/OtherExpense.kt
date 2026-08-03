package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "other_expenses")
data class OtherExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val monthKey: String, // e.g. "2026-08"
    val title: String,
    val amount: Double
)
