package com.nhajc.server.comment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhajc.server.comment.entity.Comment;
import com.nhajc.server.comment.repository.CommentRepository;
import com.nhajc.server.comment.service.impl.CommentServiceImpl;
import com.nhajc.server.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CommentPermissionsTest {
    @Test
    void ownerAndAdminCanDeleteButAnotherUserCannot() {
        var repository = mock(CommentRepository.class);
        var users = mock(UserRepository.class);
        var service =
                new CommentServiceImpl(
                        repository, users, null, null, new com.nhajc.server.common.service.UserAccess(users));
        UUID actor = UUID.randomUUID(), owner = UUID.randomUUID(), comment = UUID.randomUUID();
        when(users.findById(actor)).thenReturn(Optional.of(mock(com.nhajc.server.user.entity.User.class)));
        var entity = mock(Comment.class);
        when(entity.getUserId()).thenReturn(owner);
        when(repository.findLockedById(comment)).thenReturn(Optional.of(entity));
        assertThatThrownBy(() -> service.delete(comment, actor, false))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(403));
        verify(repository, never()).delete(entity);
        service.delete(comment, actor, true);
        verify(repository).delete(entity);
        when(users.findById(owner)).thenReturn(Optional.of(mock(com.nhajc.server.user.entity.User.class)));
        service.delete(comment, owner, false);
        verify(repository, times(2)).delete(entity);
    }

    @Test
    void missingUserGets401AndMissingCommentGets404() {
        var repository = mock(CommentRepository.class);
        var users = mock(UserRepository.class);
        var service =
                new CommentServiceImpl(
                        repository, users, null, null, new com.nhajc.server.common.service.UserAccess(users));
        UUID actor = UUID.randomUUID(), comment = UUID.randomUUID();
        assertThatThrownBy(() -> service.delete(comment, actor, true))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(401));
        when(users.findById(actor)).thenReturn(Optional.of(mock(com.nhajc.server.user.entity.User.class)));
        assertThatThrownBy(() -> service.delete(comment, actor, true))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
    }
}
