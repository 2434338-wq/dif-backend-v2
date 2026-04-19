package com.dif.routes

import com.dif.db.*
import com.dif.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

@Serializable data class InventarioRequest(val categoria: String, val articulo: String, val stock: Int, val stockMinimo: Int = 5, val unidad: String = "piezas", val ubicacion: String = "")
@Serializable data class AjusteStockRequest(val nuevoStock: Int, val motivo: String = "")
@Serializable data class EntregaRequest(val solicitudId: Int? = null, val ciudadano: String, val articulo: String, val observaciones: String = "", val fecha: String, val entregadoPor: String = "")

fun Route.adminRoutes() {

    get("/admin/inventario") {
        val lista = transaction {
            Inventario.selectAll().map {
                InventarioResponse(it[Inventario.id].value, it[Inventario.categoria], it[Inventario.articulo], it[Inventario.stock], it[Inventario.stockMinimo], it[Inventario.unidad], it[Inventario.ubicacion], it[Inventario.ultimoMovimiento])
            }
        }
        call.respond(lista)
    }

    post("/admin/inventario") {
        val body = call.receive<InventarioRequest>()
        val newId = transaction {
            Inventario.insertAndGetId {
                it[Inventario.categoria] = body.categoria
                it[Inventario.articulo] = body.articulo
                it[Inventario.stock] = body.stock
                it[Inventario.stockMinimo] = body.stockMinimo
                it[Inventario.unidad] = body.unidad
                it[Inventario.ubicacion] = body.ubicacion
                it[Inventario.ultimoMovimiento] = LocalDateTime.now().toString()
            }.value
        }
        call.respond(HttpStatusCode.Created, ApiResponse(true, "Artículo creado", newId))
    }

    post("/admin/inventario/{id}/ajuste") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
        val body = call.receive<AjusteStockRequest>()
        transaction {
            Inventario.update({ Inventario.id eq id }) {
                it[Inventario.stock] = body.nuevoStock
                it[Inventario.ultimoMovimiento] = LocalDateTime.now().toString()
            }
        }
        call.respond(ApiResponse(true, "Stock ajustado"))
    }

    delete("/admin/inventario/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
        transaction { Inventario.deleteWhere { Inventario.id eq id } }
        call.respond(ApiResponse(true, "Artículo eliminado"))
    }

    get("/admin/solicitudes") {
        val lista = transaction {
            Solicitudes.selectAll().orderBy(Solicitudes.creadoEn, SortOrder.DESC).map {
                SolicitudResponse(it[Solicitudes.id].value, it[Solicitudes.usuarioId], it[Solicitudes.nombreCompleto], it[Solicitudes.curp], it[Solicitudes.telefono], it[Solicitudes.tipoApoyo], it[Solicitudes.descripcion], it[Solicitudes.estatus], it[Solicitudes.prioridad], it[Solicitudes.imagenUrl], it[Solicitudes.creadoEn])
            }
        }
        call.respond(lista)
    }

    post("/admin/solicitudes/{id}/estado") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
        val body = call.receive<UpdateStatusRequest>()
        val sol = transaction { Solicitudes.select { Solicitudes.id eq id }.firstOrNull() }
        transaction { Solicitudes.update({ Solicitudes.id eq id }) { it[Solicitudes.estatus] = body.estado } }
        sol?.let {
            val userId = it[Solicitudes.usuarioId]
            val msg = if (body.estado == "Atendida") "Tu solicitud fue aprobada" else "Tu solicitud fue rechazada"
            transaction {
                Notificaciones.insert { n ->
                    n[Notificaciones.usuarioId] = userId
                    n[Notificaciones.titulo] = "Solicitud ${body.estado}"
                    n[Notificaciones.mensaje] = msg
                    n[Notificaciones.tipo] = if (body.estado == "Atendida") "success" else "danger"
                    n[Notificaciones.creadoEn] = LocalDateTime.now().toString()
                }
            }
        }
        call.respond(ApiResponse(true, "Estado actualizado"))
    }

    delete("/admin/solicitudes/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
        transaction { Solicitudes.deleteWhere { Solicitudes.id eq id } }
        call.respond(ApiResponse(true, "Solicitud eliminada"))
    }

    get("/admin/entregas") {
        val lista = transaction {
            Entregas.selectAll().orderBy(Entregas.creadoEn, SortOrder.DESC).map {
                EntregaResponse(it[Entregas.id].value, it[Entregas.solicitudId], it[Entregas.ciudadano], it[Entregas.articulo], it[Entregas.observaciones], it[Entregas.fecha], it[Entregas.entregadoPor], it[Entregas.creadoEn])
            }
        }
        call.respond(lista)
    }

    post("/admin/entregas") {
        val body = call.receive<EntregaRequest>()
        val newId = transaction {
            val id = Entregas.insertAndGetId {
                it[Entregas.solicitudId] = body.solicitudId
                it[Entregas.ciudadano] = body.ciudadano
                it[Entregas.articulo] = body.articulo
                it[Entregas.observaciones] = body.observaciones
                it[Entregas.fecha] = body.fecha
                it[Entregas.entregadoPor] = body.entregadoPor
                it[Entregas.creadoEn] = LocalDateTime.now().toString()
            }.value
            body.solicitudId?.let { solId ->
                Solicitudes.update({ Solicitudes.id eq solId }) { it[Solicitudes.estatus] = "Atendida" }
            }
            id
        }
        call.respond(HttpStatusCode.Created, ApiResponse(true, "Entrega registrada", newId))
    }

    delete("/admin/entregas/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
        transaction { Entregas.deleteWhere { Entregas.id eq id } }
        call.respond(ApiResponse(true, "Entrega eliminada"))
    }

    get("/admin/usuarios") {
        val lista = transaction {
            Usuarios.selectAll().map {
                UsuarioResponse(it[Usuarios.id].value, it[Usuarios.nombre], it[Usuarios.correo], it[Usuarios.telefono], it[Usuarios.curp], it[Usuarios.direccion], it[Usuarios.rol], it[Usuarios.estado], it[Usuarios.area], it[Usuarios.creadoEn])
            }
        }
        call.respond(lista)
    }

    delete("/admin/usuarios/{id}") {
        val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
        transaction { Usuarios.deleteWhere { Usuarios.id eq id } }
        call.respond(ApiResponse(true, "Usuario eliminado"))
    }
}
