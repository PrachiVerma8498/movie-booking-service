package com.preparation.moviebooking.api;

import com.preparation.moviebooking.api.dto.CreateShowRequest;
import com.preparation.moviebooking.api.dto.ShowSummaryResponse;
import com.preparation.moviebooking.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    public ShowSummaryResponse createShow(@Valid @RequestBody CreateShowRequest request) {
        return showService.createShow(request);
    }

    @GetMapping
    public List<ShowSummaryResponse> browseShows(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return showService.browseShows(city, date);
    }
}

