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

fun Route.donacionesRoutes() {

    authenticate("auth-jwt") {

        post("/donaciones") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            val body = call.receive<DonacionRequest>()

            val usuario = transaction {
                Usuarios.select { Usuarios.id eq userId }.firstOrNull()
            } ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse(false, "Usuario no encontrado")
            )

            val newId = transaction {
                Donaciones.insertAndGetId {
                    it[Donaciones.usuarioId]      = userId
                    it[Donaciones.nombreCompleto] = usuario[Usuarios.nombre]
                    it[Donaciones.telefono]       = usuario[Usuarios.telefono]
                    it[Donaciones.tipo]           = body.tipo
                    it[Donaciones.descripcion]    = body.descripcion
                    it[Donaciones.cantidad]       = body.cantidad
                    it[Donaciones.unidad]         = body.unidad
                    it[Donaciones.fecha]          = body.fecha
                    it[Donaciones.estado]         = "Disponible"
                    it[Donaciones.creadoEn]       = LocalDateTime.now().toString()
                }.value
            }

            transaction {
                Notificaciones.insert {
                    it[Notificaciones.usuarioId] = userId
                    it[Notificaciones.titulo]    = "Donación registrada"
                    it[Notificaciones.mensaje]   = "Tu donación de ${body.tipo} fue registrada exitosamente con ID DON-${newId.toString().padStart(3,'0')}."
                    it[Notificaciones.tipo]      = "success"
                    it[Notificaciones.creadoEn]  = LocalDateTime.now().toString()
                }
            }

            call.respond(HttpStatusCode.Created, ApiResponse(true, "Donación registrada", newId))
        }

        get("/mis-donaciones") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            val lista = transaction {
                Donaciones.select { Donaciones.usuarioId eq userId }
                    .orderBy(Donaciones.creadoEn, SortOrder.DESC)
                    .map { rowToDonacion(it) }
            }
            call.respond(lista)
        }
    }

    get("/admin/donaciones") {
        val lista = transaction {
            Donaciones.selectAll()
                .orderBy(Donaciones.creadoEn, SortOrder.DESC)
                .map { rowToDonacion(it) }
        }
        call.respond(lista)
    }

    post("/admin/donaciones/{id}/estado") {
        val id   = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest)
        val body = call.receive<UpdateStatusRequest>()
        transaction {
            Donaciones.update({ Donaciones.id eq id }) { it[Donaciones.estado] = body.estado }
        }
        call.respond(ApiResponse(true, "Estado actualizado"))
    }

    delete("/admin/donaciones/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest)
        transaction { Donaciones.deleteWhere { Donaciones.id eq id } }
        call.respond(ApiResponse(true, "Donación eliminada"))
    }
}

fun rowToDonacion(row: ResultRow) = DonacionResponse(
    id             = row[Donaciones.id].value,
    usuarioId      = row[Donaciones.usuarioId],
    nombreCompleto = row[Donaciones.nombreCompleto],
    telefono       = row[Donaciones.telefono],
    tipo           = row[Donaciones.tipo],
    descripcion    = row[Donaciones.descripcion],
    cantidad       = row[Donaciones.cantidad],
    unidad         = row[Donaciones.unidad],
    fecha          = row[Donaciones.fecha],
    estado         = row[Donaciones.estado],
    creadoEn       = row[Donaciones.creadoEn]
)
