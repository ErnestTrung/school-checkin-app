package com.ernestnguyen.schoolcheckin.ui

import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ernestnguyen.schoolcheckin.portal.ScheduleEntry
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckInSheetTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun keywordFieldTakesFocusWhenOpenedForTheCurrentClass() {
        compose.setContent {
            SchoolCheckInTheme {
                CheckInSheet(CheckInUiState(sampleEntry(), focusKeyword = true, ip = ""), onSubmit = {}, onDismiss = {})
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasSetTextAction() and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun keywordFieldStaysUnfocusedWhenOpenedByHand() {
        compose.setContent {
            SchoolCheckInTheme {
                CheckInSheet(CheckInUiState(sampleEntry(), focusKeyword = false, ip = ""), onSubmit = {}, onDismiss = {})
            }
        }
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    private fun sampleEntry() = ScheduleEntry(
        id = "1",
        classSectionId = "L1",
        subject = "Lập trình di động",
        classSection = "LTDD.01",
        room = "P.301",
        lecturer = null,
        date = LocalDate.now(),
        dateText = "",
        startHour = "13",
        startMinute = "30",
        endHour = "16",
        endMinute = "0",
        firstPeriod = "7",
        lastPeriod = "9",
        isExam = false,
        examShift = null,
        attendance = null,
    )
}
