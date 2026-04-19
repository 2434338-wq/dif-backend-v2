package com.dif.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.Date

object JwtConfig {
    private val secret   = System.getenv("JWT_SECRET")   ?: "dif_metepec_secret_2024_hidalgo"
    private val issuer   = System.getenv("JWT_ISSUER")   ?: "dif.metepec.gob.mx"
    private val audience = System.getenv("JWT_AUDIENCE") ?: "dif-app-users"
    val realm            = System.getenv("JWT_REALM")    ?: "DIF Metepec"

    fun init(app: Application) { /* valores ya cargados */ }

    fun makeToken(userId: Int, rol: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("rol", rol)
        .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
        .sign(Algorithm.HMAC256(secret))

    fun verifier() = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun getAudience() = audience
}
