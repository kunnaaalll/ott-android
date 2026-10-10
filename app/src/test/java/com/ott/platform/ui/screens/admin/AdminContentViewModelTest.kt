package com.ott.platform.ui.screens.admin

import com.google.firebase.auth.FirebaseUser
import com.ott.platform.data.model.Content
import com.ott.platform.data.model.ContentStatus
import com.ott.platform.data.model.User
import com.ott.platform.data.repository.AuthRepository
import com.ott.platform.data.repository.ContentRepository
import com.ott.platform.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.every

@OptIn(ExperimentalCoroutinesApi::class)
class AdminContentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var contentRepository: ContentRepository
    private lateinit var mediaUploadRepository: com.ott.platform.data.repository.MediaUploadRepository
    private lateinit var viewModel: AdminContentViewModel

    private val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true) {
        every { uid } returns "admin_uid"
    }
    
    private val adminUser = User(id = "admin_uid", tenantId = "tenant_1", isAdmin = true)
    private val normalUser = User(id = "user_uid", tenantId = "tenant_1", isAdmin = false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        contentRepository = mockk(relaxed = true)
        mediaUploadRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAdminData as admin loads content successfully`() = runTest {
        coEvery { authRepository.currentUser } returns flowOf(mockFirebaseUser)
        coEvery { userRepository.observeUser("admin_uid") } returns flowOf(adminUser)
        coEvery { contentRepository.observeAllContentAdmin("tenant_1") } returns flowOf(emptyList())

        viewModel = AdminContentViewModel(authRepository, userRepository, contentRepository, mediaUploadRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.errorMessage.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `loadAdminData as normal user shows unauthorized error`() = runTest {
        coEvery { authRepository.currentUser } returns flowOf(mockFirebaseUser)
        coEvery { userRepository.observeUser("admin_uid") } returns flowOf(normalUser)

        viewModel = AdminContentViewModel(authRepository, userRepository, contentRepository, mediaUploadRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Unauthorized", viewModel.errorMessage.value)
    }

    @Test
    fun `createOrUpdateContent restricts saving to other tenant`() = runTest {
        coEvery { authRepository.currentUser } returns flowOf(mockFirebaseUser)
        coEvery { userRepository.observeUser("admin_uid") } returns flowOf(adminUser)
        coEvery { contentRepository.observeAllContentAdmin("tenant_1") } returns flowOf(emptyList())

        viewModel = AdminContentViewModel(authRepository, userRepository, contentRepository, mediaUploadRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val content = Content(id = "new", tenantId = "tenant_2", title = "Movie", description = "Desc")
        viewModel.createOrUpdateContent(content)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Permission denied: Cannot modify another tenant's content", viewModel.errorMessage.value)
    }
    
    @Test
    fun `publishContent calls repository with PUBLISHED status`() = runTest {
        coEvery { authRepository.currentUser } returns flowOf(mockFirebaseUser)
        coEvery { userRepository.observeUser("admin_uid") } returns flowOf(adminUser)
        coEvery { contentRepository.observeAllContentAdmin("tenant_1") } returns flowOf(emptyList())

        viewModel = AdminContentViewModel(authRepository, userRepository, contentRepository, mediaUploadRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.publishContent("content_1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { contentRepository.updateContentStatus("content_1", ContentStatus.PUBLISHED.name) }
    }
    
    @Test
    fun `uploadMedia requests auth and handles failure`() = runTest {
        coEvery { authRepository.currentUser } returns flowOf(mockFirebaseUser)
        coEvery { userRepository.observeUser("admin_uid") } returns flowOf(adminUser)
        coEvery { contentRepository.observeAllContentAdmin("tenant_1") } returns flowOf(emptyList())

        viewModel = AdminContentViewModel(authRepository, userRepository, contentRepository, mediaUploadRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { mediaUploadRepository.requestUploadAuthorization(any(), any(), any(), any(), any()) } throws RuntimeException("Network Error")

        val mediaSource = com.ott.platform.data.model.MediaSource(
            uri = mockk(relaxed = true),
            displayName = "test.mp4",
            mimeType = "video/mp4",
            sizeBytes = 1024L
        )

        viewModel.uploadMedia("content_1", com.ott.platform.data.model.MediaType.MAIN_VIDEO, mediaSource)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed to upload media: Network Error", viewModel.errorMessage.value)
    }
}
