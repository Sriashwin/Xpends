package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.BudgetCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Xpends", appName)
  }

  @Test
  fun `test budget calculation`() {
    val notepad = "01 - 120\n02 - 250\n03 - 90"
    val summary = BudgetCalculator.computeSummary(
      startingAmount = 10000.0,
      notepadText = notepad,
      otherExpensesTotal = 1050.0,
      year = 2026,
      month = 8
    )

    assertEquals(460.0, summary.totalDailyExpenses, 0.01)
    assertEquals(1050.0, summary.totalOtherExpenses, 0.01)
    assertEquals(1510.0, summary.totalExpenses, 0.01)
    assertEquals(8490.0, summary.remainingBalance, 0.01)
  }
}

