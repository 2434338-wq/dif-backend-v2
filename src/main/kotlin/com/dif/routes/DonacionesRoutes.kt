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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Route.donacionesRoutes() {
    authenticate("auth-jwt") {
        post("/donaciones") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asInt()
            val body = call.receive<DonacionRequest>()
            val usuario = transaction { Usuarios.select { Usuarios.id eq userId }.firstOrNull() }
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Usuario no encontrado"))
            val newId = transaction {
                Donaciones.insertAndGetId {
                    it[Donaciones.usuarioId] = userId
                    it[Donaciones.nombreCompleto] = usuario[Usuarios.nombre]
                    it[Donaciones.telefono] = usuario[Usuarios.telefono]
                    it[Donaciones.tipo] = body.tipo
                    it[Donaciones.descripcion] = body.descripcion
                    it[Donaciones.cantidad] = body.cantidad
                    it[Donaciones.unidad] = body.unidad
                    it[Donaciones.fecha] = body.fecha
                    it[Donaciones.estado] = "Disponible"
                    it[Donaciones.creadoEn] = LocalDateTime.now().toString()
                }.value
            }

            // Actualizar inventario automáticamente
            transaction {
                val cantidadInt = body.cantidad.toIntOrNull() ?: 1
                val existing = Inventario.select {
                    (Inventario.categoria eq body.tipo) and (Inventario.articulo eq body.descripcion)
                }.firstOrNull()
                if (existing != null) {
                    val nuevoStock = existing[Inventario.stock] + cantidadInt
                    Inventario.update({ Inventario.id eq existing[Inventario.id] }) {
                        it[Inventario.stock] = nuevoStock
                        it[Inventario.ultimoMovimiento] = LocalDateTime.now().toString()
                    }
                } else {
                    Inventario.insert {
                        it[Inventario.categoria] = body.tipo
                        it[Inventario.articulo] = body.descripcion
                        it[Inventario.stock] = cantidadInt
                        it[Inventario.stockMinimo] = 5
                        it[Inventario.unidad] = body.unidad
                        it[Inventario.ubicacion] = "Por asignar"
                        it[Inventario.ultimoMovimiento] = LocalDateTime.now().toString()
                    }
                }
            }

            transaction {
                Notificaciones.insert {
                    it[Notificaciones.usuarioId] = userId
                    it[Notificaciones.titulo] = "Donación registrada"
                    it[Notificaciones.mensaje] = "Tu donación de ${body.tipo} fue registrada con ID DON-${newId.toString().padStart(3,'0')}."
                    it[Notificaciones.tipo] = "success"
                    it[Notificaciones.creadoEn] = LocalDateTime.now().toString()
                }
            }
            call.respond(HttpStatusCode.Created, ApiResponse(true, "Donación registrada", newId))
        }

        get("/mis-donaciones") {
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.getClaim("userId").asInt()
            val lista = transaction {
                Donaciones.select { Donaciones.usuarioId eq userId }.orderBy(Donaciones.creadoEn, SortOrder.DESC).map { rowToDonacion(it) }
            }
            call.respond(lista)
        }
    }

    get("/admin/donaciones") {
        val lista = transaction { Donaciones.selectAll().orderBy(Donaciones.creadoEn, SortOrder.DESC).map { rowToDonacion(it) } }
        call.respond(lista)
    }

    post("/admin/donaciones/{id}/estado") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
        val body = call.receive<UpdateStatusRequest>()
        transaction { Donaciones.update({ Donaciones.id eq id }) { it[Donaciones.estado] = body.estado } }
        call.respond(ApiResponse(true, "Estado actualizado"))
    }

    delete("/admin/donaciones/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
        transaction { Donaciones.deleteWhere { Donaciones.id eq id } }
        call.respond(ApiResponse(true, "Donación eliminada"))
    }
}

fun rowToDonacion(row: ResultRow) = DonacionResponse(row[Donaciones.id].value, row[Donaciones.usuarioId], row[Donaciones.nombreCompleto], row[Donaciones.telefono], row[Donaciones.tipo], row[Donaciones.descripcion], row[Donaciones.cantidad], row[Donaciones.unidad], row[Donaciones.fecha], row[Donaciones.estado], row[Donaciones.creadoEn])
