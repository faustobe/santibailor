package it.faustobe.santibailor.presentation.features.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import it.faustobe.santibailor.R;
import it.faustobe.santibailor.data.backup.BackupManager;
import it.faustobe.santibailor.databinding.FragmentCategorySettingsBinding;
import it.faustobe.santibailor.util.BackgroundManager;
import it.faustobe.santibailor.util.ImageHandler;
import it.faustobe.santibailor.util.LanguageManager;
import it.faustobe.santibailor.util.ThemeManager;
import it.faustobe.santibailor.util.WorkManagerHelper;

@AndroidEntryPoint
public class CategorySettingsFragment extends Fragment implements CategorySettingsAdapter.OnSettingItemClickListener {
    private FragmentCategorySettingsBinding binding;
    private CategorySettingsAdapter adapter;
    private List<SettingItem> settingItems;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<String> createBackupLauncher;
    private ActivityResultLauncher<String[]> openBackupLauncher;

    @Inject
    BackupManager backupManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleCustomBackgroundSelected(uri);
                    }
                }
        );
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        enableNotifications();
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.notifications_disabled_label),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
        createBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri != null) {
                        performBackupExport(uri);
                    }
                }
        );
        openBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        confirmBackupRestore(uri);
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategorySettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CategorySettingsFragmentArgs args = CategorySettingsFragmentArgs.fromBundle(getArguments());
        String categoryTitle = args.getCategoryTitle();
        SettingItem[] settingItemsArray = args.getSettingItems();

        binding.categoryTitle.setText(categoryTitle);

        if (settingItemsArray != null && settingItemsArray.length > 0) {
            settingItems = Arrays.asList(settingItemsArray);
            setupRecyclerView(settingItems);
        } else {
            Log.e("CategorySettingsFragment", "Nessun SettingItem fornito");
        }

        setupBackButton();
    }

    private void setupRecyclerView(List<SettingItem> settingItems) {
        adapter = new CategorySettingsAdapter(settingItems, this);
        binding.settingsRecyclerView.setAdapter(adapter);
        binding.settingsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setupBackButton() {
        binding.btnBackToSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_categorySettingsFragment_to_navigation_settings)
        );
    }

    @Override
    public void onSettingItemClick(SettingItem item) {
        switch (item.getType()) {
            case NAVIGATION:
                Integer navigationAction = item.getNavigationAction();
                if (navigationAction != null && navigationAction != 0) {
                    navigateToDestination(navigationAction);
                }
                break;
            case TOGGLE:
                boolean newState = !item.isToggleState();
                item.setToggleState(newState);
                // Qui potresti voler aggiornare lo stato nel ViewModel o nel repository
                break;
            case ACTION:
                performAction(item);
                break;
        }
        adapter.notifyDataSetChanged();
    }

    private void navigateToDestination(int destinationId) {
        NavController navController = Navigation.findNavController(requireView());
        try {
            navController.navigate(destinationId);
        } catch (Exception e) {
            Log.e("CategorySettingsFragment", "Navigation error: " + e.getMessage());
            Toast.makeText(requireContext(), getString(R.string.navigation_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void performAction(SettingItem item) {
        String title = item.getTitle();

        if (getString(R.string.settings_general_theme_title).equals(title)) {
            showThemeDialog();
        } else if (getString(R.string.settings_general_language_title).equals(title)) {
            showLanguageDialog();
        } else if (getString(R.string.settings_general_background_title).equals(title)) {
            showBackgroundDialog();
        } else if (getString(R.string.settings_general_notifications_title).equals(title)) {
            showNotificationDialog();
        } else if (getString(R.string.settings_notifications_ricorrenze_title).equals(title)) {
            showRicorrenzeNotificationDialog();
        } else if (getString(R.string.settings_notifications_impegni_title).equals(title)) {
            showImpegniNotificationDialog();
        } else if (getString(R.string.settings_privacy_app_permissions_title).equals(title)) {
            openAppPermissions();
        } else if (getString(R.string.settings_backup_title).equals(title)) {
            startBackupExport();
        } else if (getString(R.string.settings_restore_title).equals(title)) {
            startBackupRestore();
        } else if (getString(R.string.settings_privacy_export_data_title).equals(title)) {
            exportData();
        } else if (getString(R.string.settings_privacy_delete_data_title).equals(title)) {
            confirmDeleteData();
        } else if (getString(R.string.settings_privacy_policy_title).equals(title)) {
            showPrivacyPolicy();
        } else if (getString(R.string.settings_share_export_title).equals(title)) {
            shareExport();
        } else {
            Log.d("CategorySettingsFragment", "Action executed for: " + title);
            Toast.makeText(requireContext(), getString(R.string.feature_in_development), Toast.LENGTH_SHORT).show();
        }
    }

    private void showThemeDialog() {
        String currentTheme = ThemeManager.getSavedTheme(requireContext());

        String[] themes = {
                ThemeManager.THEME_LIGHT,
                ThemeManager.THEME_DARK,
                ThemeManager.THEME_SYSTEM
        };

        String[] themeNames = {
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark),
                getString(R.string.settings_theme_system)
        };

        int selectedIndex = Arrays.asList(themes).indexOf(currentTheme);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_theme)
                .setSingleChoiceItems(themeNames, selectedIndex, (dialog, which) -> {
                    String selectedTheme = themes[which];
                    ThemeManager.saveTheme(requireContext(), selectedTheme);
                    ThemeManager.applyTheme(selectedTheme);
                    Toast.makeText(requireContext(), getString(R.string.theme_applied), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLanguageDialog() {
        String currentLanguage = LanguageManager.getSavedLanguage(requireContext());

        String[] languages = {
                LanguageManager.LANGUAGE_ITALIAN,
                LanguageManager.LANGUAGE_ENGLISH,
                LanguageManager.LANGUAGE_SYSTEM
        };

        String[] languageNames = {
                getString(R.string.settings_language_italian),
                getString(R.string.settings_language_english),
                getString(R.string.settings_language_system)
        };

        int selectedIndex = Arrays.asList(languages).indexOf(currentLanguage);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_language)
                .setSingleChoiceItems(languageNames, selectedIndex, (dialog, which) -> {
                    String selectedLanguage = languages[which];
                    LanguageManager.saveLanguage(requireContext(), selectedLanguage);
                    LanguageManager.applyLanguage(requireContext(), selectedLanguage);
                    Toast.makeText(requireContext(), getString(R.string.language_applied_restart), Toast.LENGTH_LONG).show();
                    dialog.dismiss();

                    // Ricrea l'activity per applicare la lingua
                    requireActivity().recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showBackgroundDialog() {
        String currentBackground = BackgroundManager.getSavedBackground(requireContext());

        String[] backgrounds = {
                BackgroundManager.BG_SANTO,
                BackgroundManager.BG_NONE,
                BackgroundManager.BG_GRADIENT_WARM,
                BackgroundManager.BG_GRADIENT_COOL,
                BackgroundManager.BG_GRADIENT_SUNSET,
                BackgroundManager.BG_CUSTOM
        };

        String[] backgroundNames = {
                getString(R.string.background_santo),
                getString(R.string.background_none),
                getString(R.string.background_gradient_warm),
                getString(R.string.background_gradient_cool),
                getString(R.string.background_gradient_sunset),
                getString(R.string.background_custom)
        };

        int selectedIndex = Arrays.asList(backgrounds).indexOf(currentBackground);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_background)
                .setSingleChoiceItems(backgroundNames, selectedIndex, (dialog, which) -> {
                    String selectedBackground = backgrounds[which];
                    if (BackgroundManager.BG_CUSTOM.equals(selectedBackground)) {
                        imagePickerLauncher.launch("image/*");
                    } else {
                        BackgroundManager.saveBackground(requireContext(), selectedBackground);
                        Toast.makeText(requireContext(), getString(R.string.background_applied), Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showNotificationDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        boolean currentEnabled = prefs.getBoolean("notifications_enabled", true);

        String[] options = {
                getString(R.string.notifications_enabled_label),
                getString(R.string.notifications_disabled_label)
        };

        int selectedIndex = currentEnabled ? 0 : 1;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_general_notifications_title)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    if (which == 0) {
                        // Abilitate
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(requireContext(),
                                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                enableNotifications();
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                            }
                        } else {
                            enableNotifications();
                        }
                    } else {
                        // Disabilitate
                        disableNotifications();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void enableNotifications() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("notifications_enabled", true).apply();
        WorkManagerHelper.scheduleDailySaintNotification(requireContext());
        Toast.makeText(requireContext(), getString(R.string.notifications_updated), Toast.LENGTH_SHORT).show();
    }

    private void disableNotifications() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("notifications_enabled", false).apply();
        WorkManagerHelper.cancelDailySaintNotification(requireContext());
        Toast.makeText(requireContext(), getString(R.string.notifications_updated), Toast.LENGTH_SHORT).show();
    }

    private void showRicorrenzeNotificationDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        boolean currentEnabled = prefs.getBoolean("saint_notifications_enabled", true);

        String[] options = {
                getString(R.string.notifications_enabled_label),
                getString(R.string.notifications_disabled_label)
        };

        int selectedIndex = currentEnabled ? 0 : 1;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_notifications_ricorrenze_title)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    if (which == 0) {
                        // Abilitate → mostra TimePicker
                        dialog.dismiss();
                        int savedHour = prefs.getInt("saint_notification_hour", 7);
                        int savedMinute = prefs.getInt("saint_notification_minute", 0);
                        new TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
                            prefs.edit()
                                    .putBoolean("saint_notifications_enabled", true)
                                    .putInt("saint_notification_hour", hourOfDay)
                                    .putInt("saint_notification_minute", minuteOfHour)
                                    .apply();
                            WorkManagerHelper.scheduleDailySaintNotification(requireContext(), hourOfDay, minuteOfHour);
                            Toast.makeText(requireContext(),
                                    String.format(getString(R.string.notification_time_set), hourOfDay, minuteOfHour),
                                    Toast.LENGTH_SHORT).show();
                        }, savedHour, savedMinute, true).show();
                    } else {
                        // Disabilitate
                        prefs.edit().putBoolean("saint_notifications_enabled", false).apply();
                        WorkManagerHelper.cancelDailySaintNotification(requireContext());
                        Toast.makeText(requireContext(), getString(R.string.notifications_updated), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showImpegniNotificationDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        boolean currentEnabled = prefs.getBoolean("impegni_notifications_enabled", true);

        String[] options = {
                getString(R.string.notifications_enabled_label),
                getString(R.string.notifications_disabled_label)
        };

        int selectedIndex = currentEnabled ? 0 : 1;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_notifications_impegni_title)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    boolean enabled = (which == 0);
                    prefs.edit().putBoolean("impegni_notifications_enabled", enabled).apply();
                    Toast.makeText(requireContext(), getString(R.string.impegni_notifications_updated), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openAppPermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void exportData() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
            StringBuilder sb = new StringBuilder();
            sb.append("=== Santibailor Data Export ===\n\n");
            sb.append("Settings:\n");
            for (String key : prefs.getAll().keySet()) {
                sb.append("  ").append(key).append(" = ").append(prefs.getAll().get(key)).append("\n");
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Santibailor - Data Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
            startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_privacy_export_data_title)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.privacy_export_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteData() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.privacy_delete_confirm_title)
                .setMessage(R.string.privacy_delete_confirm_message)
                .setPositiveButton(R.string.elimina, (dialog, which) -> {
                    SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
                    prefs.edit().clear().apply();
                    Toast.makeText(requireContext(), getString(R.string.privacy_data_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.annulla, null)
                .show();
    }

    private void showPrivacyPolicy() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_privacy_policy_title)
                .setMessage(R.string.privacy_policy_text)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void startBackupExport() {
        String fileName = "santibailor_backup_"
                + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date())
                + ".json";
        createBackupLauncher.launch(fileName);
    }

    private void performBackupExport(Uri uri) {
        backupManager.exportToUri(uri, new BackupManager.Callback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.backup_success), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.backup_error), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startBackupRestore() {
        // Alcuni file manager etichettano i .json in modi diversi
        openBackupLauncher.launch(new String[]{"application/json", "application/octet-stream", "text/plain"});
    }

    private void confirmBackupRestore(Uri uri) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.restore_confirm_title)
                .setMessage(R.string.restore_confirm_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> performBackupRestore(uri))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performBackupRestore(Uri uri) {
        backupManager.importFromUri(uri, new BackupManager.Callback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.restore_success), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.restore_error), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void shareExport() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Santibailor");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.info_app_description));
            startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_share_export_title)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.share_export_no_data), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCustomBackgroundSelected(Uri uri) {
        ImageHandler imageHandler = ImageHandler.getInstance(requireContext());
        imageHandler.saveLocalImageOnly(uri, new ImageHandler.OnImageSavedListener() {
            @Override
            public void onImageSaved(String imageUrl) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        BackgroundManager.saveCustomBackgroundPath(requireContext(), imageUrl);
                        BackgroundManager.saveBackground(requireContext(), BackgroundManager.BG_CUSTOM);
                        Toast.makeText(requireContext(), getString(R.string.background_applied), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), getString(R.string.background_custom_error), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}