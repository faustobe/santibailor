package it.faustobe.santibailor.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import it.faustobe.santibailor.data.local.entities.ImpegnoEntity;

@Dao
public interface ImpegnoDao extends BaseDao<ImpegnoEntity> {

    /**
     * Ottiene tutti gli impegni ordinati per data
     */
    @Query("SELECT * FROM impegni ORDER BY data_ora ASC")
    LiveData<List<ImpegnoEntity>> getAllImpegni();

    /**
     * Ottiene tutti gli impegni (sincrono)
     */
    @Query("SELECT * FROM impegni ORDER BY data_ora ASC")
    List<ImpegnoEntity> getAllImpegniSync();

    /**
     * Ottiene un impegno per ID
     */
    @Query("SELECT * FROM impegni WHERE id = :id")
    LiveData<ImpegnoEntity> getImpegnoById(int id);

    /**
     * Ottiene un impegno per ID (sincrono)
     */
    @Query("SELECT * FROM impegni WHERE id = :id")
    ImpegnoEntity getImpegnoByIdSync(int id);

    /**
     * Ottiene impegni di oggi
     */
    @Query("SELECT * FROM impegni WHERE data_ora >= :startOfDay AND data_ora < :endOfDay ORDER BY data_ora ASC")
    LiveData<List<ImpegnoEntity>> getImpegniOggi(long startOfDay, long endOfDay);

    /**
     * Ottiene impegni di oggi (sincrono)
     */
    @Query("SELECT * FROM impegni WHERE data_ora >= :startOfDay AND data_ora < :endOfDay ORDER BY data_ora ASC")
    List<ImpegnoEntity> getImpegniOggiSync(long startOfDay, long endOfDay);

    /**
     * Ottiene impegni futuri (non completati)
     */
    @Query("SELECT * FROM impegni WHERE data_ora >= :now AND completato = 0 ORDER BY data_ora ASC LIMIT :limit")
    LiveData<List<ImpegnoEntity>> getImpegniFuturi(long now, int limit);

    /**
     * Ottiene impegni per categoria
     */
    @Query("SELECT * FROM impegni WHERE categoria = :categoria ORDER BY data_ora ASC")
    LiveData<List<ImpegnoEntity>> getImpegniPerCategoria(String categoria);

    /**
     * Ottiene impegni in un range di date
     */
    @Query("SELECT * FROM impegni WHERE data_ora >= :startDate AND data_ora <= :endDate ORDER BY data_ora ASC")
    LiveData<List<ImpegnoEntity>> getImpegniInRange(long startDate, long endDate);

    /**
     * Ottiene impegni completati
     */
    @Query("SELECT * FROM impegni WHERE completato = 1 ORDER BY data_ora DESC")
    LiveData<List<ImpegnoEntity>> getImpegniCompletati();

    /**
     * Ottiene impegni non completati
     */
    @Query("SELECT * FROM impegni WHERE completato = 0 ORDER BY data_ora ASC")
    LiveData<List<ImpegnoEntity>> getImpegniNonCompletati();

    /**
     * Segna un impegno come completato
     */
    @Query("UPDATE impegni SET completato = 1, updated_at = :updatedAt WHERE id = :id")
    void markAsCompletato(int id, long updatedAt);

    /**
     * Segna un impegno come non completato
     */
    @Query("UPDATE impegni SET completato = 0, updated_at = :updatedAt WHERE id = :id")
    void markAsNonCompletato(int id, long updatedAt);

    /**
     * Conta impegni totali
     */
    @Query("SELECT COUNT(*) FROM impegni")
    int getTotalCount();

    /**
     * Conta impegni di oggi
     */
    @Query("SELECT COUNT(*) FROM impegni WHERE data_ora >= :startOfDay AND data_ora < :endOfDay")
    int getCountImpegniOggi(long startOfDay, long endOfDay);

    /**
     * Conta impegni futuri non completati
     */
    @Query("SELECT COUNT(*) FROM impegni WHERE data_ora >= :now AND completato = 0")
    int getCountImpegniFuturi(long now);

    /**
     * Conta impegni completati
     */
    @Query("SELECT COUNT(*) FROM impegni WHERE completato = 1")
    int getCountImpegniCompletati();

    /**
     * Elimina impegni vecchi (più vecchi di X giorni e completati)
     */
    @Query("DELETE FROM impegni WHERE completato = 1 AND data_ora < :cutoffDate")
    void deleteOldCompletedImpegni(long cutoffDate);

    /**
     * Elimina tutti gli impegni (usato dal ripristino backup)
     */
    @Query("DELETE FROM impegni")
    void deleteAllImpegni();
}
