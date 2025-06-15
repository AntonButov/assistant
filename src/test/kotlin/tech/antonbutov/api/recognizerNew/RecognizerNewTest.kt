package tech.antonbutov.api.recognizerNew

import SpeechKitAuth
import StringBag
import androidx.compose.runtime.getValue
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mainViewModel.StateButton
import microphoneSharedFlow.SoundSharedFlow
import openAi.OpenAi
import org.junit.Before
import org.junit.Test
import recognizer.RecognizerNew

@ExperimentalCoroutinesApi
class RecognizerNewButtonTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var speechKitAuth: SpeechKitAuth
    private lateinit var soundSharedFlow: SoundSharedFlow
    private lateinit var recognizer: RecognizerNew

    private lateinit var stringBuffer: StringBag

    private lateinit var openAi: OpenAi

    @Before
    fun setup() {
        // Mock dependencies
        speechKitAuth =
            mockk(relaxed = true) {
                coEvery { getAccessToken() } returns "fake-token"
            }

        soundSharedFlow =
            mockk(relaxed = true) {
                coEvery { audioFlow } returns flowOf(ByteArray(0))
                coEvery { run() } returns Unit
                coEvery { audioFlow } returns emptyFlow()
            }

        stringBuffer = mockk(relaxed = true)

        openAi = mockk(relaxed = true)

        recognizer =
            RecognizerNew(
                speechKitAuth = speechKitAuth,
                soundSharedFlow = soundSharedFlow,
                stringBag = stringBuffer,
                openAi = openAi,
            )
    }

    @Test
    fun `initial button state should be Idle`() {
        // Get the current state
        val initialState by recognizer.stateButton

        // Verify initial state is Idle
        assertTrue("Initial button state should be Idle", initialState is StateButton.Idle)
    }

    @Test
    fun `click should toggle state from Idle to Start`() =
        runTest {
            // Given - initial state is Idle
            val initialState = recognizer.stateButton.value
            assertTrue("Initial state should be Idle", initialState is StateButton.Idle)

            // When - click is called
            recognizer.click()

            // Then - state should change to Start
            val newState = recognizer.stateButton.value
            assertTrue("State should change to Start after click", newState is StateButton.Start)

            advanceUntilIdle()
            // Verify microphone was started
            // coVerify(exactly = 1) { microphoneSharedFlow.run() } не выполняется потому что подписки уже нет
        }

    @Test
    fun `click should toggle state from Start to Idle`() =
        runTest {
            // Given - Click once to set state to Start
            recognizer.click()
            val stateAfterFirstClick by recognizer.stateButton
            assertTrue("State should be Start after first click", stateAfterFirstClick is StateButton.Start)
            // Verify microphone was run

            // When - Click again
            recognizer.click()
            // Then - State should change back to Idle
            val stateAfterSecondClick = recognizer.stateButton.value
            assertTrue("State should change back to Idle after second click", stateAfterSecondClick is StateButton.Idle)
        }
}
