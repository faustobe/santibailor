package it.faustobe.santibailor.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import it.faustobe.santibailor.data.local.entities.RicorrenzaEntity;

@Dao
public interface RicorrenzaDao extends BaseDao<RicorrenzaEntity> {

    @Query("SELECT * FROM santi")
    List<RicorrenzaEntity> getAllRicorrenze();

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese= :mese")
    LiveData<List<RicorrenzaEntity>> getRicorrenzePerGiornoMeseLiveData(int giorno, int mese);

    @Query("UPDATE santi SET image_url = :newUrl WHERE id = :ricorrenzaId")
    void updateImageUrl(int ricorrenzaId, String newUrl);

    @Query("UPDATE santi SET image_url = :newUrl WHERE id = :ricorrenzaId")
    void updateImageUrlById(int ricorrenzaId, String newUrl);

    @Query("UPDATE santi SET image_url = :newUrl WHERE image_url = :oldUrl")
    void updateImageUrlByOldUrl(String oldUrl, String newUrl);

    @Query("SELECT * FROM santi WHERE image_url IS NOT NULL AND image_url != ''")
    List<RicorrenzaEntity> getAllRicorrenzeWithImages();

    @Query("SELECT DISTINCT image_url FROM santi WHERE image_url IS NOT NULL AND image_url != ''")
    List<String> getAllImageUrls();

    @Query("SELECT DISTINCT image_url FROM santi WHERE image_url IS NOT NULL AND image_url != '' AND image_url LIKE 'file://%'")
    List<String> getImageUrlsInUse();

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese ORDER BY RANDOM()")
    LiveData<List<RicorrenzaEntity>> getRicorrenzeDelGiorno(int giorno, int mese);

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese AND id_tipo = :tipoId")
    LiveData<List<RicorrenzaEntity>> getRicorrenzeDelGiornoPerTipo(int giorno, int mese, int tipoId);

    @Query("SELECT * FROM santi WHERE id = :id")
    LiveData<RicorrenzaEntity> getRicorrenzaById(int id);

    @Query("SELECT * FROM santi WHERE giorno_del_mese = :giorno AND id_mese = :mese")
    List<RicorrenzaEntity> getRicorrenzeDelGiornoSync(int giorno, int mese);

    @Query("SELECT * FROM santi WHERE santo LIKE :nome")
    LiveData<List<RicorrenzaEntity>> cercaRicorrenzePerNome(String nome);

    @Query("SELECT COUNT(*) FROM santi")
    int getTotalItemCount();

    @Query("SELECT * FROM santi WHERE image_url = :imageUrl LIMIT 1")
    RicorrenzaEntity getRicorrenzaByImageUrl(String imageUrl);

    @Query("SELECT * FROM santi WHERE " +
            "(:nome IS NULL OR santo LIKE '%' || :nome || '%') " +
            "AND (:tipo IS NULL OR id_tipo = :tipo) " +
            "AND (:meseInizio IS NULL OR id_mese >= :meseInizio) " +
            "AND (:meseFine IS NULL OR id_mese <= :meseFine) " +
            "AND (:giornoInizio IS NULL OR giorno_del_mese>= :giornoInizio) " +
            "AND (:giornoFine IS NULL OR giorno_del_mese<= :giornoFine)")
    LiveData<List<RicorrenzaEntity>> ricercaAvanzata(String nome, Integer tipo,
                                                     Integer meseInizio, Integer meseFine,
                                                     Integer giornoInizio, Integer giornoFine);

    @Query("SELECT * FROM santi WHERE " +
            "(:nome IS NULL OR santo LIKE '%' || :nome || '%') " +
            "AND (:tipo IS NULL OR id_tipo = :tipo) " +
            "AND (:meseInizio IS NULL OR id_mese > :meseInizio OR (id_mese = :meseInizio AND giorno_del_mese>= :giornoInizio)) " +
            "AND (:meseFine IS NULL OR id_mese < :meseFine OR (id_mese = :meseFine AND giorno_del_mese<= :giornoFine)) " +
            "ORDER BY id_mese, giorno_del_mese " +
            "LIMIT :limit OFFSET :offset")
    List<RicorrenzaEntity> ricercaAvanzataPaginata(String nome, Integer tipo,
                                                   Integer meseInizio, Integer meseFine,
                                                   Integer giornoInizio, Integer giornoFine,
                                                   int limit, int offset);

    @Query("SELECT COUNT(*) FROM santi WHERE " +
            "(:nome IS NULL OR santo LIKE '%' || :nome || '%') " +
            "AND (:tipo IS NULL OR id_tipo = :tipo) " +
            "AND (:meseInizio IS NULL OR id_mese > :meseInizio OR (id_mese = :meseInizio AND giorno_del_mese>= :giornoInizio)) " +
            "AND (:meseFine IS NULL OR id_mese < :meseFine OR (id_mese = :meseFine AND giorno_del_mese<= :giornoFine))")
    int contaRisultatiRicercaAvanzata(String nome, Integer tipo,
                                      Integer meseInizio, Integer meseFine,
                                      Integer giornoInizio, Integer giornoFine);

    @Query("SELECT * FROM santi ORDER BY id_mese, giorno_del_mese")
    LiveData<List<RicorrenzaEntity>> getAllRicorrenzeOrdered();

    @RawQuery
    List<RicorrenzaEntity> eseguiRicercaAvanzata(SupportSQLiteQuery query);

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese ORDER BY RANDOM() LIMIT :limit OFFSET :offset")
    List<RicorrenzaEntity> getRicorrenzeDelGiornoPaginate(int giorno, int mese, int offset, int limit);

    @Query("SELECT COUNT(*) FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese")
    int getCountRicorrenzeDelGiorno(int giorno, int mese);

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese AND id_tipo = :tipoId LIMIT :limit OFFSET :offset")
    List<RicorrenzaEntity> getRicorrenzeDelGiornoPerTipoPaginate(int giorno, int mese, int tipoId, int offset, int limit);

    // Nuova query per ricorrenze laiche E personali insieme (tipo 2 o 3)
    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese AND id_tipo IN (2, 3) LIMIT :limit OFFSET :offset")
    List<RicorrenzaEntity> getRicorrenzeLaicheEPersonaliPaginate(int giorno, int mese, int offset, int limit);

    @Query("SELECT COUNT(*) FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese AND id_tipo = :tipoId")
    int getCountRicorrenzeDelGiornoPerTipo(int giorno, int mese, int tipoId);

    // Metodi per debug
    @Query("SELECT COUNT(*) FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese")
    int contaRicorrenzePerGiornoMese(int giorno, int mese);

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese")
    List<RicorrenzaEntity> getRicorrenzePerGiornoMese(int giorno, int mese);

    @Query("SELECT * FROM santi WHERE giorno_del_mese= :giorno AND id_mese = :mese")
    List<RicorrenzaEntity> debugRicorrenzeDelGiorno(int giorno, int mese);

    // su remoto
    @Query("UPDATE santi SET bio = :bio WHERE id = :id")
    void updateBio(int id, String bio);

    @Query("SELECT bio FROM santi WHERE id = :id")
    String getBio(int id);

    /**
     * Ricorrenze non religiose (laiche e personali), cioè i dati creati
     * dall'utente — i santi (id_tipo = 1) sono precaricati dall'asset.
     * Usate dal backup.
     */
    @Query("SELECT * FROM santi WHERE id_tipo != 1")
    List<RicorrenzaEntity> getRicorrenzeNonReligioseSync();

    @Query("DELETE FROM santi WHERE id_tipo != 1")
    void deleteRicorrenzeNonReligiose();
}