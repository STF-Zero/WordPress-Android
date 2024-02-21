package org.wordpress.android.ui.notifications

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.NotificationsTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.CommentStore
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.Note
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.push.GCMMessageHandler
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.notifications.utils.NotificationsActionsWrapper
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderPostActionsWrapper
import org.wordpress.android.util.EventBusWrapper

private const val REQUEST_BLOG_LISTENER_PARAM_POSITION = 2

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NotificationsListViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    private lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    private lateinit var gcmMessageHandler: GCMMessageHandler

    @Mock
    private lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper

    @Mock
    private lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    private lateinit var readerPostActionsWrapper: ReaderPostActionsWrapper

    @Mock
    private lateinit var notificationsActionsWrapper: NotificationsActionsWrapper

    @Mock
    private lateinit var notificationsTableWrapper: NotificationsTableWrapper

    @Mock
    private lateinit var eventBusWrapper: EventBusWrapper

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var siteStore: SiteStore

    @Mock
    private lateinit var commentStore: CommentsStore

    @Mock
    private lateinit var  accountStore: AccountStore

    @Mock
    private lateinit var action: ActionHandler

    private lateinit var viewModel: NotificationsListViewModel

    @Before
    fun setup() {
        viewModel = NotificationsListViewModel(
            testDispatcher(),
            appPrefsWrapper,
            jetpackFeatureRemovalOverlayUtil,
            gcmMessageHandler,
            notificationsUtilsWrapper,
            appLogWrapper,
            siteStore,
            commentStore,
            readerPostTableWrapper,
            readerPostActionsWrapper,
            notificationsTableWrapper,
            notificationsActionsWrapper,
            eventBusWrapper,
            accountStore,
        )
    }

    @Test
    fun `WHEN marking a note as read THEN the note is marked as read and the notification removed from system bar`() {
        // Given
        val noteId = "1"
        val context: Context = mock()
        val note = mock<Note>()
        whenever(note.id).thenReturn(noteId)
        whenever(note.isUnread).thenReturn(true)

        // When
        viewModel.markNoteAsRead(context, listOf(note))

        // Then
        verify(gcmMessageHandler, times(1)).removeNotificationWithNoteIdFromSystemBar(context, noteId)
        verify(notificationsActionsWrapper, times(1)).markNoteAsRead(note)
        verify(note, times(1)).setRead()
        verify(notificationsTableWrapper, times(1)).saveNotes(listOf(note), false)
        verify(eventBusWrapper, times(1)).post(any())
    }

    @Test
    fun `WHEN marking a note as read THEN the read note is saved`() {
        // Given
        val noteId = "1"
        val context: Context = mock()
        val note = mock<Note>()
        whenever(note.id).thenReturn(noteId)
        whenever(note.isUnread).thenReturn(true)

        // When
        viewModel.markNoteAsRead(context, listOf(note))

        // Then
        verify(notificationsTableWrapper, times(1)).saveNotes(listOf(note), false)
        verify(eventBusWrapper, times(1)).post(any())
    }

    @Test
    fun `WHEN marking all as read THEN only the unread notes are marked as read and saved`() {
        // Given
        val noteId1 = "1"
        val noteId2 = "2"
        val context: Context = mock()
        val note1 = mock<Note>()
        val note2 = mock<Note>()
        whenever(note1.id).thenReturn(noteId1)
        whenever(note1.isUnread).thenReturn(true)
        whenever(note2.isUnread).thenReturn(false)

        // When
        viewModel.markNoteAsRead(context, listOf(note1, note2))

        // Then
        verify(gcmMessageHandler, times(1)).removeNotificationWithNoteIdFromSystemBar(context, noteId1)
        verify(notificationsActionsWrapper, times(1)).markNoteAsRead(note1)
        verify(note1, times(1)).setRead()
        verify(gcmMessageHandler, times(0)).removeNotificationWithNoteIdFromSystemBar(context, noteId2)
        verify(notificationsActionsWrapper, times(0)).markNoteAsRead(note2)
        verify(note2, times(0)).setRead()
        verify(notificationsTableWrapper, times(1)).saveNotes(listOf(note1), false)
        verify(eventBusWrapper, times(1)).post(any())
    }

    @Test
    fun `WHEN liking a comment THEN set the remote like status and save it`() = test {
        // Given
        val siteId = 1L
        val commentId = 3L
        val note = mock<Note>()
        val site = mock<SiteModel>()
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)
        whenever(note.commentId).thenReturn(commentId)
        whenever(commentStore.likeComment(site, commentId, null, true)).thenReturn(
            CommentsStore.CommentsActionPayload(
                CommentsStore.CommentsData.CommentsActionData(emptyList(), 0)
            )
        )

        // When
        viewModel.likeComment(note, true)

        // Then
        verify(note, times(1)).setLikedComment(true)
        verify(commentStore, times(1)).likeComment(site, commentId, null, true)
        verify(notificationsTableWrapper, times(1)).saveNote(note)
    }

    @Test
    fun `WHEN unliking a comment THEN set the remote like status and save it`() = test {
        // Given
        val siteId = 1L
        val commentId = 3L
        val note = mock<Note>()
        val site = mock<SiteModel>()
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)
        whenever(note.commentId).thenReturn(commentId)
        whenever(commentStore.likeComment(site, commentId, null, false)).thenReturn(
            CommentsStore.CommentsActionPayload(
                CommentsStore.CommentsData.CommentsActionData(emptyList(), 0)
            )
        )

        // When
        viewModel.likeComment(note, false)

        // Then
        verify(note, times(1)).setLikedComment(false)
        verify(commentStore, times(1)).likeComment(site, commentId, null, false)
        verify(notificationsTableWrapper, times(1)).saveNote(note)
    }

    @Test
    fun `WHEN liking a comment and changing the remote status fails THEN do not save it`() = test {
        // Given
        val siteId = 1L
        val commentId = 3L
        val note = mock<Note>()
        val site = mock<SiteModel>()
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(siteStore.getSiteBySiteId(siteId)).thenReturn(site)
        whenever(note.commentId).thenReturn(commentId)
        whenever(commentStore.likeComment(site, commentId, null, true)).thenReturn(
            CommentsStore.CommentsActionPayload(
                CommentStore.CommentError(CommentStore.CommentErrorType.GENERIC_ERROR,"error"), null)
        )

        // When
        viewModel.likeComment(note, true)

        // Then
        verify(note, times(1)).setLikedComment(true)
        verify(commentStore, times(1)).likeComment(site, commentId, null, true)
        verify(notificationsTableWrapper, times(0)).saveNote(note)
    }

    @Test
    fun `WHEN the note cannot be retrieved THEN try opening the detail view`() {
        // Given
        val noteId = "1"
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(null)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    @Test
    fun `WHEN the note is not a comment THEN open detail view`() {
        // Given
        val noteId = "1"
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.isCommentType).thenReturn(false)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    @Test
    fun `WHEN the note is a comment that can be moderated THEN open detail view`() {
        // Given
        val noteId = "1"
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(true)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    @Test
    fun `WHEN the note is a comment that cannot be moderated and the reader post exists THEN open in reader`() {
        // Given
        val noteId = "1"
        val siteId = 1L
        val postId = 2L
        val commentId = 3L
        val note = mock<Note>()
        val readerPost = mock<ReaderPost>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.commentId).thenReturn(commentId)
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(false)
        whenever(readerPostTableWrapper.getBlogPost(siteId, postId, false)).thenReturn(readerPost)

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(1)).openInTheReader(siteId, postId, commentId)
        verify(action, times(0)).openDetailView()
    }

    @Test
    fun `WHEN the note is a comment that cannot be moderated and the reader post is retrieved THEN open in reader`() {
        // Given
        val noteId = "1"
        val siteId = 1L
        val postId = 2L
        val commentId = 3L
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.commentId).thenReturn(commentId)
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(false)
        whenever(readerPostTableWrapper.getBlogPost(siteId, postId, false)).thenReturn(null)
        whenever(readerPostActionsWrapper.requestBlogPost(any(), any(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                .onSuccess(null)
        }

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(1)).openInTheReader(siteId, postId, commentId)
        verify(action, times(0)).openDetailView()
    }

    @Test
    fun `WHEN the comment note cannot be moderated and the reader post retrieval fails THEN open detail view`() {
        // Given
        val noteId = "1"
        val siteId = 1L
        val postId = 2L
        val note = mock<Note>()
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(note)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.isCommentType).thenReturn(true)
        whenever(note.canModerate()).thenReturn(false)
        whenever(readerPostTableWrapper.getBlogPost(siteId, postId, false)).thenReturn(null)
        whenever(readerPostActionsWrapper.requestBlogPost(any(), any(), any())).then {
            (it.arguments[REQUEST_BLOG_LISTENER_PARAM_POSITION] as ReaderActions.OnRequestListener<*>)
                .onFailure(500)
        }

        // When
        viewModel.openNote(noteId, action::openInTheReader, action::openDetailView)

        // Then
        verify(action, times(0)).openInTheReader(any(), any(), any())
        verify(action, times(1)).openDetailView()
    }

    interface ActionHandler {
        fun openInTheReader(siteId: Long, postId: Long, commentId: Long)

        fun openDetailView()
    }
}
