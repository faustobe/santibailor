package it.faustobe.santibailor.presentation.features.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import it.faustobe.santibailor.data.repository.ImpegnoRepository;
import it.faustobe.santibailor.domain.model.Impegno;

/**
 * ViewModel per la schermata dei promemoria: espone gli impegni futuri
 * non completati che hanno un reminder attivo
 */
@HiltViewModel
public class NotificationsViewModel extends ViewModel {

    private static final int MAX_PROMEMORIA = 100;

    private final ImpegnoRepository repository;

    @Inject
    public NotificationsViewModel(ImpegnoRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Impegno>> getPromemoriaProgrammati() {
        return Transformations.map(repository.getImpegniFuturi(MAX_PROMEMORIA), impegni -> {
            List<Impegno> conReminder = new ArrayList<>();
            if (impegni != null) {
                for (Impegno impegno : impegni) {
                    if (impegno.isReminderEnabled()) {
                        conReminder.add(impegno);
                    }
                }
            }
            return conReminder;
        });
    }
}
