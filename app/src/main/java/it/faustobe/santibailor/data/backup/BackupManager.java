package it.faustobe.santibailor.data.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import it.faustobe.santibailor.data.AppDatabase;
import it.faustobe.santibailor.data.local.dao.ImpegnoDao;
import it.faustobe.santibailor.data.local.dao.ItemSpesaDao;
import it.faustobe.santibailor.data.local.dao.ListaSpesaDao;
import it.faustobe.santibailor.data.local.dao.NoteDao;
import it.faustobe.santibailor.data.local.dao.ProdottoFrequenteDao;
import it.faustobe.santibailor.data.local.dao.RicorrenzaDao;
import it.faustobe.santibailor.data.local.entities.ImpegnoEntity;
import it.faustobe.santibailor.data.local.entities.ItemSpesaEntity;
import it.faustobe.santibailor.data.local.entities.ListaSpesaEntity;
import it.faustobe.santibailor.data.local.entities.NoteEntity;
import it.faustobe.santibailor.data.local.entities.ProdottoFrequenteEntity;
import it.faustobe.santibailor.data.local.entities.RicorrenzaEntity;
import it.faustobe.santibailor.data.mapper.ImpegnoMapper;
import it.faustobe.santibailor.util.WorkManagerHelper;

/**
 * Esporta e ripristina i dati utente (impegni, note, liste spesa,
 * prodotti frequenti, ricorrenze non religiose) in/da un file JSON
 * scelto tramite Storage Access Framework.
 *
 * Il ripristino è sostitutivo: i dati utente correnti vengono eliminati
 * e rimpiazzati da quelli del backup, in un'unica transazione.
 */
@Singleton
public class BackupManager {

    private static final String TAG = "BackupManager";

    private final Context context;
    private final AppDatabase database;
    private final ImpegnoDao impegnoDao;
    private final NoteDao noteDao;
    private final ListaSpesaDao listaSpesaDao;
    private final ItemSpesaDao itemSpesaDao;
    private final ProdottoFrequenteDao prodottoFrequenteDao;
    private final RicorrenzaDao ricorrenzaDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    @Inject
    public BackupManager(@ApplicationContext Context context,
                         AppDatabase database,
                         ImpegnoDao impegnoDao,
                         NoteDao noteDao,
                         ListaSpesaDao listaSpesaDao,
                         ItemSpesaDao itemSpesaDao,
                         ProdottoFrequenteDao prodottoFrequenteDao,
                         RicorrenzaDao ricorrenzaDao) {
        this.context = context;
        this.database = database;
        this.impegnoDao = impegnoDao;
        this.noteDao = noteDao;
        this.listaSpesaDao = listaSpesaDao;
        this.itemSpesaDao = itemSpesaDao;
        this.prodottoFrequenteDao = prodottoFrequenteDao;
        this.ricorrenzaDao = ricorrenzaDao;
    }

