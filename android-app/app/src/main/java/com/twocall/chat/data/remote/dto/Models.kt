package com.twocall.chat.data.remote.dto

data class CreatePairRequestDto(
    val deviceFingerprint: String,
    val publicIdentityKey: String,
    val deviceLabel: String?
)

data class CreatePairResponseDto(
    val pairId: String,
    val pairingCode: String,
    val expiresAt: String,
    val deviceId: String,
    val accessToken: String,
    val refreshToken: String
)

data class JoinPairRequestDto(
    val code: String,
    val deviceFingerprint: String,
    val publicIdentityKey: String,
    val deviceLabel: String?
)

data class JoinPairResponseDto(
    val pairId: String,
    val deviceId: String,
    val accessToken: String,
    val refreshToken: String,
    val partnerDeviceId: String?,
    val partnerPublicKey: String?
)

data class RefreshTokenRequestDto(
    val refreshToken: String
)

data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresInMs: Long
)

data class SendMessageRequestDto(
    val clientMessageId: String,
    val ciphertextPayload: String,
    val iv: String,
    val ephemeralPublicKey: String?,
    val messageType: String,
    val replyToMessageId: String?,
    val mediaAttachmentId: String?
)

data class MessageResponseDto(
    val id: String,
    val pairId: String,
    val senderDeviceId: String,
    val clientMessageId: String,
    val ciphertextPayload: String,
    val iv: String,
    val ephemeralPublicKey: String?,
    val messageType: String,
    val replyToMessageId: String?,
    val mediaAttachmentId: String?,
    val status: String,
    val isDeleted: Boolean,
    val createdAt: String,
    val editedAt: String?,
    val reactions: Map<String, String>?
)

data class ReceiptUpdateRequestDto(
    val messageId: String,
    val status: String
)

data class ReactionRequestDto(
    val messageId: String,
    val emoji: String
)

data class AttachmentUploadResponseDto(
    val attachmentId: String,
    val fileName: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val sha256Hash: String
)

data class TurnCredentialsResponseDto(
    val username: String,
    val password: String,
    val ttl: Long,
    val urls: List<String>
)

data class DevicePairInfoResponseDto(
    val pairId: String,
    val deviceId: String,
    val partnerDeviceId: String?,
    val partnerPublicKey: String?,
    val pairStatus: String
)

data class RegisterPushTokenRequestDto(
    val fcmToken: String
)

data class WsEventDto<T>(
    val eventType: String,
    val pairId: String,
    val senderDeviceId: String,
    val recipientDeviceId: String?,
    val timestamp: String?,
    val payload: T?
)
