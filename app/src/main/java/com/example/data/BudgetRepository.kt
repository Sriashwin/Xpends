package com.example.data

import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val dao: BudgetDao) {

    fun getBudgetMonth(monthKey: String): Flow<BudgetMonth?> =
        dao.getBudgetMonth(monthKey)

    suspend fun saveBudgetMonth(budgetMonth: BudgetMonth) =
        dao.insertOrUpdateBudgetMonth(budgetMonth)

    fun getOtherExpenses(monthKey: String): Flow<List<OtherExpense>> =
        dao.getOtherExpenses(monthKey)

    suspend fun addOtherExpense(expense: OtherExpense) =
        dao.insertOtherExpense(expense)

    suspend fun updateOtherExpense(expense: OtherExpense) =
        dao.updateOtherExpense(expense)

    suspend fun deleteOtherExpense(expense: OtherExpense) =
        dao.deleteOtherExpense(expense)

    suspend fun deleteOtherExpenseById(id: Int) =
        dao.deleteOtherExpenseById(id)
}
