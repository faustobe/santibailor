package it.faustobe.santibailor.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import it.faustobe.santibailor.data.local.dao.ImpegnoDao;
import it.faustobe.santibailor.data.local.entities.ImpegnoEntity;
import it.faustobe.santibailor.data.mapper.ImpegnoMapper;
import it.faustobe.santibailor.domain.model.Impegno;
import it.faustobe.santibailor.util.WorkManagerHelper;

/**
 * Repository per gestire gli impegni
 * Gestisce l'accesso ai dati (Room database) e mantiene allineata
 * la schedulazione dei promemoria (WorkManager) a ogni scrittura
 */
@Singleton
public class ImpegnoRepository {

    private final ImpegnoDao impegnoDao;
    private final ExecutorService executorService;
    private final Context context;

    @Inject
    public ImpegnoRepository(ImpegnoDao impegnoDao, @ApplicationContext Context context) {
        this.impegnoDao = impegnoDao;
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Ottiene tutti gli impegni
     */
    public LiveData<List<Impegno>> getAllImpegni() {
        return Transformations.map(
                impegnoDao.getAllImpegni(),
                ImpegnoMapper::toDomainList
        );
    }

    /**
     * Ottiene un impegno per ID
     */
    public LiveData<Impegno> getImpegnoById(int id) {
        return Transformations.map(
                impegnoDao.getImpegnoById(id),
                ImpegnoMapper::toDomain
        );
    }

    /**
     * Ottiene impegni di oggi
     */
    public LiveData<List<Impegno>> getImpegniOggi() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endOfDay = calendar.getTimeInMillis();

        return Transformations.map(
                impegnoDao.getImpegniOggi(startOfDay, endOfDay),
                ImpegnoMapper::toDomainList
        );
    }

    /**
     * Ottiene impegni futuri (prossimi N impegni non completati)
     */
    public LiveData<List<Impegno>> getImpegniFuturi(int limit) {
        long now = System.currentTimeMillis();
        return Transformations.map(
                impegnoDao.getImpegniFuturi(now, limit),
                ImpegnoMapper::toDomainList
        );
    }

    /**
     * Ottiene impegni per categoria
     */
    public LiveData<List<Impegno>> getImpegniPerCategoria(String categoria) {
        return Transformations.map(
                impegnoDao.getImpegniPerCategoria(categoria),
                ImpegnoMapper::toDomainList
        );
    }

    /**
     * Ottiene impegni in un range di date
     */
    public LiveData<List<Impegno>> getImpegniInRange(long startDate, long endDate) {
        return Transformations.map(
                impegnoDao.getImpegniInRange(startDate, endDate),
                ImpegnoMapper::toDomainList
        );
    }

    /**
     * Inserisce un nuovo impegno
     */
    public void insertImpegno(Impegno impegno, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            try {
                ImpegnoEntity entity = ImpegnoMapper.toEntity(impegno);
                long id = impegnoDao.insert(entity);
                impegno.setId((int) id);
                WorkManagerHelper.scheduleImpegnoReminder(context, impegno);
                if (listener != null) {
                    listener.onSuccess((int) id);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Aggiorna un impegno esistente
     */
    public void updateImpegno(Impegno impegno, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            try {
                ImpegnoEntity entity = ImpegnoMapper.toEntity(impegno);
                impegnoDao.update(entity);
                WorkManagerHelper.scheduleImpegnoReminder(context, impegno);
                if (listener != null) {
                    listener.onSuccess(impegno.getId());
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Elimina un impegno
     */
    public void deleteImpegno(Impegno impegno, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            try {
                ImpegnoEntity entity = ImpegnoMapper.toEntity(impegno);
                impegnoDao.delete(entity);
                WorkManagerHelper.cancelImpegnoReminder(context, impegno.getId());
                if (listener != null) {
                    listener.onSuccess(impegno.getId());
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Segna un impegno come completato
     */
    public void markAsCompletato(int id, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                impegnoDao.markAsCompletato(id, now);
                WorkManagerHelper.cancelImpegnoReminder(context, id);
                if (listener != null) {
                    listener.onSuccess(id);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Segna un impegno come non completato
     */
    public void markAsNonCompletato(int id, OnOperationCompleteListener listener) {
        executorService.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                impegnoDao.markAsNonCompletato(id, now);
                ImpegnoEntity entity = impegnoDao.getImpegnoByIdSync(id);
                if (entity != null) {
                    WorkManagerHelper.scheduleImpegnoReminder(context, ImpegnoMapper.toDomain(entity));
                }
                if (listener != null) {
                    listener.onSuccess(id);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Riallinea la schedulazione dei promemoria per tutti gli impegni futuri
     * con reminder attivo. Idempotente (le richieste usano REPLACE): viene
     * chiamata all'avvio dell'app per coprire impegni salvati prima
     * dell'introduzione dei promemoria o schedulazioni perse.
     */
    public void rescheduleAllReminders() {
        executorService.execute(() -> {
            try {
                List<ImpegnoEntity> entities = impegnoDao.getAllImpegniSync();
                for (ImpegnoEntity entity : entities) {
                    if (entity.isReminderEnabled() && !entity.isCompletato()) {
                        WorkManagerHelper.scheduleImpegnoReminder(context, ImpegnoMapper.toDomain(entity));
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ImpegnoRepository", "Errore nel riallineare i promemoria", e);
            }
        });
    }

    /**
     * Callback per operazioni asincrone
     */
    public interface OnOperationCompleteListener {
        void onSuccess(int id);
        void onError(String error);
    }
}
