package com.dif.routes

import com.dif.db.*
import com.dif.models.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDateTime

fun Route.solicitudesRoutes() {

    authenticate("auth-jwt") {

        post("/solicitudes") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId    = principal.payload.getClaim("userId").asInt()

            val usuario = transaction {
                Usuarios.select { Usuarios.id eq userId }.firstOrNull()
            } ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Usuario no encontrado"))

            var tipoApoyo  = ""
            var descripcion = ""
            var imagenUrl  = "no_image.jpg"

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "tipoApoyo"   -> tipoApoyo   = part.value
                            "descripcion" -> descripcion = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        val fileName = "sol_${System.currentTimeMillis()}.jpg"
                        val file = File("uploads/$fileName")
                        file.parentFile?.mkdirs()
                        part.streamProvider().use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        imagenUrl = fileName
                    }
                    else -> {}
                }
                part.dispose()
            }

            val newId = transaction {
                Solicitudes.insertAndGetId {
                    it[Solicitudes.usuarioId]      = userId
                    it[Solicitudes.nombreCompleto] = usuario[Usuarios.nombre]
                    it[Solicitudes.curp]           = usuario[Usuarios.curp]
                    it[Solicitudes.telefono]       = usuario[Usuarios.telefono]
                    it[Solicitudes.tipoApoyo]      = tipoApoyo
                    it[Solicitudes.descripcion]    = descripcion
                    it[Solicitudes.estatus]        = "Pendiente"
                    it[Solicitudes.prioridad]      = "Media"
                    it[Solicitudes.imagenUrl]      = imagenUrl
                    it[Solicitudes.creadoEn]       = LocalDateTime.now().toString()
                }.value
            }

            transaction {
                Notificaciones.insert {
                    it[Notificaciones.usuarioId] = userId
                    it[Notificaciones.titulo]    = "Solicitud recibida"
                    it[Notificaciones.mensaje]   = "Tu solicitud de $tipoApoyo fue registrada con ID SOL-${newId.toString().padStart(3,'0')}."
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

fun rowToSolicitud(row: ResultRow) = SolicitudResponse(row[Solicitudes.id].value, row[Solicitudes.usuarioId], row[Solicitudes.nombreCompleto], row[Solicitudes.curp], row[Solicitudes.telefono], row[Solicitudes.tipoApoyo], row[Solicitudes.descripcion], row[Solicitudes.estatus], row[Solicitudes.prioridad], row[Solicitudes.imagenUrl], row[Solicitudes.creadoEn])
