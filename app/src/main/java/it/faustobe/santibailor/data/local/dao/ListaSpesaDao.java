package it.faustobe.santibailor.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import it.faustobe.santibailor.data.local.entities.ListaSpesaEntity;

@Dao
public interface ListaSpesaDao {
    @Insert
    long insert(ListaSpesaEntity lista);

    @Update
    void update(ListaSpesaEntity lista);

    @Delete
    void delete(ListaSpesaEntity lista);

    @Query("SELECT * FROM liste_spesa ORDER BY data_creazione DESC")
    LiveData<List<ListaSpesaEntity>> getAllListe();

    @Query("SELECT * FROM liste_spesa ORDER BY data_creazione DESC")
    List<ListaSpesaEntity> getAllListeSync();

    @Query("SELECT * FROM liste_spesa WHERE id = :id")
    LiveData<ListaSpesaEntity> getListaById(int id);

    @Query("SELECT * FROM liste_spesa WHERE id = :id")
    ListaSpesaEntity getListaByIdSync(int id);

    @Query("SELECT * FROM liste_spesa WHERE completata = 0 ORDER BY data_creazione DESC")
    LiveData<List<ListaSpesaEntity>> getListeAttive();

    @Query("SELECT * FROM liste_spesa WHERE completata = 1 ORDER BY data_creazione DESC")
    LiveData<List<ListaSpesaEntity>> getListeCompletate();

    @Query("DELETE FROM liste_spesa WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM liste_spesa WHERE completata = 1")
    void deleteAllCompletate();

    @Query("DELETE FROM liste_spesa")
    void deleteAllListe();
}
