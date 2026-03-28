package com.preparation.moviebooking.service;

import com.preparation.moviebooking.api.dto.CreateShowRequest;
import com.preparation.moviebooking.api.dto.ShowSummaryResponse;
import com.preparation.moviebooking.domain.Seat;
import com.preparation.moviebooking.domain.SeatStatus;
import com.preparation.moviebooking.domain.Show;
import com.preparation.moviebooking.exception.BookingValidationException;
import com.preparation.moviebooking.repository.SeatRepository;
import com.preparation.moviebooking.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(ShowRepository showRepository, SeatRepository seatRepository) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ShowSummaryResponse createShow(CreateShowRequest request) {
        Set<String> normalized = normalizeAndValidateSeats(request.getSeatNumbers());

        Show show = new Show();
        show.setMovieName(request.getMovieName().trim());
        show.setTheatreName(request.getTheatreName().trim());
        show.setCity(request.getCity().trim());
        show.setShowTime(request.getShowTime());
        show.setBasePrice(request.getBasePrice());

        for (String seatNumber : normalized) {
            Seat seat = new Seat();
            seat.setSeatNumber(seatNumber);
            seat.setStatus(SeatStatus.AVAILABLE);
            show.addSeat(seat);
        }

        Show saved = showRepository.save(show);
        return toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<ShowSummaryResponse> browseShows(String city, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);

        return showRepository.findByCityIgnoreCaseAndShowTimeBetweenOrderByShowTimeAsc(city, start, end)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private ShowSummaryResponse toSummary(Show show) {
        ShowSummaryResponse response = new ShowSummaryResponse();
        response.setShowId(show.getId());
        response.setMovieName(show.getMovieName());
        response.setTheatreName(show.getTheatreName());
        response.setCity(show.getCity());
        response.setShowTime(show.getShowTime());
        response.setBasePrice(show.getBasePrice());
        response.setAvailableSeats(seatRepository.countByShowIdAndStatus(show.getId(), SeatStatus.AVAILABLE));
        return response;
    }

    private Set<String> normalizeAndValidateSeats(List<String> seatNumbers) {
        Set<String> set = new HashSet<>();
        for (String seat : seatNumbers) {
            String normalized = seat == null ? "" : seat.trim().toUpperCase();
            if (normalized.isEmpty()) {
                throw new BookingValidationException("Seat number cannot be blank.");
            }
            if (!set.add(normalized)) {
                throw new BookingValidationException("Duplicate seat in request: " + normalized);
            }
        }
        return set;
    }
}

