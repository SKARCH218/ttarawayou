package com.trevit.repository

import com.trevit.entity.Place
import com.trevit.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByType(type: Place.PlaceType): List<Place>
}

interface WalletRepository : JpaRepository<Wallet, Long>
