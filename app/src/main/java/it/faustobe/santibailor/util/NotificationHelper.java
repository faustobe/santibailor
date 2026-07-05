package it.faustobe.santibailor.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import it.faustobe.santibailor.R;
import it.faustobe.santibailor.presentation.features.main.MainActivity;

/**
 * Helper class per la gestione delle notifiche dell'app
 */
public class NotificationHelper {

    private static final String CHANNEL_ID_DAILY_SAINT = "daily_saint_channel";
    private static final String CHANNEL_NAME_DAILY_SAINT = "Santo del Giorno";
    private static final String CHANNEL_DESC_DAILY_SAINT = "Notifica giornaliera con il santo del giorno";

    private static final String CHANNEL_ID_IMPEGNO_REMINDER = "impegno_reminder_channel";
    private static final String CHANNEL_NAME_IMPEGNO_REMINDER = "Promemoria Impegni";
    private static final String CHANNEL_DESC_IMPEGNO_REMINDER = "Promemoria per gli impegni in scadenza";

    public static final int NOTIFICATION_ID_DAILY_SAINT = 1001;
    // Gli id delle notifiche impegno sono derivati dall'id dell'impegno per non sovrapporsi
    private static final int NOTIFICATION_ID_IMPEGNO_BASE = 2000;

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(this.context);
        createNotificationChannels();
    }

    /**
     * Crea i canali di notifica (richiesto per Android 8.0+)
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canale per il Santo del Giorno
            NotificationChannel dailySaintChannel = new NotificationChannel(
                    CHANNEL_ID_DAILY_SAINT,
                    CHANNEL_NAME_DAILY_SAINT,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            dailySaintChannel.setDescription(CHANNEL_DESC_DAILY_SAINT);
            dailySaintChannel.enableVibration(true);
            dailySaintChannel.setShowBadge(true);

            // Canale per i promemoria degli impegni
            NotificationChannel impegnoReminderChannel = new NotificationChannel(
                    CHANNEL_ID_IMPEGNO_REMINDER,
                    CHANNEL_NAME_IMPEGNO_REMINDER,
                    NotificationManager.IMPORTANCE_HIGH
            );
            impegnoReminderChannel.setDescription(CHANNEL_DESC_IMPEGNO_REMINDER);
            impegnoReminderChannel.enableVibration(true);
            impegnoReminderChannel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(dailySaintChannel);
                manager.createNotificationChannel(impegnoReminderChannel);
            }
        }
    }

    /**
     * Mostra notifica con il santo del giorno
     *
     * @param saintName Nome del santo
     * @param saintDescription Breve descrizione (opzionale)
     */
    public void showDailySaintNotification(String saintName, String saintDescription) {
        // Intent per aprire l'app quando si clicca sulla notifica
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Costruisci la notifica
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DAILY_SAINT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Santo del Giorno")
                .setContentText(saintName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Aggiungi descrizione se presente
        if (saintDescription != null && !saintDescription.isEmpty()) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(saintName + "\n\n" + saintDescription));
        }

        // Mostra la notifica
        try {
            notificationManager.notify(NOTIFICATION_ID_DAILY_SAINT, builder.build());
        } catch (SecurityException e) {
            // Permessi notifiche non concessi (Android 13+)
            android.util.Log.e("NotificationHelper", "Permesso notifiche non concesso", e);
        }
    }

    /**
     * Cancella la notifica del santo del giorno
     */
    public void cancelDailySaintNotification() {
        notificationManager.cancel(NOTIFICATION_ID_DAILY_SAINT);
    }

    /**
     * Mostra il promemoria per un impegno
     *
     * @param impegnoId Id dell'impegno (usato per derivare l'id della notifica)
     * @param titolo Titolo dell'impegno
     * @param testo Testo della notifica (es. orario dell'impegno)
     * @param dettaglio Testo esteso opzionale (es. descrizione dell'impegno)
     */
    public void showImpegnoReminderNotification(int impegnoId, String titolo, String testo, String dettaglio) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_IMPEGNO_BASE + impegnoId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_IMPEGNO_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.impegno_reminder_notification_title))
                .setContentText(titolo + (testo != null && !testo.isEmpty() ? " — " + testo : ""))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (dettaglio != null && !dettaglio.isEmpty()) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(titolo + (testo != null && !testo.isEmpty() ? " — " + testo : "") + "\n\n" + dettaglio));
        }

        try {
            notificationManager.notify(NOTIFICATION_ID_IMPEGNO_BASE + impegnoId, builder.build());
        } catch (SecurityException e) {
            android.util.Log.e("NotificationHelper", "Permesso notifiche non concesso", e);
        }
    }

    /**
     * Cancella il promemoria di un impegno (se ancora visibile)
     */
    public void cancelImpegnoReminderNotification(int impegnoId) {
        notificationManager.cancel(NOTIFICATION_ID_IMPEGNO_BASE + impegnoId);
    }

    /**
     * Verifica se le notifiche sono abilitate
     *
     * @return true se le notifiche sono abilitate
     */
    public boolean areNotificationsEnabled() {
        return notificationManager.areNotificationsEnabled();
    }

    /**
     * Verifica se il canale specifico è abilitato (Android 8.0+)
     *
     * @param channelId ID del canale
     * @return true se il canale è abilitato
     */
    public boolean isChannelEnabled(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
        }
        return areNotificationsEnabled();
    }
}
