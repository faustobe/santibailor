package it.faustobe.santibailor.presentation.features.organizza;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import it.faustobe.santibailor.R;
import it.faustobe.santibailor.data.local.dao.ImpegnoDao;
import it.faustobe.santibailor.data.local.entities.ImpegnoEntity;
import it.faustobe.santibailor.domain.model.Priorita;

@AndroidEntryPoint
public class OrganizzaGiornataFragment extends Fragment {

    @Inject
    ImpegnoDao impegnoDao;

    private EditText[] etTitoli;
    private TextView[] tvOre;
    private TextView tvDataSelezionata;
    private MaterialButton btnSalva;
    private ImageButton btnBack;

    private Calendar selectedDate;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_organizza_giornata, container, false);

        etTitoli = new EditText[]{
            view.findViewById(R.id.et_impegno_1),
            view.findViewById(R.id.et_impegno_2),
            view.findViewById(R.id.et_impegno_3),
            view.findViewById(R.id.et_impegno_4),
            view.findViewById(R.id.et_impegno_5)
        };

        tvOre = new TextView[]{
            view.findViewById(R.id.tv_ora_1),
            view.findViewById(R.id.tv_ora_2),
            view.findViewById(R.id.tv_ora_3),
            view.findViewById(R.id.tv_ora_4),
            view.findViewById(R.id.tv_ora_5)
        };

        tvDataSelezionata = view.findViewById(R.id.tv_data_selezionata);
        btnSalva = view.findViewById(R.id.btn_salva_impegni);
        btnBack = view.findViewById(R.id.btn_back);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedDate = Calendar.getInstance();

        setupDatePicker();
        setupTimePickers();

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        btnSalva.setOnClickListener(v -> salvaImpegni());
    }

    private void setupDatePicker() {
        aggiornaTestoData();
        tvDataSelezionata.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (picker, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    aggiornaTestoData();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialog.show();
        });
    }

    private void aggiornaTestoData() {
        Calendar oggi = Calendar.getInstance();
        boolean isOggi = selectedDate.get(Calendar.YEAR) == oggi.get(Calendar.YEAR)
                && selectedDate.get(Calendar.DAY_OF_YEAR) == oggi.get(Calendar.DAY_OF_YEAR);

        if (isOggi) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM", Locale.ITALIAN);
            String dataFormattata = sdf.format(selectedDate.getTime());
            tvDataSelezionata.setText(getString(R.string.organizza_oggi, dataFormattata));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.ITALIAN);
            tvDataSelezionata.setText(sdf.format(selectedDate.getTime()));
        }
    }

    private void setupTimePickers() {
        Calendar now = Calendar.getInstance();
        int minuti = now.get(Calendar.MINUTE);
        int arrotondato = ((minuti / 15) + 1) * 15;
        int oraDefault = now.get(Calendar.HOUR_OF_DAY);
        if (arrotondato >= 60) {
            arrotondato = 0;
            oraDefault = (oraDefault + 1) % 24;
        }

        String oraDefaultStr = String.format(Locale.ITALIAN, "%02d:%02d", oraDefault, arrotondato);

        for (int i = 0; i < tvOre.length; i++) {
            tvOre[i].setText(oraDefaultStr);
            final int index = i;
            tvOre[i].setOnClickListener(v -> mostraTimePicker(index));
        }
    }

    private void mostraTimePicker(int index) {
        String oraCorrente = tvOre[index].getText().toString();
        int ora = 9, min = 0;
        if (oraCorrente.contains(":")) {
            String[] parti = oraCorrente.split(":");
            try {
                ora = Integer.parseInt(parti[0]);
                min = Integer.parseInt(parti[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        TimePickerDialog dialog = new TimePickerDialog(
            requireContext(),
            (picker, hourOfDay, minute) -> {
                String oraStr = String.format(Locale.ITALIAN, "%02d:%02d", hourOfDay, minute);
                tvOre[index].setText(oraStr);
            },
            ora, min, true
        );
        dialog.show();
    }

    private void salvaImpegni() {
        List<ImpegnoEntity> entities = new ArrayList<>();

        for (int i = 0; i < etTitoli.length; i++) {
            String titolo = etTitoli[i].getText().toString().trim();
            if (!titolo.isEmpty()) {
                entities.add(creaEntity(titolo, tvOre[i].getText().toString()));
            }
        }

        if (entities.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.insert_at_least_one), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSalva.setEnabled(false);
        int count = entities.size();

        executor.execute(() -> {
            for (ImpegnoEntity entity : entities) {
                impegnoDao.insert(entity);
            }
            mainHandler.post(() -> {
                if (isAdded() && getView() != null) {
                    Toast.makeText(getContext(), getString(R.string.commitments_saved, count), Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                }
            });
        });
    }

    private ImpegnoEntity creaEntity(String titolo, String oraStr) {
        Calendar dataOra = (Calendar) selectedDate.clone();

        if (!oraStr.trim().isEmpty() && oraStr.contains(":")) {
            String[] parti = oraStr.split(":");
            try {
                dataOra.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parti[0]));
                dataOra.set(Calendar.MINUTE, Integer.parseInt(parti[1]));
            } catch (NumberFormatException ignored) {
            }
        }
        dataOra.set(Calendar.SECOND, 0);
        dataOra.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();
        return new ImpegnoEntity(
            titolo,
            "",
            dataOra.getTimeInMillis(),
            null,
            false,
            0,
            false,
            null,
            now,
            now,
            Priorita.MEDIA,
            null
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}
