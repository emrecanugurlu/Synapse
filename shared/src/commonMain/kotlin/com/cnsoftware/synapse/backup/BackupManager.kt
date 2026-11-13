package com.cnsoftware.synapse.backup

import com.cnsoftware.synapse.domain.NoteRepository
import com.cnsoftware.synapse.util.currentTimeMillis
import com.cnsoftware.synapse.util.formatBackupTimestamp
import kotlinx.serialization.json.Json

/** Yedekleme ve geri yükleme işlemlerinin sonucu. */
sealed interface BackupResult {
    /** [noteCount] kadar not işlendi. */
    data class Success(val noteCount: Int) : BackupResult

    /** Kullanıcı dosya seçmekten vazgeçti; hata değil. */
    data object Cancelled : BackupResult

    /** Dosya okunamadı, biçim tanınmadı veya yazma başarısız oldu. */
    data class Failure(val reason: FailureReason) : BackupResult
}

enum class FailureReason {
    /** JSON ayrıştırılamadı ya da beklenen alanlar yok. */
    INVALID_FILE,

    /** Dosya, uygulamanın okuyamadığı daha yeni bir biçim sürümünde. */
    UNSUPPORTED_VERSION,

    /** Okuma/yazma sırasında beklenmeyen bir hata. */
    IO,
}

/**
 * Yedeği üretir ve geri yükler.
 *
 * Geri yükleme **birleştirme** yapar: aynı id'ye sahip kayıtlar yedektekiyle
 * değişir, yedekte olmayan mevcut notlara dokunulmaz. Yanlış dosya seçmenin
 * bedeli böylece veri kaybı değil, sadece fazladan not olur.
 */
class BackupManager(
    private val repository: NoteRepository,
    private val fileService: BackupFileService,
) {

    private val json = Json {
        prettyPrint = true
        // İleride eklenecek alanlar eski sürümü çökertmesin.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(): BackupResult {
        return try {
            val notes = repository.getAllNotes()
            val labels = repository.getAllLabels()
            val now = currentTimeMillis()

            val payload = BackupFile(
                exportedAt = now,
                labels = labels.map { it.toBackup() },
                notes = notes.map { it.toBackup() },
            )

            val written = fileService.writeBackup(
                suggestedName = "synapse-${formatBackupTimestamp(now)}.json",
                content = json.encodeToString(payload),
            )
            if (written) BackupResult.Success(notes.size) else BackupResult.Cancelled
        } catch (e: Exception) {
            BackupResult.Failure(FailureReason.IO)
        }
    }

    suspend fun import(): BackupResult {
        val raw = try {
            fileService.readBackup()
        } catch (e: Exception) {
            return BackupResult.Failure(FailureReason.IO)
        } ?: return BackupResult.Cancelled

        val payload = try {
            json.decodeFromString<BackupFile>(raw)
        } catch (e: Exception) {
            return BackupResult.Failure(FailureReason.INVALID_FILE)
        }

        // Bilmediğimiz bir sürümü "elimizden geldiğince" okumak sessiz veri
        // bozulması demek; açıkça reddetmek daha dürüst.
        if (payload.version > BACKUP_VERSION) {
            return BackupResult.Failure(FailureReason.UNSUPPORTED_VERSION)
        }

        return try {
            val labels = payload.labels.map { it.toDomain() }
            val labelsById = labels.associateBy { it.id }
            val notes = payload.notes.map { it.toDomain(labelsById) }
            repository.mergeBackup(notes = notes, labels = labels)
            BackupResult.Success(notes.size)
        } catch (e: Exception) {
            BackupResult.Failure(FailureReason.IO)
        }
    }
}
