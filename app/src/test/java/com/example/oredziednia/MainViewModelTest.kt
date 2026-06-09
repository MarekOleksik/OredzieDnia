package com.example.oredziednia

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sample = Apparition(
        id = 1,
        name = "Fatima",
        location = "Portugalia",
        message = "Przykładowe orędzie",
        date = "2025-01-18"
    )

    @Test
    fun `getRandomApparition publishes a fetched apparition and clears loading`() = runTest {
        val viewModel = MainViewModel(repository = FakeApparitionRepository(listOf(sample)))

        viewModel.getRandomApparition()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(sample, viewModel.currentApparition.value)
        assertNull(viewModel.errorMessageRes.value)
        assertTrue(!viewModel.isLoading.value)
    }

    @Test
    fun `getRandomApparition reports an error when the fetch fails`() = runTest {
        val viewModel = MainViewModel(repository = FakeApparitionRepository(failWith = RuntimeException("network down")))

        viewModel.getRandomApparition()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.currentApparition.value)
        assertEquals(R.string.error_fetch_failed, viewModel.errorMessageRes.value)
        assertTrue(!viewModel.isLoading.value)
    }

    @Test
    fun `getRandomApparition reports an error when the table is empty`() = runTest {
        val viewModel = MainViewModel(repository = FakeApparitionRepository(emptyList()))

        viewModel.getRandomApparition()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.currentApparition.value)
        assertEquals(R.string.error_no_apparitions, viewModel.errorMessageRes.value)
    }

    @Test
    fun `isLoading is true while the fetch is in flight, then false once it settles`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val viewModel = MainViewModel(repository = FakeApparitionRepository(listOf(sample), gate = gate))

        viewModel.getRandomApparition()
        dispatcher.scheduler.runCurrent() // let the coroutine reach the suspension point (gate.await)
        assertTrue(viewModel.isLoading.value)

        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(!viewModel.isLoading.value)
    }
}

private class FakeApparitionRepository(
    private val data: List<Apparition> = emptyList(),
    private val failWith: Throwable? = null,
    private val gate: CompletableDeferred<Unit>? = null
) : ApparitionRepository {
    override suspend fun getAll(): List<Apparition> {
        gate?.await()
        failWith?.let { throw it }
        return data
    }
}
