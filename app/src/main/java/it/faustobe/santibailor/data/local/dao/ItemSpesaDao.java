package it.faustobe.santibailor.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import it.faustobe.santibailor.data.local.entities.ItemSpesaEntity;

@Dao
public interface ItemSpesaDao {
    @Insert
    long insert(ItemSpesaEntity item);

    @Update
    void update(ItemSpesaEntity item);

    @Delete
    void delete(ItemSpesaEntity item);

    @Query("SELECT * FROM item_spesa WHERE id_lista = :idLista ORDER BY completato ASC, id DESC")
    LiveData<List<ItemSpesaEntity>> getItemsByListaId(int idLista);

    @Query("SELECT * FROM item_spesa WHERE id_lista = :idLista ORDER BY ordine ASC, id ASC")
    List<ItemSpesaEntity> getItemsByListaIdSync(int idLista);

    @Query("SELECT * FROM item_spesa")
    List<ItemSpesaEntity> getAllItemsSync();

    @Query("DELETE FROM item_spesa")
    void deleteAllItems();

    @Query("SELECT * FROM item_spesa WHERE id = :id")
    ItemSpesaEntity getItemById(int id);

    @Query("SELECT * FROM item_spesa WHERE id_lista = :idLista AND completato = 0 ORDER BY ordine ASC")
    LiveData<List<ItemSpesaEntity>> getItemNonCompletati(int idLista);

    @Query("SELECT * FROM item_spesa WHERE id_lista = :idLista AND completato = 1 ORDER BY ordine ASC")
    LiveData<List<ItemSpesaEntity>> getItemCompletati(int idLista);

    @Query("DELETE FROM item_spesa WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM item_spesa WHERE id_lista = :idLista")
    void deleteAllByListaId(int idLista);

    @Query("DELETE FROM item_spesa WHERE id_lista = :idLista AND completato = 1")
    void deleteCompletatiByListaId(int idLista);

    @Query("UPDATE item_spesa SET completato = :completato WHERE id = :id")
    void updateCompletato(int id, boolean completato);

    @Query("SELECT COUNT(*) FROM item_spesa WHERE id_lista = :idLista")
    int getCountByListaId(int idLista);

    @Query("SELECT COUNT(*) FROM item_spesa WHERE id_lista = :idLista AND completato = 1")
    int getCountCompletatiByListaId(int idLista);

    @Query("SELECT COUNT(*) FROM item_spesa WHERE id_lista = :idLista AND LOWER(nome) = LOWER(:nome)")
    int countByNomeInLista(int idLista, String nome);
}
