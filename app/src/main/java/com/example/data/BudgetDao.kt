package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget_months WHERE monthKey = :monthKey LIMIT 1")
    fun getBudgetMonth(monthKey: String): Flow<BudgetMonth?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudgetMonth(budgetMonth: BudgetMonth)

    @Query("SELECT * FROM other_expenses WHERE monthKey = :monthKey ORDER BY id DESC")
    fun getOtherExpenses(monthKey: String): Flow<List<OtherExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtherExpense(expense: OtherExpense)

    @Delete
    suspend fun deleteOtherExpense(expense: OtherExpense)

    @Query("DELETE FROM other_expenses WHERE id = :id")
    suspend fun deleteOtherExpenseById(id: Int)
}
