package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BudgetMonth
import com.example.data.BudgetRepository
import com.example.data.OtherExpense
import com.example.utils.BudgetCalculator
import com.example.utils.BudgetSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    private val today = LocalDate.now()
    private val _selectedYear = MutableStateFlow(today.year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(today.monthValue)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _inMemoryNotepad = MutableStateFlow<Pair<String, String>?>(null)
    private var saveNotepadJob: Job? = null

    val monthKey: StateFlow<String> = combine(_selectedYear, _selectedMonth) { year, month ->
        String.format("%04d-%02d", year, month)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, String.format("%04d-%02d", today.year, today.monthValue))

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetMonth: StateFlow<BudgetMonth> = combine(
        monthKey.flatMapLatest { key -> repository.getBudgetMonth(key) },
        _inMemoryNotepad,
        monthKey
    ) { dbBudget, localNotepad, key ->
        val base = dbBudget ?: BudgetMonth(monthKey = key, startingAmount = 10000.0, dailyNotepadText = "")
        if (localNotepad != null && localNotepad.first == key) {
            base.copy(dailyNotepadText = localNotepad.second)
        } else {
            base
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetMonth(monthKey = String.format("%04d-%02d", today.year, today.monthValue), startingAmount = 10000.0)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val otherExpenses: StateFlow<List<OtherExpense>> = monthKey.flatMapLatest { key ->
        repository.getOtherExpenses(key)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<BudgetSummary> = combine(
        budgetMonth,
        otherExpenses,
        _selectedYear,
        _selectedMonth
    ) { budget, others, year, month ->
        val totalOthers = others.sumOf { it.amount }
        BudgetCalculator.computeSummary(
            startingAmount = budget.startingAmount,
            notepadText = budget.dailyNotepadText,
            otherExpensesTotal = totalOthers,
            year = year,
            month = month
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetCalculator.computeSummary(10000.0, "", 0.0, today.year, today.monthValue)
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BudgetRepository(db.budgetDao())
    }

    fun selectMonth(year: Int, month: Int) {
        _inMemoryNotepad.value = null
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun selectPreviousMonth() {
        _inMemoryNotepad.value = null
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 1) {
            _selectedMonth.value = 12
            _selectedYear.value = currentYear - 1
        } else {
            _selectedMonth.value = currentMonth - 1
        }
    }

    fun selectNextMonth() {
        _inMemoryNotepad.value = null
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value
        if (currentMonth == 12) {
            _selectedMonth.value = 1
            _selectedYear.value = currentYear + 1
        } else {
            _selectedMonth.value = currentMonth + 1
        }
    }

    fun updateStartingAmount(amount: Double) {
        viewModelScope.launch {
            val currentBudget = budgetMonth.value
            repository.saveBudgetMonth(currentBudget.copy(startingAmount = maxOf(0.0, amount)))
        }
    }

    fun updateDailyNotepadText(text: String) {
        val currentKey = monthKey.value
        _inMemoryNotepad.value = Pair(currentKey, text)

        saveNotepadJob?.cancel()
        saveNotepadJob = viewModelScope.launch {
            delay(350)
            val currentBudget = budgetMonth.value
            repository.saveBudgetMonth(currentBudget.copy(dailyNotepadText = text))
        }
    }

    fun updateDayExpense(day: Int, amount: Double) {
        val currentBudget = budgetMonth.value
        val daysInMonth = BudgetCalculator.getDaysInMonth(_selectedYear.value, _selectedMonth.value)
        val updatedText = BudgetCalculator.updateDayInNotepad(
            notepadText = currentBudget.dailyNotepadText,
            dayToUpdate = day,
            newAmount = amount,
            daysInMonth = daysInMonth
        )
        val currentKey = monthKey.value
        _inMemoryNotepad.value = Pair(currentKey, updatedText)

        saveNotepadJob?.cancel()
        saveNotepadJob = viewModelScope.launch {
            delay(200)
            repository.saveBudgetMonth(currentBudget.copy(dailyNotepadText = updatedText))
        }
    }

    fun addOtherExpense(title: String, amount: Double) {
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch {
            repository.addOtherExpense(
                OtherExpense(
                    monthKey = monthKey.value,
                    title = title.trim(),
                    amount = amount
                )
            )
        }
    }

    fun updateOtherExpense(expense: OtherExpense, title: String, amount: Double) {
        if (title.isBlank() || amount <= 0) return
        viewModelScope.launch {
            repository.updateOtherExpense(
                expense.copy(
                    title = title.trim(),
                    amount = amount
                )
            )
        }
    }

    fun deleteOtherExpense(expense: OtherExpense) {
        viewModelScope.launch {
            repository.deleteOtherExpense(expense)
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            val currentBudget = budgetMonth.value
            repository.saveBudgetMonth(currentBudget.copy(currencySymbol = symbol))
        }
    }

    fun generateBlankNotepadTemplate() {
        viewModelScope.launch {
            val days = BudgetCalculator.getDaysInMonth(_selectedYear.value, _selectedMonth.value)
            val template = BudgetCalculator.generateDefaultNotepad(days)
            val currentKey = monthKey.value
            _inMemoryNotepad.value = Pair(currentKey, template)
            val currentBudget = budgetMonth.value
            repository.saveBudgetMonth(currentBudget.copy(dailyNotepadText = template))
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val sampleNotepad = "120\n250\n90"
            val currentKey = monthKey.value
            _inMemoryNotepad.value = Pair(currentKey, sampleNotepad)
            val currentBudget = budgetMonth.value
            repository.saveBudgetMonth(
                currentBudget.copy(
                    startingAmount = 10000.0,
                    dailyNotepadText = sampleNotepad
                )
            )
            repository.addOtherExpense(OtherExpense(monthKey = monthKey.value, title = "Laundry", amount = 150.0))
            repository.addOtherExpense(OtherExpense(monthKey = monthKey.value, title = "Bus Pass", amount = 900.0))
            repository.addOtherExpense(OtherExpense(monthKey = monthKey.value, title = "Movie", amount = 200.0))
        }
    }
}
