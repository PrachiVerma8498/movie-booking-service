package com.preparation.moviebooking.repository;

import com.preparation.moviebooking.domain.Seat;
import com.preparation.moviebooking.domain.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.show.id = :showId and s.seatNumber in :seatNumbers")
    List<Seat> findAllForUpdateByShowIdAndSeatNumbers(
            @Param("showId") Long showId,
            @Param("seatNumbers") Collection<String> seatNumbers
    );

    long countByShowIdAndStatus(Long showId, SeatStatus status);
}

