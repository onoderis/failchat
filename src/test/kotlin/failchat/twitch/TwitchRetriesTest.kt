package failchat.twitch

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TwitchRetriesTest {

    private companion object {
        const val secret = "secret"
        val token = HelixApiToken("token", Instant.MAX)
        val expected = object : Any() {}
    }

    private val apiClientMock = mockk<TwitchApiClient>()
    private val tokenContainerMock = mockk<HelixTokenContainer>()

    @Test
    fun shouldGetAndSaveNewTokenIfTokenDoesntExist() = runBlocking<Unit> {
        // Given
        every { tokenContainerMock.getToken() } returns null
        coEvery { apiClientMock.generateToken(secret) } returns token
        every { tokenContainerMock.setToken(token) } returns Unit

        // When
        val actual = doWithRetryOnAuthError(apiClientMock, secret, tokenContainerMock) { expected }

        // Then
        assertEquals(expected, actual)
        verify(exactly = 1) { tokenContainerMock.setToken(token) }
    }

    @Test
    fun shouldRetryOnInvalidToken() = runBlocking<Unit> {
        // Given
        every { tokenContainerMock.getToken() } returns token
        coEvery { apiClientMock.generateToken(secret) } returns token
        every { tokenContainerMock.setToken(token) } returns Unit
        var calls = 0
        val op: suspend (String) -> Any = {
            calls++
            if (calls <= 1) {
                throw InvalidTokenException()
            }
            expected
        }

        // When
        val actual = doWithRetryOnAuthError(apiClientMock, secret, tokenContainerMock, op)

        // Then
        assertEquals(expected, actual)
        verify(exactly = 1) { tokenContainerMock.getToken() }
        coVerify(exactly = 1) { apiClientMock.generateToken(secret) }
        verify(exactly = 1) { tokenContainerMock.setToken(token) }
        assertEquals(2, calls)
    }

    @Test
    fun shouldNotRetryOnValidToken() = runBlocking<Unit> {
        // Given
        every { tokenContainerMock.getToken() } returns token
        var calls = 0
        val op: suspend (String) -> Any = {
            calls++
            expected
        }

        // When
        val actual = doWithRetryOnAuthError(apiClientMock, secret, tokenContainerMock, op)

        // Then
        assertEquals(expected, actual)
        verify(exactly = 1) { tokenContainerMock.getToken() }
        assertEquals(1, calls)
    }
}
