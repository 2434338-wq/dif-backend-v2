package com.dif.routes

import com.dif.db.*
import com.dif.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Route.solicitudesRoutes() {

    authenticate("auth-jwt") {

        post("/solicitudes") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            val body = call.receive<SolicitudRequest>()

            val usuario = transaction {
                Usuarios.select { Usuarios.id eq userId }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Usuario no encontrado"))

            val newId = transaction {
                Solicitudes.insertAndGetId {
                    it[Solicitudes.usuarioId]      = userId
                    it[Solicitudes.nombreCompleto] = usuario[Usuarios.nombre]
                    it[Solicitudes.curp]           = usuario[Usuarios.curp]
                    it[Solicitudes.telefono]       = usuario[Usuarios.telefono]
                    it[Solicitudes.tipoApoyo]      = body.tipoApoyo
                    it[Solicitudes.descripcion]    = body.descripcion
                    it[Solicitudes.estatus]        = "Pendiente"
                    it[Solicitudes.prioridad]      = "Media"
                    it[Solicitudes.imagenUrl]      = body.imagenUrl ?: "no_image.jpg"
                    it[Solicitudes.creadoEn]       = LocalDateTime.now().toString()
                }.value
            }

            transaction {
                Notificaciones.insert {
                    it[Notificaciones.usuarioId] = userId
                    it[Notificaciones.titulo]    = "Solicitud recibida"
                    it[Notificaciones.mensaje]   = "Tu solicitud de ${body.tipoApoyo} fue registrada con ID SOL-${newId.toString().padStart(3,'0')}."
                    it[Notificaciones.tipo]      = "info"
                    it[Notificaciones.creadoEn]  = LocalDateTime.now().toString()
                }
            }

            call.respond(HttpStatusCode.Created, ApiResponse(true, "Solicitud registrada", newId))
        }

        get("/mis-solicitudes") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            val lista = transaction {
                Solicitudes.select { Solicitudes.usuarioId eq userId }
                    .orderBy(Solicitudes.creadoEn, SortOrder.DESC)
                    .map { rowToSolicitud(it) }
            }
            call.respond(lista)
        }
    }

    get("/admin/solicitudes/pendientes") {
        val lista = transaction {
            Solicitudes.select { Solicitudes.estatus eq "Pendiente" }
                .orderBy(Solicitudes.creadoEn, SortOrder.DESC)
                .map { rowToSolicitud(it) }
        }
        call.respond(lista)
    }
}

fun rowToSolicitud(row: ResultRow) = SolicitudResponse(
    id             = row[Solicitudes.id].value,
    usuarioId      = row[Solicitudes.usuarioId],
    nombreCompleto = row[Solicitudes.nombreCompleto],
    curp           = row[Solicitudes.curp],
    telefono       = row[Solicitudes.telefono],
    tipoApoyo      = row[Solicitudes.tipoApoyo],
    descripcion    = row[Solicitudes.descripcion],
    estatus        = row[Solicitudes.estatus],
    prioridad      = row[Solicitudes.prioridad],
    imagenUrl      = row[Solicitudes.imagenUrl],
    creadoEn       = row[Solicitudes.creadoEn]
)
