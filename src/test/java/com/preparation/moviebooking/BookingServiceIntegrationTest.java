package com.preparation.moviebooking;

import com.preparation.moviebooking.api.dto.BookTicketsRequest;
import com.preparation.moviebooking.api.dto.BookingResponse;
import com.preparation.moviebooking.api.dto.CreateShowRequest;
import com.preparation.moviebooking.api.dto.ShowSummaryResponse;
import com.preparation.moviebooking.exception.BookingValidationException;
import com.preparation.moviebooking.service.BookingService;
import com.preparation.moviebooking.service.ShowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BookingServiceIntegrationTest {

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Test
    void shouldApplyThirdTicketAndAfternoonDiscounts() {
        ShowSummaryResponse show = showService.createShow(buildShowRequest(
                "Inception",
                LocalDateTime.now().plusDays(1).withHour(14).withMinute(0),
                new BigDecimal("100.00")
        ));

        BookTicketsRequest bookTicketsRequest = new BookTicketsRequest();
        bookTicketsRequest.setShowId(show.getShowId());
        bookTicketsRequest.setSeatNumbers(List.of("A1", "A2", "A3"));

        BookingResponse booking = bookingService.bookTickets(bookTicketsRequest);

        assertEquals(new BigDecimal("300.00"), booking.getGrossAmount());
        assertEquals(new BigDecimal("100.00"), booking.getDiscountAmount());
        assertEquals(new BigDecimal("200.00"), booking.getFinalAmount());
    }

    @Test
    void shouldPreventDoubleBooking() {
        ShowSummaryResponse show = showService.createShow(buildShowRequest(
                "Interstellar",
                LocalDateTime.now().plusDays(1).withHour(20).withMinute(0),
                new BigDecimal("250.00")
        ));

        BookTicketsRequest first = new BookTicketsRequest();
        first.setShowId(show.getShowId());
        first.setSeatNumbers(List.of("B1"));
        bookingService.bookTickets(first);

        BookTicketsRequest second = new BookTicketsRequest();
        second.setShowId(show.getShowId());
        second.setSeatNumbers(List.of("B1"));

        assertThrows(BookingValidationException.class, () -> bookingService.bookTickets(second));
    }

    @Test
    void shouldReleaseSeatsOnCancellation() {
        ShowSummaryResponse show = showService.createShow(buildShowRequest(
                "Dune",
                LocalDateTime.now().plusDays(1).withHour(16).withMinute(0),
                new BigDecimal("150.00")
        ));

        BookTicketsRequest request = new BookTicketsRequest();
        request.setShowId(show.getShowId());
        request.setSeatNumbers(List.of("C1"));

        BookingResponse created = bookingService.bookTickets(request);
        bookingService.cancelBooking(created.getBookingId());

        BookingResponse rebook = bookingService.bookTickets(request);
        assertEquals(new BigDecimal("120.00"), rebook.getFinalAmount());
    }

    private CreateShowRequest buildShowRequest(String movie, LocalDateTime showTime, BigDecimal basePrice) {
        CreateShowRequest request = new CreateShowRequest();
        request.setMovieName(movie);
        request.setTheatreName("PVR Downtown");
        request.setCity("Bangalore");
        request.setShowTime(showTime);
        request.setBasePrice(basePrice);
        request.setSeatNumbers(List.of("A1", "A2", "A3", "B1", "C1"));
        return request;
    }
}

