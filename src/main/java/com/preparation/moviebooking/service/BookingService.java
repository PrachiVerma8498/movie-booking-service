package com.preparation.moviebooking.service;

import com.preparation.moviebooking.api.dto.BookTicketsRequest;
import com.preparation.moviebooking.api.dto.BookingResponse;
import com.preparation.moviebooking.domain.Booking;
import com.preparation.moviebooking.domain.BookingStatus;
import com.preparation.moviebooking.domain.Seat;
import com.preparation.moviebooking.domain.SeatStatus;
import com.preparation.moviebooking.domain.Show;
import com.preparation.moviebooking.exception.BookingValidationException;
import com.preparation.moviebooking.exception.ResourceNotFoundException;
import com.preparation.moviebooking.repository.BookingRepository;
import com.preparation.moviebooking.repository.SeatRepository;
import com.preparation.moviebooking.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingService {

    private static final BigDecimal THIRD_TICKET_DISCOUNT_RATE = new BigDecimal("0.50");
    private static final BigDecimal AFTERNOON_DISCOUNT_RATE = new BigDecimal("0.20");

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    public BookingService(ShowRepository showRepository, SeatRepository seatRepository, BookingRepository bookingRepository) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public BookingResponse bookTickets(BookTicketsRequest request) {
        Set<String> normalizedSeatSet = normalizeAndValidateSeats(request.getSeatNumbers());
        List<String> normalizedSeatList = new ArrayList<>(normalizedSeatSet);

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.getShowId()));

        List<Seat> seats = seatRepository.findAllForUpdateByShowIdAndSeatNumbers(show.getId(), normalizedSeatList);
        if (seats.size() != normalizedSeatList.size()) {
            throw new BookingValidationException("One or more seats do not exist for this show.");
        }

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new BookingValidationException("Seat already booked: " + seat.getSeatNumber());
            }
        }

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
        }

        int ticketCount = seats.size();
        BigDecimal grossAmount = show.getBasePrice().multiply(BigDecimal.valueOf(ticketCount));

        BigDecimal thirdTicketDiscount = ticketCount >= 3
                ? show.getBasePrice().multiply(THIRD_TICKET_DISCOUNT_RATE)
                : BigDecimal.ZERO;

        BigDecimal subtotalAfterThird = grossAmount.subtract(thirdTicketDiscount);

        BigDecimal afternoonDiscount = isAfternoonShow(show)
                ? subtotalAfterThird.multiply(AFTERNOON_DISCOUNT_RATE)
                : BigDecimal.ZERO;

        BigDecimal totalDiscount = thirdTicketDiscount.add(afternoonDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = grossAmount.subtract(totalDiscount).setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setSeatNumbersCsv(String.join(",", normalizedSeatList));
        booking.setTicketCount(ticketCount);
        booking.setGrossAmount(grossAmount.setScale(2, RoundingMode.HALF_UP));
        booking.setDiscountAmount(totalDiscount);
        booking.setFinalAmount(finalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingValidationException("Booking is already cancelled.");
        }

        List<String> seatNumbers = List.of(booking.getSeatNumbersCsv().split(","));
        List<Seat> seats = seatRepository.findAllForUpdateByShowIdAndSeatNumbers(
                booking.getShow().getId(),
                seatNumbers
        );

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setShowId(booking.getShow().getId());
        response.setSeatNumbers(List.of(booking.getSeatNumbersCsv().split(",")));
        response.setTicketCount(booking.getTicketCount());
        response.setGrossAmount(booking.getGrossAmount());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setFinalAmount(booking.getFinalAmount());
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    private Set<String> normalizeAndValidateSeats(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new BookingValidationException("At least one seat must be provided.");
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String seat : seatNumbers) {
            String value = seat == null ? "" : seat.trim().toUpperCase();
            if (value.isEmpty()) {
                throw new BookingValidationException("Seat number cannot be blank.");
            }
            if (!normalized.add(value)) {
                throw new BookingValidationException("Duplicate seat in request: " + value);
            }
        }
        return normalized;
    }

    private boolean isAfternoonShow(Show show) {
        int hour = show.getShowTime().getHour();
        return hour >= 12 && hour < 18;
    }
}

