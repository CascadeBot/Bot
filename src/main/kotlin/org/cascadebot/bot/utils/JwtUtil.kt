package org.cascadebot.bot.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.cascadebot.bot.Main
import java.time.Instant
import java.time.temporal.ChronoUnit

object JwtUtil {

    fun createLoginJwt(userId: Long): String {
        return JWT.create()
            .withClaim("did", userId.toString())
            .withExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
            .sign(Algorithm.ECDSA256(Main.config.dashboard.encryption.privateKey))
    }

}