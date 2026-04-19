package com.dif.db

import io.ktor.server.config.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object Usuarios : IntIdTable("usuarios") {
    val nombre    = varchar("nombre", 200)
    val correo    = varchar("correo", 200)
    val password  = varchar("password", 255)
    val telefono  = varchar("telefono", 20).default("")
    val curp      = varchar("curp", 18).default("")
    val direccion = varchar("direccion", 500).default("")
    val rol       = varchar("rol", 50).default("ciudadano")
    val estado    = varchar("estado", 20).default("Activo")
    val area      = varchar("area", 100).default("")
    val fcmToken  = varchar("fcm_token", 255).default("")
    val creadoEn  = varchar("creado_en", 30).default("")
}

object Donaciones : IntIdTable("donaciones") {
    val usuarioId      = integer("usuario_id")
    val nombreCompleto = varchar("nombre_completo", 200)
    val telefono       = varchar("telefono", 20).default("")
    val tipo           = varchar("tipo", 100)
    val descripcion    = varchar("descripcion", 500).default("")
    val cantidad       = varchar("cantidad", 50).default("1")
    val unidad         = varchar("unidad", 50).default("pza")
    val fecha          = varchar("fecha", 20)
    val estado         = varchar("estado", 50).default("Disponible")
    val creadoEn       = varchar("creado_en", 30).default("")
}

object Solicitudes : IntIdTable("solicitudes") {
    val usuarioId      = integer("usuario_id")
    val nombreCompleto = varchar("nombre_completo", 200)
    val curp           = varchar("curp", 18).default("")
    val telefono       = varchar("telefono", 20).default("")
    val tipoApoyo      = varchar("tipo_apoyo", 100)
    val descripcion    = varchar("descripcion", 500).default("")
    val estatus        = varchar("estatus", 50).default("Pendiente")
    val prioridad      = varchar("prioridad", 20).default("Media")
    val imagenUrl      = varchar("imagen_url", 255).default("no_image.jpg")
    val creadoEn       = varchar("creado_en", 30).default("")
}

object Inventario : IntIdTable("inventario") {
    val categoria        = varchar("categoria", 100)
    val articulo         = varchar("articulo", 200)
    val stock            = integer("stock").default(0)
    val stockMinimo      = integer("stock_minimo").default(5)
    val unidad           = varchar("unidad", 50).default("piezas")
    val ubicacion        = varchar("ubicacion", 200).default("")
    val ultimoMovimiento = varchar("ultimo_movimiento", 30).default("")
}

object Entregas : IntIdTable("entregas") {
    val solicitudId   = integer("solicitud_id").nullable()
    val ciudadano     = varchar("ciudadano", 200)
    val articulo      = varchar("articulo", 200)
    val observaciones = varchar("observaciones", 500).default("")
    val fecha         = varchar("fecha", 20)
    val entregadoPor  = varchar("entregado_por", 200).default("")
    val creadoEn      = varchar("creado_en", 30).default("")
}

object Notificaciones : IntIdTable("notificaciones") {
    val usuarioId = integer("usuario_id")
    val titulo    = varchar("titulo", 200)
    val mensaje   = varchar("mensaje", 500)
    val tipo      = varchar("tipo", 50).default("info")
    val leida     = bool("leida").default(false)
    val creadoEn  = varchar("creado_en", 30).default("")
}

fun initDatabase(config: ApplicationConfig) {
    val url      = "jdbc:mysql://mysql.railway.internal:3306/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    val user     = "root"
    val password = "pqgVXixEukoPCwTJcVlDFSrSrqXftugk"

    Database.connect(url, driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)

    transaction {
        val count = Usuarios.select { Usuarios.correo eq "admin@dif.gob.mx" }.count()
        if (count == 0L) {
            Usuarios.insert {
                it[nombre]   = "Administrador DIF"
                it[correo]   = "admin@dif.gob.mx"
                it[password] = BCrypt.hashpw("admin123", BCrypt.gensalt())
                it[rol]      = "admin"
                it[area]     = "Dirección General"
            }
            println("✅ Admin creado")
        }
    }
    println("Conexion MySQL Railway exitosa - DIF Metepec listo en puerto 8080")
}
