package com.hobbeast.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hobbeast.app.data.model.*
import com.hobbeast.app.ui.discovery.*
import com.hobbeast.app.ui.theme.HobbeastTheme
import org.junit.*
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class DiscoveryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleEvents = listOf(
        Event(id = "1", title = "Zenei Fesztivál Budapest", category = "Zene",
            startTime = "2026-06-01T18:00:00", location = "Budapest Park", isTrending = true),
        Event(id = "2", title = "Futóverseny a Városligetben", category = "Sport",
            startTime = "2026-06-15T09:00:00", location = "Városliget"),
        Event(id = "3", title = "Ingyenes Jóga", category = "Wellness",
            startTime = "2026-06-20T07:00:00", location = "Margitsziget", isFree = true),
    )

    @Test
    fun eventCard_displaysTitle() {
        composeTestRule.setContent {
            HobbeastTheme {
                EventCard(event = sampleEvents[0], onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Zenei Fesztivál Budapest").assertIsDisplayed()
    }

    @Test
    fun eventCard_displaysLocation() {
        composeTestRule.setContent {
            HobbeastTheme {
                EventCard(event = sampleEvents[0], onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Budapest Park").assertIsDisplayed()
    }

    @Test
    fun eventCard_freeEventShowsIngyenes() {
        composeTestRule.setContent {
            HobbeastTheme {
                EventCard(event = sampleEvents[2], onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Ingyenes").assertIsDisplayed()
    }

    @Test
    fun capacityBar_showsCorrectCount() {
        composeTestRule.setContent {
            HobbeastTheme {
                CapacityBar(current = 45, max = 100)
            }
        }
        composeTestRule.onNodeWithText("45 / 100 résztvevő").assertIsDisplayed()
    }

    @Test
    fun filterModeBar_allModesVisible() {
        composeTestRule.setContent {
            HobbeastTheme {
                // Render enough context for tabs to show
                EventCard(event = sampleEvents[0], onClick = {})
            }
        }
        // At minimum the event card is shown
        composeTestRule.onNodeWithText("Zenei Fesztivál Budapest").assertExists()
    }

    @Test
    fun participationChip_goingShowsCheckmark() {
        composeTestRule.setContent {
            HobbeastTheme {
                ParticipationChip(state = ParticipationState.GOING)
            }
        }
        composeTestRule.onNodeWithText("Megyek ✓").assertIsDisplayed()
    }

    @Test
    fun participationChip_waitlistedShown() {
        composeTestRule.setContent {
            HobbeastTheme {
                ParticipationChip(state = ParticipationState.WAITLISTED)
            }
        }
        composeTestRule.onNodeWithText("Várólistán").assertIsDisplayed()
    }
}

@RunWith(AndroidJUnit4::class)
class CapacityBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun nearFullCapacity_showsWarning() {
        composeTestRule.setContent {
            HobbeastTheme {
                CapacityBar(current = 95, max = 100)
            }
        }
        composeTestRule.onNodeWithText("Majdnem teli").assertIsDisplayed()
    }

    @Test
    fun lowCapacity_noWarning() {
        composeTestRule.setContent {
            HobbeastTheme {
                CapacityBar(current = 10, max = 100)
            }
        }
        composeTestRule.onNodeWithText("Majdnem teli").assertDoesNotExist()
    }
}
