package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.example.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BudgetMonth
import com.example.data.OtherExpense
import com.example.ui.theme.*
import com.example.utils.BudgetCalculator
import com.example.utils.BudgetSummary
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyBudgetScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    val budgetMonth by viewModel.budgetMonth.collectAsState()
    val otherExpenses by viewModel.otherExpenses.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var showEditStartingAmountDialog by remember { mutableStateOf(false) }
    var showAddOtherExpenseDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showMonthPickerModal by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Notepad View, 1: Day Rows View

    val monthName = remember(selectedMonth) {
        Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.xpends_logo_1785598213687),
                                    contentDescription = "Xpends Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "XPENDS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextMain,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "$monthName $selectedYear",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = DarkSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = { showCurrencyDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("currency_selector_btn")
                        ) {
                            Text(
                                text = budgetMonth.currencySymbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentBlue
                            )
                        }
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = DarkSurfaceVariant
                    ) {
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextMain)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Load Example Data") },
                                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                                onClick = {
                                    viewModel.loadSampleData()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fill Notepad Template") },
                                leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                onClick = {
                                    viewModel.generateBlankNotepadTemplate()
                                    showMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Month Selector Bar
            item {
                MonthNavigationHeader(
                    monthName = monthName,
                    year = selectedYear,
                    onPrevClick = { viewModel.selectPreviousMonth() },
                    onNextClick = { viewModel.selectNextMonth() },
                    onTitleClick = { showMonthPickerModal = true }
                )
            }

            // Summary Hero Card (Bold Typography Style)
            item {
                SummaryHeaderCard(
                    summary = summary,
                    currency = budgetMonth.currencySymbol,
                    onEditStartingAmount = { showEditStartingAmountDialog = true }
                )
            }

            // Today's Insight Banner
            item {
                TodayInsightBanner(
                    summary = summary,
                    currency = budgetMonth.currencySymbol
                )
            }

            // Daily Expenses Section Header + Notepad/Rows View
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Daily Log",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "DAILY LOG",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Total: ${budgetMonth.currencySymbol} ${formatCurrency(summary.totalDailyExpenses)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // View Mode Switcher
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = "Notepad",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Notepad", style = MaterialTheme.typography.labelSmall)
                                }
                                SegmentedButton(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarViewDay,
                                        contentDescription = "Day List",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Rows", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Text(
                            text = if (activeTab == 0)
                                "Type amounts line by line (Line 1 = Day 1, Line 2 = Day 2...)"
                            else
                                "Tap any day to update spending directly",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        if (activeTab == 0) {
                            NotepadEditorView(
                                notepadText = budgetMonth.dailyNotepadText,
                                onTextChange = { viewModel.updateDailyNotepadText(it) }
                            )
                        } else {
                            DayRowsView(
                                notepadText = budgetMonth.dailyNotepadText,
                                daysInMonth = summary.daysInMonth,
                                currency = budgetMonth.currencySymbol,
                                todayDay = summary.todayDate,
                                onDayAmountChange = { day, amount ->
                                    viewModel.updateDayExpense(day, amount)
                                }
                            )
                        }
                    }
                }
            }

            // Other Expenses Section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = "Other Expenses",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "OTHER EXPENSES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Total: ${budgetMonth.currencySymbol} ${formatCurrency(summary.totalOtherExpenses)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AccentBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = { showAddOtherExpenseDialog = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = TextMain
                                ),
                                modifier = Modifier.testTag("add_expense_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("+ Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (otherExpenses.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No other expenses added yet\n(e.g., Laundry, Travel, Rent)",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                otherExpenses.forEach { expense ->
                                    OtherExpenseRow(
                                        expense = expense,
                                        currency = budgetMonth.currencySymbol,
                                        onDelete = { viewModel.deleteOtherExpense(expense) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEditStartingAmountDialog) {
        EditStartingAmountDialog(
            currentAmount = budgetMonth.startingAmount,
            currency = budgetMonth.currencySymbol,
            onDismiss = { showEditStartingAmountDialog = false },
            onConfirm = { newAmount ->
                viewModel.updateStartingAmount(newAmount)
                showEditStartingAmountDialog = false
            }
        )
    }

    if (showAddOtherExpenseDialog) {
        AddOtherExpenseDialog(
            currency = budgetMonth.currencySymbol,
            onDismiss = { showAddOtherExpenseDialog = false },
            onConfirm = { title, amount ->
                viewModel.addOtherExpense(title, amount)
                showAddOtherExpenseDialog = false
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectorDialog(
            currentSymbol = budgetMonth.currencySymbol,
            onDismiss = { showCurrencyDialog = false },
            onSelect = { symbol ->
                viewModel.updateCurrencySymbol(symbol)
                showCurrencyDialog = false
            }
        )
    }

    if (showMonthPickerModal) {
        MonthYearPickerDialog(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onDismiss = { showMonthPickerModal = false },
            onSelect = { year, month ->
                viewModel.selectMonth(year, month)
                showMonthPickerModal = false
            }
        )
    }
}

@Composable
fun MonthNavigationHeader(
    monthName: String,
    year: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onTitleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = TextMain)
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTitleClick() },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$monthName $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }
            }

            IconButton(onClick = onNextClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = TextMain)
            }
        }
    }
}

