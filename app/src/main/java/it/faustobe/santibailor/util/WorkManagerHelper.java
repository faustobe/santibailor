package it.faustobe.santibailor.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import it.faustobe.santibailor.domain.model.Impegno;
import it.faustobe.santibailor.worker.DailySaintNotificationWorker;
import it.faustobe.santibailor.worker.ImpegnoReminderWorker;

/**
 * Helper class per la gestione di WorkManager e task periodici
 */
public class WorkManagerHelper {

    private static final String DAILY_SAINT_WORK_NAME = "daily_saint_notification_work";
    private static final String IMPEGNO_REMINDER_WORK_PREFIX = "impegno_reminder_";
    private static final int NOTIFICATION_HOUR = 7; // Ora della notifica (7:00 AM)
    private static final int NOTIFICATION_MINUTE = 0;

    /**
     * Programma la notifica giornaliera del santo del giorno (default 7:00)
     *
     * @param context Context dell'applicazione
     */
    public static void scheduleDailySaintNotification(Context context) {
        scheduleDailySaintNotification(context, NOTIFICATION_HOUR, NOTIFICATION_MINUTE);
    }

    /**
     * Programma la notifica giornaliera del santo del giorno con orario personalizzato
     *
     * @param context Context dell'applicazione
     * @param hour Ora della notifica (0-23)
     * @param minute Minuto della notifica (0-59)
     */
    public static void scheduleDailySaintNotification(Context context, int hour, int minute) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build();

        long initialDelay = calculateInitialDelay(hour, minute);

        PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
                DailySaintNotificationWorker.class,
                1,
                TimeUnit.DAYS
        )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(DAILY_SAINT_WORK_NAME)
                .build();

        // Usa REPLACE per aggiornare l'orario quando l'utente lo cambia
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_SAINT_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest
        );

        android.util.Log.d("WorkManagerHelper", "Daily saint notification scheduled at " + hour + ":" + minute + " with initial delay: " + initialDelay + "ms");
    }

    /**
     * Calcola il delay iniziale per l'orario di default
     */
    private static long calculateInitialDelay() {
        return calculateInitialDelay(NOTIFICATION_HOUR, NOTIFICATION_MINUTE);
    }

    /**
     * Calcola il delay iniziale per eseguire il worker all'ora specificata
     *
     * @param hour Ora della notifica (0-23)
     * @param minute Minuto della notifica (0-59)
     * @return delay in millisecondi
     */
    private static long calculateInitialDelay(int hour, int minute) {
        Calendar currentTime = Calendar.getInstance();
        Calendar scheduledTime = Calendar.getInstance();

        scheduledTime.set(Calendar.HOUR_OF_DAY, hour);
        scheduledTime.set(Calendar.MINUTE, minute);
        scheduledTime.set(Calendar.SECOND, 0);
        scheduledTime.set(Calendar.MILLISECOND, 0);

        if (scheduledTime.before(currentTime)) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return scheduledTime.getTimeInMillis() - currentTime.getTimeInMillis();
    }

    /**
     * Cancella la schedulazione della notifica giornaliera
     *
     * @param context Context dell'applicazione
     */
    public static void cancelDailySaintNotification(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_SAINT_WORK_NAME);
        android.util.Log.d("WorkManagerHelper", "Daily saint notification cancelled");
    }

    /**
     * Verifica se la notifica giornaliera è schedulata
     * (Metodo asincrono, usare ListenableFuture per ottenere il risultato)
     *
     * @param context Context dell'applicazione
     */
    public static void isDailySaintNotificationScheduled(Context context, OnWorkStatusCheckedListener listener) {
        WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(DAILY_SAINT_WORK_NAME)
                .addListener(() -> {
                    // Callback eseguito quando lo stato è disponibile
                    if (listener != null) {
                        listener.onStatusChecked(true);
                    }
                }, command -> command.run());
    }

    public interface OnWorkStatusCheckedListener {
        void onStatusChecked(boolean isScheduled);
    }

    /**
     * Programma (o riprogramma) il promemoria per un impegno.
     * Se il reminder è disabilitato, l'impegno è completato o l'orario del
     * promemoria è già passato, l'eventuale schedulazione esistente viene cancellata.
     *
     * @param context Context dell'applicazione
     * @param impegno Impegno con id valorizzato
     */
    public static void scheduleImpegnoReminder(Context context, Impegno impegno) {
        String workName = IMPEGNO_REMINDER_WORK_PREFIX + impegno.getId();
        WorkManager workManager = WorkManager.getInstance(context);

        if (!impegno.isReminderEnabled() || impegno.isCompletato()) {
            workManager.cancelUniqueWork(workName);
            return;
        }

        long triggerAt = impegno.getDataOra() - impegno.getReminderMinutesBefore() * 60_000L;
        long delay = triggerAt - System.currentTimeMillis();
        if (delay <= 0) {
            workManager.cancelUniqueWork(workName);
            android.util.Log.d("WorkManagerHelper", "Reminder impegno " + impegno.getId() + " nel passato, non schedulato");
            return;
        }

        Data inputData = new Data.Builder()
                .putInt(ImpegnoReminderWorker.KEY_IMPEGNO_ID, impegno.getId())
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ImpegnoReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(IMPEGNO_REMINDER_WORK_PREFIX)
                .build();

        // REPLACE: al salvataggio dell'impegno il promemoria segue sempre i dati più recenti
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request);
        android.util.Log.d("WorkManagerHelper", "Reminder impegno " + impegno.getId() + " schedulato tra " + delay + "ms");
    }

    /**
     * Cancella il promemoria schedulato per un impegno
     *
     * @param context Context dell'applicazione
     * @param impegnoId Id dell'impegno
     */
    public static void cancelImpegnoReminder(Context context, int impegnoId) {
        WorkManager.getInstance(context).cancelUniqueWork(IMPEGNO_REMINDER_WORK_PREFIX + impegnoId);
        android.util.Log.d("WorkManagerHelper", "Reminder impegno " + impegnoId + " cancellato");
    }

    /**
     * Cancella tutti i promemoria impegni schedulati (usato dal ripristino backup)
     */
    public static void cancelAllImpegnoReminders(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(IMPEGNO_REMINDER_WORK_PREFIX);
        android.util.Log.d("WorkManagerHelper", "Tutti i reminder impegni cancellati");
    }
}
