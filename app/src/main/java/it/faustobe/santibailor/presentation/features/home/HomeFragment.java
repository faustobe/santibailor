package it.faustobe.santibailor.presentation.features.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.FirebaseStorage;

import dagger.hilt.android.AndroidEntryPoint;
import it.faustobe.santibailor.domain.model.Impegno;
import it.faustobe.santibailor.domain.model.Ricorrenza;
import it.faustobe.santibailor.domain.model.TipoRicorrenza;
import it.faustobe.santibailor.presentation.common.ricorrenze.RicorrenzaAdapter;
import it.faustobe.santibailor.presentation.features.main.MainActivity;
import it.faustobe.santibailor.R;
import it.faustobe.santibailor.databinding.FragmentHomeBinding;
import it.faustobe.santibailor.util.BackgroundManager;
import it.faustobe.santibailor.util.DateUtils;
import it.faustobe.santibailor.presentation.common.viewmodels.RicorrenzaViewModel;
import it.faustobe.santibailor.presentation.features.impegni.ImpegniViewModel;
import it.faustobe.santibailor.presentation.features.listespesa.ListeSpesaViewModel;
import it.faustobe.santibailor.domain.model.ListaSpesa;
import it.faustobe.santibailor.domain.model.OverviewItem;
import it.faustobe.santibailor.domain.model.Nota;
import it.faustobe.santibailor.presentation.features.note.NoteViewModel;
import it.faustobe.santibailor.util.ImageHandler;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private RicorrenzaViewModel ricorrenzaViewModel;
    private ImpegniViewModel impegniViewModel;
    private ListeSpesaViewModel listeSpesaViewModel;
    private boolean isInitialized = false;
    private RicorrenzaAdapter ricorrenzaAdapter;
    private RicorrenzaAdapter eventiLaiciAdapter;
    private OverviewAdapter overviewAdapter;
    private NoteViewModel noteViewModel;
    private boolean isSaintsListExpanded = false;
    private boolean isCalendarExpanded; // Inizializzato in setupCalendar() leggendo lo stato salvato
    private HomeViewModel homeViewModel;
    private static final String TAG = "HomeFragment";
    private NestedScrollView nestedScrollView;
    private ImageHandler imageHandler;
    private MotionLayout motionLayout;
    private Handler collapseHandler = new Handler(Looper.getMainLooper());
    private static final long COLLAPSE_DELAY = 5000; // 5 secondi
    private String currentNavSection = "santi"; // sezione corrente del nav bar
    private SharedPreferences calendarPrefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inizializza SharedPreferences per salvare lo stato del calendario
        calendarPrefs = requireContext().getSharedPreferences("calendar_state", Context.MODE_PRIVATE);

        // Usa questo metodo per ottenere il ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        ricorrenzaViewModel = new ViewModelProvider(requireActivity()).get(RicorrenzaViewModel.class);
        impegniViewModel = new ViewModelProvider(this).get(ImpegniViewModel.class);
        listeSpesaViewModel = new ViewModelProvider(this).get(ListeSpesaViewModel.class);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        // Inizializza ImageMigrationService solo se ricorrenzaViewModel è disponibile
        if (ricorrenzaViewModel != null && ricorrenzaViewModel.getRepository() != null) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            imageHandler = ImageHandler.getInstance(requireContext());
        } else {
            Log.e("HomeFragment", "RicorrenzaViewModel o il suo repo è null");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        Log.d(TAG, "onCreateView: Vista creata");

        motionLayout = binding.motionLayout;
        if (motionLayout == null) {
            Log.e(TAG, "onCreateView: MotionLayout non trovato nel binding");
        } else {
            Log.d(TAG, "onCreateView: MotionLayout trovato e inizializzato");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("HomeFragment", "onViewCreated: Inizializzazione della vista");

        ricorrenzaViewModel = new ViewModelProvider(requireActivity()).get(RicorrenzaViewModel.class);

        if (ricorrenzaAdapter == null) {
            ricorrenzaAdapter = new RicorrenzaAdapter(this::navigateToRicorrenzaDetail, ricorrenzaViewModel);
        }
        // IMPORTANTE: Ri-assegna l'adapter ogni volta che la view viene ricreata
        binding.recyclerViewSaints.setAdapter(ricorrenzaAdapter);
        binding.recyclerViewSaints.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inizializza adapter per Eventi Laici Generali
        if (eventiLaiciAdapter == null) {
            eventiLaiciAdapter = new RicorrenzaAdapter(this::navigateToRicorrenzaDetail, ricorrenzaViewModel);
        }
        // IMPORTANTE: Ri-assegna l'adapter ogni volta che la view viene ricreata
        binding.recyclerViewEventiLaici.setAdapter(eventiLaiciAdapter);
        binding.recyclerViewEventiLaici.setLayoutManager(new LinearLayoutManager(getContext()));

        // Osserva le ricorrenze del giorno
        ricorrenzaViewModel.getRicorrenzeDelGiorno().observe(getViewLifecycleOwner(), this::updateRicorrenzeList);

        // Osserva il santo del giorno
        ricorrenzaViewModel.getCurrentSaint().observe(getViewLifecycleOwner(), this::updateSaintOfDay);

        if (!isInitialized) {
            ricorrenzaViewModel.loadRicorrenzeForCurrentDate();
            isInitialized = true;
        }

        // Imposta il listener per il pulsante di ricarica del santo
        binding.reloadSaintButton.setOnClickListener(v -> {
            Log.d("HomeFragment", "Refresh button clicked");
            reloadSaintOfDay();
        });

        ricorrenzaViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                // Mostra un indicatore di caricamento
            } else {
                // Nascondi l'indicatore di caricamento
            }
        });

        nestedScrollView = binding.nestedScrollView;

        setupScrollListener();

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateComponentsState();
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        setupDateDisplay();
        loadBackgroundImage();
        setupSaintsList();
        updateComponentsState();
        updateInitialVisibility();
        setInitialSaintsListState();
        observeRicorrenze();
        setupCalendar();
        setupCalendarCollapse();
        setupOverview();
        setupQuickActions();
    }

    private void showSection(String section) {
        currentNavSection = section;

        // Scroll alla sezione selezionata
        View targetView = null;
        switch (section) {
            case "santi":
                targetView = binding.saintCard;
                break;
            case "eventi":
                targetView = binding.cardEventiLaici;
                break;
        }

        if (targetView != null) {
            final View finalView = targetView;
            binding.nestedScrollView.post(() -> {
                binding.nestedScrollView.smoothScrollTo(0, finalView.getTop());
            });
        }
    }

    private void setupCalendar() {
        MotionLayout motionLayout = binding.motionLayout;

        // Ripristina lo stato salvato del calendario dopo che il layout è pronto
        // Usa jumpToState() per impostare lo stato senza animazione
        motionLayout.post(() -> {
            // Controlla se esiste uno stato salvato
            if (calendarPrefs.contains("is_expanded")) {
                // Usa lo stato salvato
                isCalendarExpanded = calendarPrefs.getBoolean("is_expanded", true);
                Log.d(TAG, "Stato SALVATO trovato: " + (isCalendarExpanded ? "espanso" : "collassato"));
            } else {
                // Prima apertura: default ESPANSO
                isCalendarExpanded = true;
                Log.d(TAG, "Prima apertura: imposto ESPANSO");
            }

            if (isCalendarExpanded) {
                motionLayout.jumpToState(R.id.expanded);
                // Aggiorna le dimensioni del testo per lo stato espanso (progress = 0.0)
                updateCalendarTextSize(0.0f);
            } else {
                motionLayout.jumpToState(R.id.collapsed);
                // Aggiorna le dimensioni del testo per lo stato collassato (progress = 1.0)
                updateCalendarTextSize(1.0f);
            }

            // Forza l'aggiornamento della visibilità dei pulsanti
            updateQuickActionsVisibility();
            Log.d(TAG, "Calendario ripristinato: " + (isCalendarExpanded ? "espanso" : "collassato"));
        });
    }

    private void showAllSections() {
        // Mostra tutte le card quando il calendario è espanso
        // (card_eventi_laici è gestita da updateEventiLaiciGenerali())
        binding.saintCard.setVisibility(View.VISIBLE);
    }

    private void updateCalendarTextSize(float progress) {
        float expandedWeekdaySize = getResources().getDimension(R.dimen.calendar_expanded_weekday_text_size);
        float collapsedWeekdaySize = getResources().getDimension(R.dimen.calendar_collapsed_weekday_text_size);
        float expandedDaySize = getResources().getDimension(R.dimen.calendar_expanded_day_text_size);
        float collapsedDaySize = getResources().getDimension(R.dimen.calendar_collapsed_day_text_size);
        float expandedMonthSize = getResources().getDimension(R.dimen.calendar_expanded_month_text_size);
        float collapsedMonthSize = getResources().getDimension(R.dimen.calendar_collapsed_month_text_size);

        float weekdaySize = expandedWeekdaySize + (collapsedWeekdaySize - expandedWeekdaySize) * progress;
        float daySize = expandedDaySize + (collapsedDaySize - expandedDaySize) * progress;
        float monthSize = expandedMonthSize + (collapsedMonthSize - expandedMonthSize) * progress;

        binding.tvWeekday.setTextSize(TypedValue.COMPLEX_UNIT_PX, weekdaySize);
        binding.tvDay.setTextSize(TypedValue.COMPLEX_UNIT_PX, daySize);
        binding.tvMonth.setTextSize(TypedValue.COMPLEX_UNIT_PX, monthSize);

        // Gestione dell'alpha
        float alpha = 1f - (0.3f * progress);
        binding.tvWeekday.setAlpha(alpha);
        binding.tvMonth.setAlpha(alpha);
    }

    private void scheduleCalendarCollapse() {
        collapseHandler.removeCallbacksAndMessages(null);
        collapseHandler.postDelayed(() -> {
            if (isAdded() && binding != null && isCalendarExpanded) {
                binding.motionLayout.transitionToEnd();
                isCalendarExpanded = false;
            }
        }, COLLAPSE_DELAY);
    }

    private void setupCalendarCollapse() {
        Log.d(TAG, "setupCalendarCollapse: Configurazione del collasso del calendario");

        if (motionLayout == null) {
            Log.e(TAG, "setupCalendarCollapse: MotionLayout è null");
            return;
        }

        motionLayout.setTransitionListener(new MotionLayout.TransitionListener() {
            @Override
            public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
                Log.d(TAG, "onTransitionStarted: Transizione iniziata da " + startId + " a " + endId);
            }

            @Override
            public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
                Log.d(TAG, "onTransitionChange: Progresso della transizione: " + progress);
                updateCalendarTextSize(progress);
            }

            @Override
            public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                Log.d(TAG, "onTransitionCompleted: Transizione completata. Stato corrente: " + currentId);
                isCalendarExpanded = (currentId == R.id.expanded);

                // Salva lo stato
                calendarPrefs.edit().putBoolean("is_expanded", isCalendarExpanded).apply();
                Log.d(TAG, "Stato salvato: " + (isCalendarExpanded ? "ESPANSO" : "COLLASSATO"));
            }

            @Override
            public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
                Log.d(TAG, "onTransitionTrigger: Trigger " + triggerId + " attivato");
            }
        });

        binding.calendarCard.setOnClickListener(v -> {
            Log.d(TAG, "Calendario cliccato");
            if (motionLayout.getCurrentState() == R.id.collapsed) {
                Log.d(TAG, "Tentativo di espandere il calendario");
                motionLayout.transitionToState(R.id.expanded);
            } else {
                Log.d(TAG, "Tentativo di collassare il calendario");
                motionLayout.transitionToState(R.id.collapsed);
            }
        });
    }

    private void observeRicorrenze() {
        // Osserva il Santo del Giorno (selezionato casualmente)
        ricorrenzaViewModel.getCurrentSaint().observe(getViewLifecycleOwner(), this::updateSaintOfDay);

        // Osserva le ricorrenze religiose (Santi) separatamente dalle laiche
        ricorrenzaViewModel.getRicorrenzeReligiose().observe(getViewLifecycleOwner(), this::updateRicorrenzeReligiose);
        ricorrenzaViewModel.getRicorrenzeLaiche().observe(getViewLifecycleOwner(), this::updateRicorrenzeLaiche);

        // Osserva gli impegni di oggi - RIMOSSO, ora gestito in setupOverview()
        // impegniViewModel.getImpegniOggi().observe(getViewLifecycleOwner(), this::updateImpegniOggi);
    }

    private void updateRicorrenze(List<Ricorrenza> ricorrenze) {
        if (ricorrenze == null) return;
        Log.d("HomeFragment", "updateRicorrenze chiamato con " + ricorrenze.size() + " ricorrenze");
        updateRicorrenzeList(ricorrenze);
    }

    private void updateRicorrenzeList(List<Ricorrenza> ricorrenze) {
        if (ricorrenze != null && !ricorrenze.isEmpty()) {
            if (ricorrenze.size() > 1) {
                binding.expandCollapseSaintsIcon.setVisibility(View.VISIBLE);
                ricorrenzaAdapter.setRicorrenze(ricorrenze.subList(1, ricorrenze.size()));
            } else {
                binding.expandCollapseSaintsIcon.setVisibility(View.GONE);
                binding.recyclerViewSaints.setVisibility(View.GONE);
            }
        } else {
            binding.expandCollapseSaintsIcon.setVisibility(View.GONE);
            binding.recyclerViewSaints.setVisibility(View.GONE);
        }
        updateComponentsState();
    }

    private void updateSaintOfDay(Ricorrenza saint) {
        if (saint == null || binding == null || !isAdded()) return;
        Log.d("HomeFragment", "Updated saint of day: " + saint.getPrefix() + " " + saint.getNome());
        String saintText = saint.getPrefix() + " " + saint.getNome();
        binding.tvSaintOfDay.setText(saintText);
        binding.tvSaintOfDay.setOnClickListener(v -> navigateToRicorrenzaDetail(saint.getId()));
    }

    private void updateRicorrenzeReligiose(List<Ricorrenza> ricorrenze) {
        if (binding == null || !isAdded()) return;
        Log.d("HomeFragment", "Ricorrenze religiose (Santi): " + (ricorrenze != null ? ricorrenze.size() : 0));
        if (ricorrenze == null || ricorrenze.isEmpty()) {
            binding.expandCollapseSaintsIcon.setVisibility(View.GONE);
            binding.recyclerViewSaints.setVisibility(View.GONE);
            binding.tvSaintOfDay.setText(R.string.no_saints_today);
            return;
        }

        // Ottieni il santo correntemente mostrato come "Santo del Giorno"
        Ricorrenza currentSaint = ricorrenzaViewModel.getCurrentSaint().getValue();

        // Filtra la lista per ESCLUDERE il santo già mostrato come "Santo del Giorno"
        List<Ricorrenza> filteredList = new java.util.ArrayList<>();
        for (Ricorrenza ricorrenza : ricorrenze) {
            if (currentSaint == null || ricorrenza.getId() != currentSaint.getId()) {
                filteredList.add(ricorrenza);
            }
        }

        // Mostra TUTTI gli altri santi nella lista espandibile (escludendo quello già mostrato sopra)
        if (!filteredList.isEmpty()) {
            binding.expandCollapseSaintsIcon.setVisibility(View.VISIBLE);
            ricorrenzaAdapter.setRicorrenze(filteredList);
            Log.d("HomeFragment", "Showing " + filteredList.size() + " saints in expandable list (excluding current saint)");
        } else {
            binding.expandCollapseSaintsIcon.setVisibility(View.GONE);
            binding.recyclerViewSaints.setVisibility(View.GONE);
        }

        updateComponentsState();
    }

    private void updateRicorrenzeLaiche(List<Ricorrenza> ricorrenze) {
        if (binding == null || !isAdded()) return;
        Log.d("HomeFragment", "Ricorrenze laiche/personali: " + (ricorrenze != null ? ricorrenze.size() : 0));
        if (ricorrenze == null || ricorrenze.isEmpty()) {
            updateEventiLaiciGenerali(new java.util.ArrayList<>());
            return;
        }

        // Separa eventi laici generali da personali
        // Criterio: SOLO il tipo di ricorrenza (non l'URL dell'immagine)
        List<Ricorrenza> eventiGenerali = new java.util.ArrayList<>();
        List<Ricorrenza> eventiPersonali = new java.util.ArrayList<>();

        for (Ricorrenza ric : ricorrenze) {
            if (ric.getTipoRicorrenzaId() == it.faustobe.santibailor.domain.model.TipoRicorrenza.PERSONALE) {
                eventiPersonali.add(ric);
            } else {
                eventiGenerali.add(ric);
            }
        }

        Log.d("HomeFragment", "Eventi laici generali: " + eventiGenerali.size() + ", personali: " + eventiPersonali.size());
        updateEventiLaiciGenerali(eventiGenerali);
    }

    private void updateEventiLaiciGenerali(List<Ricorrenza> eventi) {
        if (binding == null || !isAdded()) return;
        Log.d("HomeFragment", "Eventi laici generali: " + eventi.size());
        if (eventi == null || eventi.isEmpty()) {
            Log.d("HomeFragment", "Setting card_eventi_laici to GONE (empty list)");
            binding.cardEventiLaici.setVisibility(View.GONE);
            return;
        }

        Log.d("HomeFragment", "Setting card_eventi_laici to VISIBLE");
        binding.cardEventiLaici.setVisibility(View.VISIBLE);
        binding.cardEventiLaici.setAlpha(1f); // Assicura che sia completamente visibile
        eventiLaiciAdapter.setRicorrenze(eventi);

        // Forza il layout del RecyclerView
        binding.recyclerViewEventiLaici.requestLayout();

        // Forza l'aggiornamento del layout del MotionLayout
        if (binding.motionLayout != null) {
            binding.motionLayout.requestLayout();
        }

        // Verifica la visibilità dopo un breve delay
        binding.cardEventiLaici.postDelayed(() -> {
            if (binding != null && binding.cardEventiLaici != null) {
                int visibility = binding.cardEventiLaici.getVisibility();
                String visStr = visibility == View.VISIBLE ? "VISIBLE" : (visibility == View.GONE ? "GONE" : "INVISIBLE");
                float alpha = binding.cardEventiLaici.getAlpha();
                Log.d("HomeFragment", "card_eventi_laici check: visibility=" + visStr + ", alpha=" + alpha);

                // Se è diventata GONE o invisibile, forza di nuovo VISIBLE
                if (visibility != View.VISIBLE || alpha < 1f) {
                    Log.d("HomeFragment", "FORCING card_eventi_laici to VISIBLE again!");
                    binding.cardEventiLaici.setVisibility(View.VISIBLE);
                    binding.cardEventiLaici.setAlpha(1f);
                }
            }
        }, 100);

        Log.d("HomeFragment", "Displaying " + eventi.size() + " eventi laici generali");
        for (Ricorrenza e : eventi) {
            Log.d("HomeFragment", "  - " + e.getNomeCompleto());
        }
    }

    private void updateInitialVisibility() {
        if (binding.recyclerViewSaints != null) {
            binding.recyclerViewSaints.setVisibility(View.GONE);
        }

        updateSaintsListIcon();
    }

    private void loadBackgroundImage() {
        String bg = BackgroundManager.getSavedBackground(requireContext());

        switch (bg) {
            case BackgroundManager.BG_SANTO:
                binding.backgroundImage.setVisibility(View.VISIBLE);
                binding.backgroundImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                Glide.with(this)
                        .load(R.drawable.background_saint)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(binding.backgroundImage);
                break;

            case BackgroundManager.BG_NONE:
                binding.backgroundImage.setVisibility(View.GONE);
                break;

            case BackgroundManager.BG_GRADIENT_WARM:
            case BackgroundManager.BG_GRADIENT_COOL:
            case BackgroundManager.BG_GRADIENT_SUNSET:
                binding.backgroundImage.setVisibility(View.VISIBLE);
                binding.backgroundImage.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                binding.backgroundImage.setImageResource(BackgroundManager.getBackgroundDrawableResId(bg));
                break;

            case BackgroundManager.BG_CUSTOM:
                String customPath = BackgroundManager.getCustomBackgroundPath(requireContext());
                binding.backgroundImage.setVisibility(View.VISIBLE);
                binding.backgroundImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                if (customPath != null) {
                    Glide.with(this)
                            .load(customPath)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerCrop()
                            .error(R.drawable.background_saint)
                            .into(binding.backgroundImage);
                } else {
                    Glide.with(this)
                            .load(R.drawable.background_saint)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerCrop()
                            .into(binding.backgroundImage);
                }
                break;

            default:
                binding.backgroundImage.setVisibility(View.VISIBLE);
                binding.backgroundImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                Glide.with(this)
                        .load(R.drawable.background_saint)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(binding.backgroundImage);
                break;
        }
    }

    private void navigateToRicorrenzaDetail(int ricorrenzaId) {
        if (ricorrenzaId > 0) {
            NavDirections action = HomeFragmentDirections.actionHomeFragmentToRicorrenzaDetailFragment(ricorrenzaId);
            Navigation.findNavController(requireView()).navigate(action);
        } else {
            Toast.makeText(requireContext(), getString(R.string.invalid_ricorrenza_id), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSaintsList() {
        ricorrenzaAdapter = new RicorrenzaAdapter(this::navigateToRicorrenzaDetail, ricorrenzaViewModel);
        binding.recyclerViewSaints.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewSaints.setAdapter(ricorrenzaAdapter);

        binding.expandCollapseSaintsIcon.setOnClickListener(v -> toggleSaintsListExpansion());

        ricorrenzaViewModel.getRicorrenzeDelGiorno().observe(getViewLifecycleOwner(), this::updateRicorrenzeList);
    }

    private void reloadSaintOfDay() {
        Log.d("HomeFragment", "reloadSaintOfDay chiamata");
        ricorrenzaViewModel.refreshRandomSaint();
    }

    private void toggleSaintsListExpansion() {
        isSaintsListExpanded = !isSaintsListExpanded;
        binding.recyclerViewSaints.setVisibility(isSaintsListExpanded ? View.VISIBLE : View.GONE);
        binding.expandCollapseSaintsIcon.setImageResource(isSaintsListExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        updateComponentsState();
    }

    private void updateSaintsListIcon() {
        if (binding.expandCollapseSaintsIcon != null) {
            binding.expandCollapseSaintsIcon.setImageResource(isSaintsListExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        }
    }
    private void navigateToAddItemFragment() {
        NavDirections action = HomeFragmentDirections.actionHomeFragmentToAddItemFragment();
        Navigation.findNavController(requireView()).navigate(action);
    }

    private void setInitialSaintsListState() {
        isSaintsListExpanded = false;
        if (binding.recyclerViewSaints != null) {
            binding.recyclerViewSaints.setVisibility(View.GONE);
        }
        if (ricorrenzaAdapter != null) {
            ricorrenzaAdapter.setCollapsedView(true);
        }
        updateSaintsListIcon();
    }

    private void navigateToEditFragment(int ricorrenzaId) {
        if (ricorrenzaId > 0) {
            NavDirections action = HomeFragmentDirections.actionHomeFragmentToRicorrenzaDetailFragment(ricorrenzaId);
            Navigation.findNavController(requireView()).navigate(action);
        } else {
            Toast.makeText(requireContext(), getString(R.string.invalid_ricorrenza_id), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDateDisplay() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat weekdayFormat = new SimpleDateFormat("EEEE", Locale.ITALIAN);
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ITALIAN);

        binding.tvWeekday.setText(weekdayFormat.format(calendar.getTime()));
        binding.tvDay.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        binding.tvMonth.setText(DateUtils.getCurrentMonthNameFull());
    }

    private void showDeleteConfirmationDialog(Ricorrenza ricorrenza) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete)
                .setMessage(R.string.confirm_delete_ricorrenza)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> deleteRicorrenza(ricorrenza))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteRicorrenza(Ricorrenza ricorrenza) {
        ricorrenzaViewModel.deleteRicorrenza(ricorrenza);
    }

    private void observeDeleteResult() {
        ricorrenzaViewModel.getDeleteResult().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                Toast.makeText(requireContext(), getString(R.string.ricorrenza_deleted_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupScrollListener() {
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).handleScroll();
                }
            }
        });
    }

    private boolean isScrolledToTop() {
        return nestedScrollView != null && nestedScrollView.getScrollY() == 0;
    }

    private void updateBottomMenuVisibility() {
        if (!isAdded()) return;
        boolean allCollapsed = !isSaintsListExpanded;
        if (allCollapsed && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateComponentsVisibility(true);
        }
    }

    private void updateComponentsState() {
        if (binding == null || !isAdded()) return;
        boolean allCollapsed = !isCalendarExpanded && !isSaintsListExpanded && isScrolledToTop();
        homeViewModel.setAllComponentsCollapsed(allCollapsed);
    }

    private void setupOverview() {
        // Inizializza adapter per la card overview unificata
        overviewAdapter = new OverviewAdapter(this::onOverviewItemClick);
        binding.recyclerViewOverview.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewOverview.setAdapter(overviewAdapter);

        // Cache locali per i dati
        final List<Impegno>[] impegniCache = new List[]{new java.util.ArrayList<>()};
        final List<ListaSpesa>[] listeCache = new List[]{new java.util.ArrayList<>()};
        final List<Nota>[] noteCache = new List[]{new java.util.ArrayList<>()};

        // Observer per impegni
        impegniViewModel.getImpegniOggi().observe(getViewLifecycleOwner(), impegni -> {
            impegniCache[0] = impegni != null ? impegni : new java.util.ArrayList<>();
            updateOverviewCard(impegniCache[0], listeCache[0], noteCache[0]);
        });

        // Observer per liste spesa
        listeSpesaViewModel.getListeAttive().observe(getViewLifecycleOwner(), liste -> {
            listeCache[0] = liste != null ? liste : new java.util.ArrayList<>();
            updateOverviewCard(impegniCache[0], listeCache[0], noteCache[0]);
        });

        // Observer per note
        noteViewModel.getAllNote().observe(getViewLifecycleOwner(), note -> {
            noteCache[0] = note != null ? note : new java.util.ArrayList<>();
            updateOverviewCard(impegniCache[0], listeCache[0], noteCache[0]);
        });
    }

    private void updateOverviewCard(List<Impegno> impegni, List<ListaSpesa> liste, List<Nota> note) {
        if (binding == null || !isAdded()) return;

        List<OverviewItem> items = new java.util.ArrayList<>();

        // Aggiungi impegni di oggi
        for (Impegno impegno : impegni) {
            items.add(new OverviewItem(
                OverviewItem.Type.IMPEGNO,
                impegno.getTitolo(),
                impegno.getId()
            ));
        }

        // Aggiungi liste spesa attive
        for (ListaSpesa lista : liste) {
            items.add(new OverviewItem(
                OverviewItem.Type.LISTA_SPESA,
                lista.getNome(),
                lista.getId()
            ));
        }

        // Aggiungi note
        for (Nota nota : note) {
            String text = nota.getTitolo();
            if (text == null || text.isEmpty()) {
                text = nota.getContenuto() != null && nota.getContenuto().length() > 50
                    ? nota.getContenuto().substring(0, 50) + "..."
                    : nota.getContenuto();
            }
            items.add(new OverviewItem(
                OverviewItem.Type.NOTA,
                text,
                nota.getId()
            ));
        }

        // Aggiorna adapter
        overviewAdapter.setItems(items);

        // Gestisci visibilità della card
        if (items.isEmpty()) {
            binding.cardOverview.setVisibility(View.GONE);
        } else {
            binding.cardOverview.setVisibility(View.VISIBLE);
        }

        Log.d("HomeFragment", "Overview updated: " + impegni.size() + " impegni, " +
              liste.size() + " liste, " + note.size() + " note = " + items.size() + " total");
    }

    private void onOverviewItemClick(OverviewItem item) {
        Bundle args = new Bundle();

        switch (item.getType()) {
            case IMPEGNO:
                // Naviga al dettaglio impegno (edit)
                args.putInt("impegnoId", item.getId());
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_global_to_edit_impegno, args);
                break;

            case LISTA_SPESA:
                // Naviga al dettaglio lista spesa
                args.putInt("listaId", item.getId());
                Navigation.findNavController(requireView())
                    .navigate(R.id.action_homeFragment_to_dettaglioListaSpesaFragment, args);
                break;

            case NOTA:
                // Naviga al dettaglio nota
                args.putInt("notaId", item.getId());
                Navigation.findNavController(requireView())
                    .navigate(R.id.noteDetailFragment, args);
                break;
        }
    }

    private void setupQuickActions() {
        android.util.Log.d("HomeFragment", "setupQuickActions called");

        if (binding == null) {
            android.util.Log.e("HomeFragment", "binding is null!");
            return;
        }
        if (binding.layoutQuickActions == null) {
            android.util.Log.e("HomeFragment", "binding.layoutQuickActions is null!");
            return;
        }
        if (binding.btnScrivi == null) {
            android.util.Log.e("HomeFragment", "binding.btnScrivi is null!");
            return;
        }

        android.util.Log.d("HomeFragment", "All quick action bindings are valid");

        // Pulsante Scrivi - per note/post-it
        binding.btnScrivi.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.action_global_to_note);
        });

        // Pulsante Organizza - per aggiungere più impegni rapidamente
        binding.btnOrganizza.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.action_global_to_organizza);
        });

        // Pulsante Ricerca - ricerca globale
        binding.btnRicerca.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.searchFragment);
        });

        // Pulsante Riepilogo - vista settimanale/mensile
        binding.btnRiepilogo.setOnClickListener(v -> {
            Navigation.findNavController(requireView()).navigate(R.id.action_global_to_riepilogo);
        });

        // Controlla visibilità dei pulsanti in base allo stato del calendario
        // I pulsanti sono visibili quando il calendario è collassato
        // Inizialmente il calendario è espanso, quindi nascondi i pulsanti
        updateQuickActionsVisibility();
    }

    private void updateQuickActionsVisibility() {
        if (binding != null && binding.layoutQuickActions != null) {
            // I pulsanti sono visibili SOLO quando il calendario è collassato
            // L'alpha è gestito dal MotionLayout (0 quando espanso, 1 quando collassato)
            binding.layoutQuickActions.setVisibility(View.VISIBLE);
            android.util.Log.d("HomeFragment", "Quick actions visibility. Calendar expanded: " + isCalendarExpanded);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume() chiamato");

        // Ricarica lo sfondo quando l'utente torna dalla schermata impostazioni
        if (binding != null) {
            loadBackgroundImage();
        }

        // Ricarica i dati SOLO se non è il primo caricamento
        if (isInitialized) {
            ricorrenzaViewModel.forceReloadRicorrenze();
        } else {
            isInitialized = true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        collapseHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}