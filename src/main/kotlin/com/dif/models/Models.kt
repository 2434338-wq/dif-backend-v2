package com.dif.models

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioResponse(
    val id: Int,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val curp: String,
    val direccion: String,
    val rol: String,
    val estado: String,
    val area: String,
    val creadoEn: String
)

@Serializable
data class LoginRequest(
    val correo: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val nombre: String,
    val correo: String,
    val password: String,
    val telefono: String = "",
    val curp: String = "",
    val direccion: String = ""
)

@Serializable
data class LoginResponse(
    val token: String,
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String,
    val telefono: String,
    val curp: String,
    val direccion: String
)

@Serializable
data class DonacionRequest(
    val tipo: String,
    val descripcion: String,
    val cantidad: String,
    val unidad: String,
    val fecha: String,
    val nombreCompleto: String = "",
    val telefono: String = ""
)

@Serializable
data class DonacionResponse(
    val id: Int,
    val usuarioId: Int,
    val nombreCompleto: String,
    val telefono: String,
    val tipo: String,
    val descripcion: String,
    val cantidad: String,
    val unidad: String,
    val fecha: String,
    val estado: String,
    val creadoEn: String
)

@Serializable
data class SolicitudRequest(
    val tipoApoyo: String,
    val descripcion: String,
    val nombreCompleto: String = "",
    val curp: String = "",
    val telefono: String = ""
)

@Serializable
data class SolicitudResponse(
    val id: Int,
    val usuarioId: Int,
    val nombreCompleto: String,
    val curp: String,
    val telefono: String,
    val tipoApoyo: String,
    val descripcion: String,
    val estatus: String,
    val prioridad: String,
    val imagenUrl: String,
    val creadoEn: String
)

@Serializable
data class NotificacionResponse(
    val id: Int,
    val titulo: String,
    val mensaje: String,
    val tipo: String,
    val leida: Boolean,
    val creadoEn: String
)

@Serializable
data class UpdateStatusRequest(
    val estado: String
)

@Serializable
data class FcmTokenRequest(
    val token: String
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String = "",
    val id: Int? = null
)

@Serializable
data class InventarioResponse(
    val id: Int,
    val categoria: String,
    val articulo: String,
    val stock: Int,
    val stockMinimo: Int,
    val unidad: String,
    val ubicacion: String,
    val ultimoMovimiento: String
)

@Serializable
data class EntregaResponse(
    val id: Int,
    val solicitudId: Int?,
    val ciudadano: String,
    val articulo: String,
    val observaciones: String,
    val fecha: String,
    val entregadoPor: String,
    val creadoEn: String
)
