package com.dif.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.Date

object JwtConfig {
    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var audience: String
    lateinit var realm: String

    fun init(app: Application) {
        val cfg = app.environment.config
        secret   = cfg.property("jwt.secret").getString()
        issuer   = cfg.property("jwt.issuer").getString()
        audience = cfg.property("jwt.audience").getString()
        realm    = cfg.property("jwt.realm").getString()
    }

    fun makeToken(userId: Int, rol: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withClaim("rol", rol)
        .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) // 30 días
        .sign(Algorithm.HMAC256(secret))

    fun verifier() = JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun getAudience() = audience
}
