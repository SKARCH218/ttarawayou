package com.trevit.controller

import com.trevit.entity.Place
import com.trevit.repository.PlaceRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/places")
class PlaceController(private val repository: PlaceRepository) {

    /** GET /api/places?type=LODGING|RESTAURANT|ATTRACTION (type 없으면 전체) */
    @GetMapping
    fun getPlaces(@RequestParam(required = false) type: Place.PlaceType?): List<Place> =
        if (type == null) repository.findAll() else repository.findByType(type)
}
