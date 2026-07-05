package it.faustobe.santibailor.presentation.features.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import dagger.hilt.android.AndroidEntryPoint;
import it.faustobe.santibailor.R;
import it.faustobe.santibailor.databinding.FragmentNotificationsBinding;
import it.faustobe.santibailor.domain.model.Impegno;

/**
 * Schermata dei promemoria: elenca gli impegni futuri con reminder attivo
 * e l'orario in cui scatterà la notifica
 */
@AndroidEntryPoint
public class NotificationsFragment extends Fragment implements PromemoriaAdapter.OnPromemoriaClickListener {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private PromemoriaAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        adapter = new PromemoriaAdapter(this);
        binding.recyclerViewPromemoria.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewPromemoria.setAdapter(adapter);

        viewModel.getPromemoriaProgrammati().observe(getViewLifecycleOwner(), impegni -> {
            boolean empty = impegni == null || impegni.isEmpty();
            binding.recyclerViewPromemoria.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.emptyStatePromemoria.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.setImpegni(impegni);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisabledBanner();
    }

    private void updateDisabledBanner() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("notifications_enabled", true)
                && prefs.getBoolean("impegni_notifications_enabled", true);
        binding.bannerPromemoriaDisabilitati.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPromemoriaClick(Impegno impegno) {
        Bundle args = new Bundle();
        args.putInt("impegnoId", impegno.getId());
        Navigation.findNavController(requireView()).navigate(R.id.editImpegnoFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
