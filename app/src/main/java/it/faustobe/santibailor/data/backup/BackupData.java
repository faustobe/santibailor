package it.faustobe.santibailor.data.backup;

import java.util.List;

import it.faustobe.santibailor.data.local.entities.ImpegnoEntity;
import it.faustobe.santibailor.data.local.entities.ItemSpesaEntity;
import it.faustobe.santibailor.data.local.entities.ListaSpesaEntity;
import it.faustobe.santibailor.data.local.entities.NoteEntity;
import it.faustobe.santibailor.data.local.entities.ProdottoFrequenteEntity;
import it.faustobe.santibailor.data.local.entities.RicorrenzaEntity;

/**
 * Struttura del file di backup JSON.
 * Contiene i dati creati dall'utente: gli id originali vengono conservati
 * nel file ma riassegnati in fase di import (vedi BackupManager).
 */
public class BackupData {

    public static final int CURRENT_VERSION = 1;

    public int version;
    public long exportedAt;

    public List<ImpegnoEntity> impegni;
    public List<NoteEntity> note;
    public List<ListaSpesaEntity> listeSpesa;
    public List<ItemSpesaEntity> itemSpesa;
    public List<ProdottoFrequenteEntity> prodottiFrequenti;
    /** Solo ricorrenze non religiose (laiche/personali): i santi sono precaricati */
    public List<RicorrenzaEntity> ricorrenze;
}
