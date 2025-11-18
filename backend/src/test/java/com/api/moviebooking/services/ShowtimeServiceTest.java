package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.api.moviebooking.helpers.mapstructs.ShowtimeMapper;
import com.api.moviebooking.models.dtos.showtime.AddShowtimeRequest;
import com.api.moviebooking.models.dtos.showtime.ShowtimeDataResponse;
import com.api.moviebooking.models.dtos.showtime.UpdateShowtimeRequest;
import com.api.moviebooking.models.entities.Movie;
import com.api.moviebooking.models.entities.Room;
import com.api.moviebooking.models.entities.Showtime;
import com.api.moviebooking.repositories.MovieRepo;
import com.api.moviebooking.repositories.RoomRepo;
import com.api.moviebooking.repositories.ShowtimeRepo;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

        @Mock
        private ShowtimeRepo showtimeRepo;

        @Mock
        private ShowtimeMapper showtimeMapper;

        @Mock
        private RoomRepo roomRepo;

        @Mock
        private MovieRepo movieRepo;

        @Mock
        private ShowtimeSeatService showtimeSeatService;

        @InjectMocks
        private ShowtimeService showtimeService;

        @Test
        void addShowtime_mapsSavesAndReturnsResponse() {
                UUID roomId = UUID.randomUUID();
                UUID movieId = UUID.randomUUID();
                LocalDateTime startTime = LocalDateTime.of(2025, 10, 25, 18, 0);

                Room room = new Room();
                room.setId(roomId);

                Movie movie = new Movie();
                movie.setId(movieId);
                movie.setDuration(120);

                AddShowtimeRequest req = AddShowtimeRequest.builder()
                                .roomId(roomId)
                                .movieId(movieId)
                                .format("2D")
                                .startTime(startTime)
                                .build();

                Showtime entity = new Showtime();
                entity.setFormat("2D");
                entity.setStartTime(startTime);

                ShowtimeDataResponse expected = ShowtimeDataResponse.builder()
                                .format("2D")
                                .startTime(startTime)
                                .build();

                when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
                when(movieRepo.findById(movieId)).thenReturn(Optional.of(movie));
                when(showtimeRepo.existsOverlappingShowtime(eq(roomId), any(UUID.class), eq(startTime),
                                any(LocalDateTime.class)))
                                .thenReturn(false);
                when(showtimeMapper.toEntity(req)).thenReturn(entity);
                when(showtimeMapper.toDataResponse(entity)).thenReturn(expected);

                ShowtimeDataResponse result = showtimeService.addShowtime(req);

                verify(showtimeRepo).save(entity);
                assertEquals(room, entity.getRoom());
                assertEquals(movie, entity.getMovie());
                assertSame(expected, result);
        }

        @Test
        void addShowtime_throwsWhenOverlapping() {
                UUID roomId = UUID.randomUUID();
                UUID movieId = UUID.randomUUID();
                LocalDateTime startTime = LocalDateTime.of(2025, 10, 25, 18, 0);

                Room room = new Room();
                room.setId(roomId);

                Movie movie = new Movie();
                movie.setId(movieId);
                movie.setDuration(120);

                AddShowtimeRequest req = AddShowtimeRequest.builder()
                                .roomId(roomId)
                                .movieId(movieId)
                                .format("2D")
                                .startTime(startTime)
                                .build();

                when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
                when(movieRepo.findById(movieId)).thenReturn(Optional.of(movie));
                when(showtimeRepo.existsOverlappingShowtime(eq(roomId), any(UUID.class), eq(startTime),
                                any(LocalDateTime.class)))
                                .thenReturn(true);

                assertThrows(IllegalArgumentException.class, () -> showtimeService.addShowtime(req));
        }

        @Test
        void updateShowtime_updatesNonNullFieldsAndSaves() {
                UUID showtimeId = UUID.randomUUID();
                UUID oldRoomId = UUID.randomUUID();
                UUID oldMovieId = UUID.randomUUID();
                UUID newRoomId = UUID.randomUUID();
                LocalDateTime oldStartTime = LocalDateTime.of(2025, 10, 25, 18, 0);
                LocalDateTime newStartTime = LocalDateTime.of(2025, 10, 25, 20, 0);

                Room oldRoom = new Room();
                oldRoom.setId(oldRoomId);

                Room newRoom = new Room();
                newRoom.setId(newRoomId);

                Movie movie = new Movie();
                movie.setId(oldMovieId);
                movie.setDuration(120);

                Showtime existing = new Showtime();
                existing.setId(showtimeId);
                existing.setRoom(oldRoom);
                existing.setMovie(movie);
                existing.setFormat("2D");
                existing.setStartTime(oldStartTime);

                UpdateShowtimeRequest req = UpdateShowtimeRequest.builder()
                                .roomId(newRoomId)
                                .format("3D")
                                .startTime(newStartTime)
                                .build();

                when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(existing));
                when(roomRepo.findById(newRoomId)).thenReturn(Optional.of(newRoom));
                when(showtimeRepo.existsOverlappingShowtime(eq(newRoomId), eq(showtimeId), eq(newStartTime),
                                any(LocalDateTime.class)))
                                .thenReturn(false);

                ShowtimeDataResponse mapped = ShowtimeDataResponse.builder()
                                .format("3D")
                                .startTime(newStartTime)
                                .build();
                when(showtimeMapper.toDataResponse(existing)).thenReturn(mapped);

                ShowtimeDataResponse result = showtimeService.updateShowtime(showtimeId, req);

                verify(showtimeRepo).save(existing);
                assertEquals(newRoom, existing.getRoom());
                assertEquals("3D", existing.getFormat());
                assertEquals(newStartTime, existing.getStartTime());

                assertSame(mapped, result);
        }

        @Test
        void updateShowtime_throwsWhenOverlapping() {
                UUID showtimeId = UUID.randomUUID();
                UUID roomId = UUID.randomUUID();
                UUID movieId = UUID.randomUUID();
                LocalDateTime oldStartTime = LocalDateTime.of(2025, 10, 25, 18, 0);
                LocalDateTime newStartTime = LocalDateTime.of(2025, 10, 25, 20, 0);

                Room room = new Room();
                room.setId(roomId);

                Movie movie = new Movie();
                movie.setId(movieId);
                movie.setDuration(120);

                Showtime existing = new Showtime();
                existing.setId(showtimeId);
                existing.setRoom(room);
                existing.setMovie(movie);
                existing.setStartTime(oldStartTime);

                UpdateShowtimeRequest req = UpdateShowtimeRequest.builder()
                                .startTime(newStartTime)
                                .build();

                when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(existing));
                when(showtimeRepo.existsOverlappingShowtime(eq(roomId), eq(showtimeId), eq(newStartTime),
                                any(LocalDateTime.class)))
                                .thenReturn(true);

                assertThrows(IllegalArgumentException.class, () -> showtimeService.updateShowtime(showtimeId, req));
        }

        @Test
        void deleteShowtime_findsAndDeletes() {
                UUID showtimeId = UUID.randomUUID();
                Showtime existing = new Showtime();
                existing.setId(showtimeId);

                when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(existing));

                showtimeService.deleteShowtime(showtimeId);

                verify(showtimeRepo).delete(existing);
        }

        @Test
        void getShowtime_returnsMappedResponse() {
                UUID showtimeId = UUID.randomUUID();
                Showtime existing = new Showtime();
                existing.setId(showtimeId);
                existing.setFormat("IMAX");

                when(showtimeRepo.findById(showtimeId)).thenReturn(Optional.of(existing));

                ShowtimeDataResponse mapped = ShowtimeDataResponse.builder()
                                .format("IMAX")
                                .build();
                when(showtimeMapper.toDataResponse(existing)).thenReturn(mapped);

                ShowtimeDataResponse result = showtimeService.getShowtime(showtimeId);
                assertSame(mapped, result);
        }

        @Test
        void getAllShowtimes_returnsMappedList() {
                Showtime showtime1 = new Showtime();
                showtime1.setFormat("2D");
                Showtime showtime2 = new Showtime();
                showtime2.setFormat("3D");

                when(showtimeRepo.findAll()).thenReturn(List.of(showtime1, showtime2));

                ShowtimeDataResponse resp1 = ShowtimeDataResponse.builder()
                                .format("2D")
                                .build();
                ShowtimeDataResponse resp2 = ShowtimeDataResponse.builder()
                                .format("3D")
                                .build();

                when(showtimeMapper.toDataResponse(showtime1)).thenReturn(resp1);
                when(showtimeMapper.toDataResponse(showtime2)).thenReturn(resp2);

                List<ShowtimeDataResponse> result = showtimeService.getAllShowtimes();

                assertEquals(2, result.size());
                assertEquals("2D", result.get(0).getFormat());
                assertEquals("3D", result.get(1).getFormat());
        }

        @Test
        void getShowtimesByMovie_returnsMappedList() {
                UUID movieId = UUID.randomUUID();
                Movie movie = new Movie();
                movie.setId(movieId);

                Showtime showtime = new Showtime();
                showtime.setFormat("2D");

                when(movieRepo.findById(movieId)).thenReturn(Optional.of(movie));
                when(showtimeRepo.findByMovieId(movieId)).thenReturn(List.of(showtime));

                ShowtimeDataResponse resp = ShowtimeDataResponse.builder()
                                .format("2D")
                                .build();
                when(showtimeMapper.toDataResponse(showtime)).thenReturn(resp);

                List<ShowtimeDataResponse> result = showtimeService.getShowtimesByMovie(movieId);

                assertEquals(1, result.size());
                assertEquals("2D", result.get(0).getFormat());
        }

        @Test
        void getUpcomingShowtimesByMovie_returnsMappedList() {
                UUID movieId = UUID.randomUUID();
                Movie movie = new Movie();
                movie.setId(movieId);

                Showtime showtime = new Showtime();
                showtime.setFormat("3D");

                when(movieRepo.findById(movieId)).thenReturn(Optional.of(movie));
                when(showtimeRepo.findUpcomingShowtimesByMovie(eq(movieId), any(LocalDateTime.class)))
                                .thenReturn(List.of(showtime));

                ShowtimeDataResponse resp = ShowtimeDataResponse.builder()
                                .format("3D")
                                .build();
                when(showtimeMapper.toDataResponse(showtime)).thenReturn(resp);

                List<ShowtimeDataResponse> result = showtimeService.getUpcomingShowtimesByMovie(movieId);

                assertEquals(1, result.size());
                assertEquals("3D", result.get(0).getFormat());
        }

        @Test
        void getShowtimesByRoom_returnsMappedList() {
                UUID roomId = UUID.randomUUID();
                Room room = new Room();
                room.setId(roomId);

                Showtime showtime = new Showtime();
                showtime.setFormat("IMAX");

                when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
                when(showtimeRepo.findByRoomId(roomId)).thenReturn(List.of(showtime));

                ShowtimeDataResponse resp = ShowtimeDataResponse.builder()
                                .format("IMAX")
                                .build();
                when(showtimeMapper.toDataResponse(showtime)).thenReturn(resp);

                List<ShowtimeDataResponse> result = showtimeService.getShowtimesByRoom(roomId);

                assertEquals(1, result.size());
                assertEquals("IMAX", result.get(0).getFormat());
        }

        @Test
        void getShowtimesByMovieAndDateRange_returnsMappedList() {
                UUID movieId = UUID.randomUUID();
                Movie movie = new Movie();
                movie.setId(movieId);

                LocalDateTime startDate = LocalDateTime.of(2025, 10, 25, 0, 0);
                LocalDateTime endDate = LocalDateTime.of(2025, 10, 26, 23, 59);

                Showtime showtime = new Showtime();
                showtime.setFormat("4DX");

                when(movieRepo.findById(movieId)).thenReturn(Optional.of(movie));
                when(showtimeRepo.findByMovieAndDateRange(movieId, startDate, endDate))
                                .thenReturn(List.of(showtime));

                ShowtimeDataResponse resp = ShowtimeDataResponse.builder()
                                .format("4DX")
                                .build();
                when(showtimeMapper.toDataResponse(showtime)).thenReturn(resp);

                List<ShowtimeDataResponse> result = showtimeService.getShowtimesByMovieAndDateRange(movieId, startDate,
                                endDate);

                assertEquals(1, result.size());
                assertEquals("4DX", result.get(0).getFormat());
        }

        @Test
        void operations_throwWhenShowtimeNotFound() {
                UUID id = UUID.randomUUID();
                when(showtimeRepo.findById(id)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class, () -> showtimeService.getShowtime(id));
                assertThrows(RuntimeException.class,
                                () -> showtimeService.updateShowtime(id, UpdateShowtimeRequest.builder().build()));
                assertThrows(RuntimeException.class, () -> showtimeService.deleteShowtime(id));
        }

        @Test
        void operations_throwWhenRoomNotFound() {
                UUID roomId = UUID.randomUUID();
                UUID movieId = UUID.randomUUID();

                Movie movie = new Movie();
                movie.setId(movieId);
                movie.setDuration(120);

                AddShowtimeRequest req = AddShowtimeRequest.builder()
                                .roomId(roomId)
                                .movieId(movieId)
                                .startTime(LocalDateTime.now())
                                .build();

                when(roomRepo.findById(roomId)).thenReturn(Optional.empty());
                // when(movieRepo.findById(movieId)).thenReturn(Optional.of(movie));

                assertThrows(RuntimeException.class, () -> showtimeService.addShowtime(req));
        }

        @Test
        void operations_throwWhenMovieNotFound() {
                UUID roomId = UUID.randomUUID();
                UUID movieId = UUID.randomUUID();

                Room room = new Room();
                room.setId(roomId);

                AddShowtimeRequest req = AddShowtimeRequest.builder()
                                .roomId(roomId)
                                .movieId(movieId)
                                .startTime(LocalDateTime.now())
                                .build();

                when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
                when(movieRepo.findById(movieId)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class, () -> showtimeService.addShowtime(req));
        }
}
