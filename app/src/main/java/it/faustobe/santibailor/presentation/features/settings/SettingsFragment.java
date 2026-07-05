package it.faustobe.santibailor.presentation.features.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import it.faustobe.santibailor.R;
import it.faustobe.santibailor.databinding.FragmentSettingsBinding;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        setupListeners();
        setupMenu();
    }

    private void setupToolbar() {
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.title_settings);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    navigateBack();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    private void setupListeners() {
        binding.cardAspetto.setOnClickListener(v -> navigateToAspetto());
        binding.cardNotifiche.setOnClickListener(v -> navigateToNotifiche());
        binding.cardCercaGestisci.setOnClickListener(v -> navigateToCercaGestisci());
        binding.cardPrivacy.setOnClickListener(v -> navigateToPrivacy());
        binding.cardInfo.setOnClickListener(v -> navigateToInfo());
    }

    private void navigateToAspetto() {
        List<SettingItem> items = new ArrayList<>();
        items.add(new SettingItem(getString(R.string.settings_general_theme_title), getString(R.string.settings_general_theme_desc)));
        items.add(new SettingItem(getString(R.string.settings_general_language_title), getString(R.string.settings_general_language_desc)));
        items.add(new SettingItem(getString(R.string.settings_general_background_title), getString(R.string.settings_general_background_desc)));
        navigateToCategorySettings(getString(R.string.settings_aspetto_category), items.toArray(new SettingItem[0]));
    }

    private void navigateToNotifiche() {
        List<SettingItem> items = new ArrayList<>();
        items.add(new SettingItem(getString(R.string.settings_notifications_ricorrenze_title), getString(R.string.settings_notifications_ricorrenze_desc)));
        items.add(new SettingItem(getString(R.string.settings_notifications_impegni_title), getString(R.string.settings_notifications_impegni_desc)));
        navigateToCategorySettings(getString(R.string.settings_notifiche_category), items.toArray(new SettingItem[0]));
    }

    private void navigateToCercaGestisci() {
        List<SettingItem> items = new ArrayList<>();
        items.add(new SettingItem(getString(R.string.settings_search_ricorrenze_title), getString(R.string.settings_search_ricorrenze_desc), R.id.action_categorySettingsFragment_to_searchFragment));
        items.add(new SettingItem(getString(R.string.settings_manage_ricorrenze_title), getString(R.string.settings_manage_ricorrenze_desc), R.id.action_categorySettingsFragment_to_manageRicorrenzeFragment));
        items.add(new SettingItem(getString(R.string.settings_share_export_title), getString(R.string.settings_share_export_desc)));
        navigateToCategorySettings(getString(R.string.settings_cerca_gestisci_category), items.toArray(new SettingItem[0]));
    }

    private void navigateToPrivacy() {
        List<SettingItem> items = new ArrayList<>();
        items.add(new SettingItem(getString(R.string.settings_privacy_app_permissions_title), getString(R.string.settings_privacy_app_permissions_desc)));
        items.add(new SettingItem(getString(R.string.settings_backup_title), getString(R.string.settings_backup_desc)));
        items.add(new SettingItem(getString(R.string.settings_restore_title), getString(R.string.settings_restore_desc)));
        items.add(new SettingItem(getString(R.string.settings_privacy_export_data_title), getString(R.string.settings_privacy_export_data_desc)));
        items.add(new SettingItem(getString(R.string.settings_privacy_delete_data_title), getString(R.string.settings_privacy_delete_data_desc)));
        items.add(new SettingItem(getString(R.string.settings_privacy_policy_title), getString(R.string.settings_privacy_policy_desc)));
        navigateToCategorySettings(getString(R.string.settings_privacy_new_category), items.toArray(new SettingItem[0]));
    }

    private void navigateToInfo() {
        Navigation.findNavController(requireView()).navigate(R.id.action_settingsFragment_to_infoFragment);
    }

    private void navigateToCategorySettings(String categoryTitle, SettingItem[] settingItems) {
        try {
            SettingsFragmentDirections.ActionSettingsFragmentToCategorySettingsFragment action =
                    SettingsFragmentDirections.actionSettingsFragmentToCategorySettingsFragment(
                            categoryTitle,
                            settingItems
                    );
            Navigation.findNavController(requireView()).navigate(action);
        } catch (Exception e) {
            Log.e("SettingsFragment", "Errore nella navigazione con Safe Args: " + e.getMessage());
            navigateToCategorySettingsFallback(categoryTitle, settingItems);
        }
    }

    private void navigateToCategorySettingsFallback(String categoryTitle, SettingItem[] settingItems) {
        Bundle args = new Bundle();
        args.putString("categoryTitle", categoryTitle);
        args.putParcelableArray("settingItems", settingItems);

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_settingsFragment_to_categorySettingsFragment, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
