package com.dif.routes

import com.dif.db.Usuarios
import com.dif.models.*
import com.dif.plugins.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

fun Route.authRoutes() {

    post("/register") {
        val body = try {
            call.receive<RegisterRequest>()
        } catch (e: Exception) {
            val params = call.receiveParameters()
            RegisterRequest(
                nombre    = params["nombre"] ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Nombre requerido")),
                correo    = params["correo"] ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Correo requerido")),
                password  = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Password requerido")),
                telefono  = params["telefono"] ?: "",
                curp      = params["curp"] ?: "",
                direccion = params["direccion"] ?: ""
            )
        }

        val existe = transaction {
            Usuarios.select { Usuarios.correo eq body.correo }.count() > 0
        }
        if (existe) return@post call.respond(
            HttpStatusCode.Conflict,
            ApiResponse(false, "El correo ya está registrado")
        )

        val hashed = BCrypt.hashpw(body.password, BCrypt.gensalt())
        val newId  = transaction {
            Usuarios.insertAndGetId {
                it[Usuarios.nombre]    = body.nombre
                it[Usuarios.correo]   = body.correo
                it[Usuarios.password] = hashed
                it[Usuarios.telefono] = body.telefono
                it[Usuarios.curp]     = body.curp
                it[Usuarios.direccion]= body.direccion
                it[Usuarios.rol]      = "ciudadano"
                it[Usuarios.creadoEn] = LocalDateTime.now().toString()
            }.value
        }

        val token = JwtConfig.makeToken(newId, "ciudadano")
        call.respond(
            HttpStatusCode.Created,
            LoginResponse(
                token     = token,
                id        = newId,
                nombre    = body.nombre,
                correo    = body.correo,
                rol       = "ciudadano",
                telefono  = body.telefono,
                curp      = body.curp,
                direccion = body.direccion
            )
        )
    }

    post("/login") {
        val (correo, password) = try {
            val b = call.receive<LoginRequest>()
            Pair(b.correo, b.password)
        } catch (e: Exception) {
            val p = call.receiveParameters()
            Pair(p["correo"] ?: "", p["password"] ?: "")
        }

        if (correo.isBlank() || password.isBlank()) return@post call.respond(
            HttpStatusCode.BadRequest,
            ApiResponse(false, "Correo y password requeridos")
        )

        val user = transaction {
            Usuarios.select { Usuarios.correo eq correo }.firstOrNull()
        } ?: return@post call.respond(
            HttpStatusCode.Unauthorized,
            ApiResponse(false, "Credenciales incorrectas")
        )

        if (!BCrypt.checkpw(password, user[Usuarios.password])) return@post call.respond(
            HttpStatusCode.Unauthorized,
            ApiResponse(false, "Credenciales incorrectas")
        )

        val token = JwtConfig.makeToken(user[Usuarios.id].value, user[Usuarios.rol])
        call.respond(
            LoginResponse(
                token     = token,
                id        = user[Usuarios.id].value,
                nombre    = user[Usuarios.nombre],
                correo    = user[Usuarios.correo],
                rol       = user[Usuarios.rol],
                telefono  = user[Usuarios.telefono],
                curp      = user[Usuarios.curp],
                direccion = user[Usuarios.direccion]
            )
        )
    }
}
