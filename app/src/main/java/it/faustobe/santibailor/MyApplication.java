package it.faustobe.santibailor;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import dagger.hilt.android.HiltAndroidApp;
import it.faustobe.santibailor.data.repository.ImpegnoRepository;
import it.faustobe.santibailor.util.FirebaseErrorHandler;
import it.faustobe.santibailor.util.ImageHandler;
import it.faustobe.santibailor.util.LanguageManager;
import it.faustobe.santibailor.util.ThemeManager;
import it.faustobe.santibailor.util.WorkManagerHelper;

import javax.inject.Inject;

@HiltAndroidApp
public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Inject
    ImageHandler imageHandler;

    @Inject
    HiltWorkerFactory workerFactory;

    @Inject
    ImpegnoRepository impegnoRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        // Applica tema e lingua salvati
        ThemeManager.applyTheme(this);
        LanguageManager.applyLanguage(this);

        // Inizializza manualmente WorkManager (dato che è disabilitato in AndroidManifest).
        // La HiltWorkerFactory è indispensabile: i worker @HiltWorker non hanno il
        // costruttore (Context, WorkerParameters) richiesto dalla factory di default.
        try {
            Configuration config = new Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build();
            WorkManager.initialize(this, config);
            Log.d(TAG, "WorkManager initialized manually");
        } catch (IllegalStateException e) {
            // WorkManager già inizializzato
            Log.d(TAG, "WorkManager already initialized");
        }

        FirebaseApp.initializeApp(this);

        // Inizializza Firebase App Check con Play Integrity
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        // Autenticazione anonima
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Autenticazione anonima completata con successo");
                    } else {
                        handleAuthenticationError(task.getException());
                    }
                });

        // Schedula la notifica giornaliera solo se abilitata nelle preferenze
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean saintNotificationsEnabled = prefs.getBoolean("saint_notifications_enabled", true);
        if (notificationsEnabled && saintNotificationsEnabled) {
            int hour = prefs.getInt("saint_notification_hour", 7);
            int minute = prefs.getInt("saint_notification_minute", 0);
            WorkManagerHelper.scheduleDailySaintNotification(this, hour, minute);
            Log.d(TAG, "WorkManager: Daily saint notification scheduled at " + hour + ":" + minute);
        } else {
            Log.d(TAG, "WorkManager: Daily saint notification disabled by user preference");
        }

        // Riallinea i promemoria degli impegni (idempotente, gira in background)
        impegnoRepository.rescheduleAllReminders();
    }

    private void handleAuthenticationError(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            Log.e(TAG, "Autenticazione fallita: " + authException.getErrorCode()
                    + " - " + authException.getMessage(), authException);
        } else {
            Log.e(TAG, "Autenticazione fallita con eccezione non specifica", exception);
        }
        // Qui potresti aggiungere ulteriori azioni in caso di fallimento dell'autenticazione
    }

    public ImageHandler getImageHandler() {
        return imageHandler;
    }
}