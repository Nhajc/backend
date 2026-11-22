package com.nhajc.server.admin;

import com.nhajc.server.common.pagination.PageRequests;
import com.nhajc.server.genre.entity.Genre;
import com.nhajc.server.genre.repository.GenreRepository;
import com.nhajc.server.genre.service.impl.GenreServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GenrePaginationTest {
    @Test void extraRowMarksNextPageButIsNotReturned() {
        var repository = mock(GenreRepository.class);
        var service = new GenreServiceImpl(repository);
        Genre first = new Genre(UUID.randomUUID(), "Jazz", "jazz");
        when(repository.findPage(any())).thenReturn(new SliceImpl<>(List.of(first), PageRequest.of(0, 1), true));

        var response = service.list(PageRequest.of(0, 1));
        assertThat(response.content()).singleElement().satisfies(genre -> assertThat(genre.id()).isEqualTo(first.getId()));
        assertThat(response.hasNext()).isTrue();
    }

    @Test void fullLastPageAndEmptyPageDoNotHaveNext() {
        var repository = mock(GenreRepository.class);
        var service = new GenreServiceImpl(repository);
        when(repository.findPage(any())).thenReturn(
                new SliceImpl<>(List.of(new Genre(UUID.randomUUID(), "Pop", "pop")), PageRequest.of(0, 1), false),
                new SliceImpl<>(List.of(), PageRequest.of(100, 1), false));
        assertThat(service.list(PageRequest.of(0, 1)).hasNext()).isFalse();
        var beyond = service.list(PageRequest.of(100, 1));
        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.hasNext()).isFalse();
    }

    @Test void unsupportedOffsetAndOversizedPageAreRejectedBeforeQuery() {
        assertThatThrownBy(() -> PageRequests.of(Integer.MAX_VALUE, 100))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
        var repository = mock(GenreRepository.class);
        var service = new GenreServiceImpl(repository);
        assertThatThrownBy(() -> service.list(PageRequest.of(0, 101)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(repository);
    }
}
