package com.preparation.moviebooking.repository;

import com.preparation.moviebooking.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}