@Composable
fun SummaryHeaderCard(
    summary: BudgetSummary,
    currency: String,
    onEditStartingAmount: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HeroCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Section: Remaining Balance & Start Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "REMAINING BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = HeroCardText.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$currency ${formatCurrency(summary.remainingBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = HeroCardText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "START",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = HeroCardText.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$currency ${formatCurrency(summary.startingAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeroCardText
                        )
                        IconButton(
                            onClick = onEditStartingAmount,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("edit_starting_amount_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Starting Amount",
                                tint = HeroCardText.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = HeroCardText.copy(alpha = 0.12f), thickness = 1.dp)

            // Bottom Section: Safe Daily Limit & Days Remaining Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAFE DAILY LIMIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = HeroCardText.copy(alpha = 0.7f)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$currency ${formatCurrency(summary.safeDailyLimit)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HeroCardText
                        )
                        Text(
                            text = "/day",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = HeroCardText.copy(alpha = 0.7f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HeroCardText,
                    contentColor = Color.White
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${summary.daysRemaining}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "DAYS LEFT",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayInsightBanner(
    summary: BudgetSummary,
    currency: String
) {
    val isSaved = summary.todaySaved >= 0
    val bannerColor = DarkSurface
    val accentColor = if (isSaved) AccentBlue else Rose600

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Today's Budget: $currency ${formatCurrency(summary.todayBudget)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Text(
                        text = "Spent Today: $currency ${formatCurrency(summary.todayExpense)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (isSaved) "Saved $currency ${formatCurrency(summary.todaySaved)}"
                    else "Over $currency ${formatCurrency(-summary.todaySaved)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun NotepadEditorView(
    notepadText: String,
    onTextChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember(notepadText) {
        mutableStateOf(TextFieldValue(text = notepadText, selection = TextRange(notepadText.length)))
    }

    val focusRequester = remember { FocusRequester() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !isEditing) {
                isEditing = true
            },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (isEditing) "EDITING NOTEPAD" else "DAILY NOTEPAD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextMuted
                    )
                }

                if (isEditing) {
                    Surface(
                        onClick = {
                            onTextChange(textFieldValue.text)
                            isEditing = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = AccentBlue,
                        contentColor = HeroCardText
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done Editing",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { isEditing = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap to edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isEditing) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != notepadText) {
                                onTextChange(newValue.text)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .focusRequester(focusRequester)
                            .testTag("notepad_text_input"),
                        placeholder = {
                            Text(
                                text = "200\n150\n90\n...\n(Line 1 = Day 1, Line 2 = Day 2)",
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 24.sp,
                            color = TextMain
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkSurfaceVariant
                        )
                    )

                    FloatingActionButton(
                        onClick = {
                            onTextChange(textFieldValue.text)
                            isEditing = false
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(40.dp),
                        containerColor = AccentBlue,
                        contentColor = HeroCardText,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                if (notepadText.isBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkBackground
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Notepad is empty",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                            Text(
                                text = "Tap inside to start typing expenses line by line (e.g. 200 on line 1 for Day 1, 150 on line 2 for Day 2).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkBackground
                    ) {
                        Text(
                            text = notepadText,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp,
                                color = TextMain
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayRowsView(
    notepadText: String,
    daysInMonth: Int,
    currency: String,
    todayDay: Int,
    onDayAmountChange: (Int, Double) -> Unit
) {
    val parsedMap = remember(notepadText) { BudgetCalculator.parseDailyExpenses(notepadText) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..daysInMonth).forEach { day ->
            val amount = parsedMap[day] ?: 0.0
            val isToday = day == todayDay

            DayRowItem(
                day = day,
                amount = amount,
                currency = currency,
                isToday = isToday,
                onAmountSave = { newAmt -> onDayAmountChange(day, newAmt) }
            )
        }
    }
}

@Composable
fun DayRowItem(
    day: Int,
    amount: Double,
    currency: String,
    isToday: Boolean,
    onAmountSave: (Double) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember(amount) { mutableStateOf(if (amount > 0) formatCurrency(amount) else "") }

    val formattedDay = String.format("%02d", day)
    val backgroundColor = if (isToday) DarkSurfaceVariant else DarkBackground

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$formattedDay -",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isToday) AccentBlue else TextMain
                )

                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentBlue,
                        contentColor = HeroCardText
                    ) {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .width(100.dp)
                            .height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextMain)
                    )
                    IconButton(
                        onClick = {
                            val cleanInput = inputText.replace(",", "").trim()
                            val newAmt = cleanInput.toDoubleOrNull() ?: 0.0
                            onAmountSave(newAmt)
                            isEditing = false
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = AccentBlue)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { isEditing = true }
                ) {
                    Text(
                        text = if (amount > 0) "$currency ${formatCurrency(amount)}" else "_",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (amount > 0) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = if (amount > 0) TextMain else AccentBlue
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Day Expense",
                        tint = TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OtherExpenseRow(
    expense: OtherExpense,
    currency: String,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DarkBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = expense.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextMuted
                )
                Text(
                    text = "$currency ${formatCurrency(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Expense",
                    tint = Rose600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EditStartingAmountDialog(
    currentAmount: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf(if (currentAmount > 0) formatCurrency(currentAmount) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Starting Amount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter total budget amount for this month:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Starting Amount ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("starting_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanText = textValue.replace(",", "").trim()
                    val amt = cleanText.toDoubleOrNull() ?: currentAmount
                    onConfirm(amt)
                },
                modifier = Modifier.testTag("save_starting_amount_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddOtherExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Other Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Laundry, Travel, Rent)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanAmt = amountText.replace(",", "").trim()
                    val amt = cleanAmt.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title, amt)
                    }
                },
                modifier = Modifier.testTag("confirm_add_expense_btn")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CurrencySelectorDialog(
    currentSymbol: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val currencies = listOf("₹", "$", "€", "£", "¥", "₱", "₩", "R$")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currencies.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { symbol ->
                            FilterChip(
                                selected = symbol == currentSymbol,
                                onClick = { onSelect(symbol) },
                                label = { Text(symbol, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun MonthYearPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onSelect: (Int, Int) -> Unit
) {
    var year by remember { mutableIntStateOf(currentYear) }
    var month by remember { mutableIntStateOf(currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Year Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Year")
                    }
                    Text("$year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Year")
                    }
                }

                HorizontalDivider()

                // Months Grid (3 columns x 4 rows)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..12).chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { m ->
                                val name = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                FilterChip(
                                    selected = m == month,
                                    onClick = { month = m },
                                    label = { Text(name) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(year, month) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatCurrency(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%,d", amount.toLong())
    } else {
        String.format(Locale.getDefault(), "%,.2f", amount)
    }
}
