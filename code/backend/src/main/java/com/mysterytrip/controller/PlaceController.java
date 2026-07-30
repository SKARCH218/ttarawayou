package com.mysterytrip.controller;

import com.mysterytrip.entity.Place;
import com.mysterytrip.repository.PlaceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceRepository repository;

    public PlaceController(PlaceRepository repository) {
        this.repository = repository;
    }

    /** GET /api/places?type=LODGING|RESTAURANT|ATTRACTION (type 없으면 전체) */
    @GetMapping
    public List<Place> getPlaces(@RequestParam(required = false) Place.PlaceType type) {
        return type == null ? repository.findAll() : repository.findByType(type);
    }
}
