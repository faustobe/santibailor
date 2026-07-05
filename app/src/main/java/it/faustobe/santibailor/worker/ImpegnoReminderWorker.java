package it.faustobe.santibailor.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import it.faustobe.santibailor.data.local.dao.ImpegnoDao;
import it.faustobe.santibailor.data.local.entities.ImpegnoEntity;
import it.faustobe.santibailor.util.NotificationHelper;

/**
 * Worker che mostra la notifica di promemoria per un singolo impegno.
 * Viene schedulato da WorkManagerHelper al salvataggio dell'impegno
 * (orario impegno - minuti di anticipo scelti dall'utente).
 */
@HiltWorker
public class ImpegnoReminderWorker extends Worker {

    public static final String KEY_IMPEGNO_ID = "impegno_id";

    private static final String TAG = "ImpegnoReminderWorker";

    private final ImpegnoDao impegnoDao;

    @AssistedInject
    public ImpegnoReminderWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters workerParams,
            ImpegnoDao impegnoDao
    ) {
        super(context, workerParams);
        this.impegnoDao = impegnoDao;
    }

    @NonNull
    @Override
    public Result doWork() {
        int impegnoId = getInputData().getInt(KEY_IMPEGNO_ID, -1);
        if (impegnoId <= 0) {
            Log.w(TAG, "Id impegno mancante nell'input data");
            return Result.failure();
        }

        try {
            // Le preferenze vengono verificate al momento del fire, così il toggle
            // nelle impostazioni ha effetto anche sui promemoria già schedulati
            SharedPreferences prefs = getApplicationContext()
                    .getSharedPreferences("settings", Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
            boolean impegniNotificationsEnabled = prefs.getBoolean("impegni_notifications_enabled", true);
            if (!notificationsEnabled || !impegniNotificationsEnabled) {
                Log.d(TAG, "Promemoria impegni disabilitati dalle preferenze, notifica saltata");
                return Result.success();
            }

            ImpegnoEntity impegno = impegnoDao.getImpegnoByIdSync(impegnoId);
            if (impegno == null || impegno.isCompletato() || !impegno.isReminderEnabled()) {
                Log.d(TAG, "Impegno " + impegnoId + " eliminato/completato/senza reminder, notifica saltata");
                return Result.success();
            }

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String orario = timeFormat.format(new Date(impegno.getDataOra()));

            NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
            notificationHelper.showImpegnoReminderNotification(
                    impegnoId,
                    impegno.getTitolo(),
                    orario,
                    impegno.getDescrizione()
            );

            Log.d(TAG, "Promemoria inviato per impegno " + impegnoId + " (" + impegno.getTitolo() + ")");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Errore nell'invio del promemoria per impegno " + impegnoId, e);
            return Result.retry();
        }
    }
}
