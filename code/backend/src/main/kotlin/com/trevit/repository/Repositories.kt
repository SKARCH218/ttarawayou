package com.trevit.repository

import com.trevit.entity.AuthToken
import com.trevit.entity.EmailVerification
import com.trevit.entity.Place
import com.trevit.entity.User
import com.trevit.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {
    fun findByType(type: Place.PlaceType): List<Place>
}

interface WalletRepository : JpaRepository<Wallet, Long>

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByGoogleSub(googleSub: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}

interface EmailVerificationRepository : JpaRepository<EmailVerification, String>

interface AuthTokenRepository : JpaRepository<AuthToken, String> {
    fun deleteByUserId(userId: Long)
}
