package com.dif.routes

import com.dif.db.*
import com.dif.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.notificacionesRoutes() {

    authenticate("auth-jwt") {

        get("/notificaciones") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            val lista = transaction {
                Notificaciones.select { Notificaciones.usuarioId eq userId }
                    .orderBy(Notificaciones.creadoEn, SortOrder.DESC)
                    .map {
                        NotificacionResponse(
                            id        = it[Notificaciones.id].value,
                            usuarioId = it[Notificaciones.usuarioId],
                            titulo    = it[Notificaciones.titulo],
                            mensaje   = it[Notificaciones.mensaje],
                            tipo      = it[Notificaciones.tipo],
                            leida     = it[Notificaciones.leida],
                            creadoEn  = it[Notificaciones.creadoEn]
                        )
                    }
            }
            call.respond(lista)
        }

        post("/notificaciones/{id}/leer") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()
            val id        = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest)

            transaction {
                Notificaciones.update({
                    (Notificaciones.id eq id) and (Notificaciones.usuarioId eq userId)
                }) { it[Notificaciones.leida] = true }
            }
            call.respond(ApiResponse(true, "Notificación marcada como leída"))
        }

        post("/notificaciones/leer-todas") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            transaction {
                Notificaciones.update({ Notificaciones.usuarioId eq userId }) {
                    it[Notificaciones.leida] = true
                }
            }
            call.respond(ApiResponse(true, "Todas las notificaciones marcadas como leídas"))
        }
    }
}
