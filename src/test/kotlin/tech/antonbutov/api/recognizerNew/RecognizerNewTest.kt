package tech.antonbutov.api.recognizerNew

import androidx.compose.runtime.getValue
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mainViewModel.MainViewModel
import mainViewModel.MainViewModelImpl
import mainViewModel.StateButton
import org.junit.Before
import org.junit.Test
import recognizer.RecognizerNew

@ExperimentalCoroutinesApi
class RecognizerNewButtonTest {
    private lateinit var recognizer: RecognizerNew
    private lateinit var mainViewModel: MainViewModel

    @Before
    fun setup() {
        // Mock dependencies
        recognizer = mockk(relaxed = true)

        mainViewModel =
            MainViewModelImpl(
                recognizer = recognizer,
            )
    }

    @Test
    fun `initial button state should be Idle`() {
        // Get the current state
        val initialState by mainViewModel.stateButton

        // Verify initial state is Idle
        assertTrue("Initial button state should be Idle", initialState is StateButton.Idle)
    }

    @Test
    fun `click should toggle state from Idle to Start`() =
        runTest {
            // Given - initial state is Idle
            val initialState by mainViewModel.stateButton
            assertTrue("Initial state should be Idle", initialState is StateButton.Idle)

            // When - click is called
            mainViewModel.click()

            // Then - state should change to Start
            val newState = mainViewModel.stateButton.value
            assertTrue("State should change to Start after click", newState is StateButton.Start)

            advanceUntilIdle()
            // Verify microphone was started
            // coVerify(exactly = 1) { microphoneSharedFlow.run() } не выполняется потому что подписки уже нет
        }

    @Test
    fun `click should toggle state from Start to Idle`() =
        runTest {
            // Given - Click once to set state to Start
            mainViewModel.click()
            val stateAfterFirstClick by mainViewModel.stateButton
            assertTrue("State should be Start after first click", stateAfterFirstClick is StateButton.Start)
            // Verify microphone was run

            // When - Click again
            mainViewModel.click()
            // Then - State should change back to Idle
            val stateAfterSecondClick = mainViewModel.stateButton.value
            assertTrue("State should change back to Idle after second click", stateAfterSecondClick is StateButton.Idle)
        }
}
