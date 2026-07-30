package com.ttarawayou.repository

import com.ttarawayou.entity.Place
import com.ttarawayou.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByType(type: Place.PlaceType): List<Place>
}

interface WalletRepository : JpaRepository<Wallet, Long>
