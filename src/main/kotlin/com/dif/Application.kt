package com.dif

import com.dif.db.*
import com.dif.models.UsuarioResponse
import com.dif.plugins.JwtConfig
import com.dif.routes.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {

    initDatabase(environment.config)

    JwtConfig.init(this)

    install(DefaultHeaders)

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        anyHost()
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint       = true
            isLenient         = true
            ignoreUnknownKeys = true
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Error interno"))
            )
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier())
            validate { credential ->
                if (credential.payload.audience.contains(JwtConfig.getAudience()))
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token inválido o expirado")
                )
            }
        }
    }

    val uploadsDir = File("uploads").apply { mkdirs() }

    routing {

        get("/") {
            call.respond(mapOf("status" to "DIF Metepec Backend v1.0", "ok" to true))
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        staticFiles("/uploads", uploadsDir)

        authRoutes()
        donacionesRoutes()
        solicitudesRoutes(uploadsDir)
        adminRoutes()
        notificacionesRoutes()

        authenticate("auth-jwt") {
            get("/perfil") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("userId").asInt()

                val user = transaction {
                    Usuarios.select { Usuarios.id eq userId }.firstOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Usuario no encontrado"))
                    return@get
                }

                call.respond(
                    UsuarioResponse(
                        id        = user[Usuarios.id].value,
                        nombre    = user[Usuarios.nombre],
                        correo    = user[Usuarios.correo],
                        telefono  = user[Usuarios.telefono],
                        curp      = user[Usuarios.curp],
                        direccion = user[Usuarios.direccion],
                        rol       = user[Usuarios.rol],
                        estado    = user[Usuarios.estado],
                        area      = user[Usuarios.area],
                        creadoEn  = user[Usuarios.creadoEn].toString()
                    )
                )
            }
        }
    }
}