    /**
     * Esporta tutti i dati utente nel file indicato da uri (SAF)
     */
    public void exportToUri(Uri uri, Callback callback) {
        executor.execute(() -> {
            try {
                BackupData data = new BackupData();
                data.version = BackupData.CURRENT_VERSION;
                data.exportedAt = System.currentTimeMillis();
                data.impegni = impegnoDao.getAllImpegniSync();
                data.note = noteDao.getAllNoteSync();
                data.listeSpesa = listaSpesaDao.getAllListeSync();
                data.itemSpesa = itemSpesaDao.getAllItemsSync();
                data.prodottiFrequenti = prodottoFrequenteDao.getAllProdottiSync();
                data.ricorrenze = ricorrenzaDao.getRicorrenzeNonReligioseSync();

                String json = gson.toJson(data);

                try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) {
                        throw new IllegalStateException("Impossibile aprire il file di destinazione");
                    }
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

                Log.d(TAG, "Backup esportato: " + count(data.impegni) + " impegni, "
                        + count(data.note) + " note, " + count(data.listeSpesa) + " liste, "
                        + count(data.ricorrenze) + " ricorrenze");
                postSuccess(callback);
            } catch (Exception e) {
                Log.e(TAG, "Errore durante l'export del backup", e);
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Ripristina i dati dal file indicato da uri (SAF), sostituendo quelli attuali
     */
    public void importFromUri(Uri uri, Callback callback) {
        executor.execute(() -> {
            try {
                String json = readAll(uri);
                BackupData data = gson.fromJson(json, BackupData.class);

                if (data == null || data.version <= 0 || data.version > BackupData.CURRENT_VERSION) {
                    throw new IllegalArgumentException("File di backup non valido o versione non supportata");
                }

                // Gli impegni inseriti (con i nuovi id) servono dopo la transazione
                // per rischedulare i promemoria
                List<ImpegnoEntity> impegniInseriti = new ArrayList<>();

                database.runInTransaction(() -> {
                    // Svuota i dati utente correnti
                    itemSpesaDao.deleteAllItems();
                    listaSpesaDao.deleteAllListe();
                    impegnoDao.deleteAllImpegni();
                    noteDao.deleteAllNote();
                    prodottoFrequenteDao.deleteAllProdotti();
                    ricorrenzaDao.deleteRicorrenzeNonReligiose();

                    // Reinserisce tutto con id riassegnati (evita collisioni con
                    // l'autoincrement corrente e con i santi precaricati)
                    if (data.impegni != null) {
                        for (ImpegnoEntity impegno : data.impegni) {
                            impegno.setId(0);
                            int newId = (int) impegnoDao.insert(impegno);
                            impegno.setId(newId);
                            impegniInseriti.add(impegno);
                        }
                    }

                    if (data.note != null) {
                        for (NoteEntity nota : data.note) {
                            nota.setId(0);
                            noteDao.insert(nota);
                        }
                    }

                    // Liste prima degli item: gli item vanno rimappati sui nuovi id lista
                    Map<Integer, Integer> listaIdMap = new HashMap<>();
                    if (data.listeSpesa != null) {
                        for (ListaSpesaEntity lista : data.listeSpesa) {
                            int oldId = lista.getId();
                            lista.setId(0);
                            int newId = (int) listaSpesaDao.insert(lista);
                            listaIdMap.put(oldId, newId);
                        }
                    }
                    if (data.itemSpesa != null) {
                        for (ItemSpesaEntity item : data.itemSpesa) {
                            Integer newListaId = listaIdMap.get(item.getIdLista());
                            if (newListaId == null) {
                                continue; // item orfano nel backup
                            }
                            item.setId(0);
                            item.setIdLista(newListaId);
                            itemSpesaDao.insert(item);
                        }
                    }

                    if (data.prodottiFrequenti != null) {
                        for (ProdottoFrequenteEntity prodotto : data.prodottiFrequenti) {
                            prodotto.setId(0);
                            prodottoFrequenteDao.insert(prodotto);
                        }
                    }

                    if (data.ricorrenze != null) {
                        for (RicorrenzaEntity ricorrenza : data.ricorrenze) {
                            ricorrenza.setId(0);
                            ricorrenzaDao.insert(ricorrenza);
                        }
                    }
                });

                // Riallinea i promemoria: via quelli dei vecchi impegni,
                // schedulati quelli del backup
                WorkManagerHelper.cancelAllImpegnoReminders(context);
                for (ImpegnoEntity impegno : impegniInseriti) {
                    if (impegno.isReminderEnabled() && !impegno.isCompletato()) {
                        WorkManagerHelper.scheduleImpegnoReminder(context, ImpegnoMapper.toDomain(impegno));
                    }
                }

                Log.d(TAG, "Backup ripristinato: " + impegniInseriti.size() + " impegni");
                postSuccess(callback);
            } catch (Exception e) {
                Log.e(TAG, "Errore durante il ripristino del backup", e);
                postError(callback, e.getMessage());
            }
        });
    }

    private String readAll(Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("Impossibile aprire il file di backup");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static int count(List<?> list) {
        return list != null ? list.size() : 0;
    }

    private void postSuccess(Callback callback) {
        if (callback != null) {
            mainHandler.post(callback::onSuccess);
        }
    }

    private void postError(Callback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
}
