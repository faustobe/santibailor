package it.faustobe.santibailor.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import it.faustobe.santibailor.data.local.entities.ProdottoFrequenteEntity;

@Dao
public interface ProdottoFrequenteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProdottoFrequenteEntity prodotto);

    @Update
    void update(ProdottoFrequenteEntity prodotto);

    @Query("SELECT * FROM prodotti_frequenti ORDER BY frequenza_utilizzo DESC, ultima_data_utilizzo DESC LIMIT 50")
    LiveData<List<ProdottoFrequenteEntity>> getProdottiFrequenti();

    @Query("SELECT * FROM prodotti_frequenti WHERE nome LIKE '%' || :query || '%' ORDER BY frequenza_utilizzo DESC LIMIT 10")
    List<ProdottoFrequenteEntity> searchProdotti(String query);

    @Query("SELECT * FROM prodotti_frequenti WHERE nome = :nome LIMIT 1")
    ProdottoFrequenteEntity getProdottoByNome(String nome);

    @Query("UPDATE prodotti_frequenti SET frequenza_utilizzo = frequenza_utilizzo + 1, ultima_data_utilizzo = :timestamp WHERE nome = :nome")
    void incrementaFrequenza(String nome, long timestamp);

    @Query("DELETE FROM prodotti_frequenti WHERE id = :id")
    void delete(int id);

    @Query("SELECT * FROM prodotti_frequenti")
    List<ProdottoFrequenteEntity> getAllProdottiSync();

    @Query("DELETE FROM prodotti_frequenti")
    void deleteAllProdotti();
}
