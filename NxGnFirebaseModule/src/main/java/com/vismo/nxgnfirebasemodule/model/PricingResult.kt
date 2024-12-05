package com.vismo.nxgnfirebasemodule.model

data class PricingResult(
    val applicableFee: Double,
    val applicableDiscount: Double? = null,
    val applicableTotal: Double,
)
