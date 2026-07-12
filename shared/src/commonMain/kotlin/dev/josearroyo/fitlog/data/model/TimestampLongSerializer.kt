package dev.josearroyo.fitlog.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
// 🚀 CORREGIDO: El import correcto usa SerialDescriptor en lugar de Scalar
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import dev.gitlive.firebase.firestore.Timestamp

object TimestampLongSerializer : KSerializer<Long> {
    // 🚀 CORREGIDO: Cambiado a PrimitiveSerialDescriptor
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TimestampLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        return try {
            // Si por alguna razón el campo ya viene como número ordinario, lo lee directo
            decoder.decodeLong()
        } catch (e: Exception) {
            // Interceptamos el objeto Timestamp nativo de Firestore
            val timestamp = decoder.decodeSerializableValue(Timestamp.serializer())
            // Lo transformamos limpiamente a milisegundos Unix para tu lógica interna
            timestamp.seconds * 1000L + (timestamp.nanoseconds / 1_000_000)
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        // Al guardar de vuelta, reconstruimos el objeto Timestamp para Firestore
        val seconds = value / 1000L
        val nanoseconds = ((value % 1000L) * 1_000_000L).toInt()
        encoder.encodeSerializableValue(Timestamp.serializer(), Timestamp(seconds, nanoseconds))
    }
}