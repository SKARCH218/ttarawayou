package com.trevit.dto

object WalletDtos {
    data class ProductResponse(val id: String, val tokens: Long, val badge: String?)
    data class PurchaseRequest(val productId: String)
}
