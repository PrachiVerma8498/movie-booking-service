package com.preparation.moviebooking.repository;

import com.preparation.moviebooking.domain.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByCityIgnoreCaseAndShowTimeBetweenOrderByShowTimeAsc(
            String city,
            LocalDateTime start,
            LocalDateTime end
    );
}

