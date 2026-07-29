package com.mysterytrip.repository;

import com.mysterytrip.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> findByType(Place.PlaceType type);
}
