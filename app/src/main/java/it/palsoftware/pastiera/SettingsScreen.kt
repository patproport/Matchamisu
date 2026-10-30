package it.palsoftware.pastiera

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import it.palsoftware.pastiera.R
import android.widget.Toast
import it.palsoftware.pastiera.BuildConfig
import it.palsoftware.pastiera.inputmethod.DeviceSpecific
import kotlinx.coroutines.delay

/**
 * Sealed class per rappresentare lo stato della navigazione nelle settings.
 */
enum class SettingsDestination {
    Main,
    Q25Input,
    Q25Display,
    KeyboardsDevices,
    TextInput,
    Accessibility,
    AutoCorrection,
    Customization,
    NavMode,
    Advanced,
    About,
    CustomInputStyles,
    AppLanguage,
    DeviceSymLayerEditor,
    Modifiers
}

/** The destination payload of one SettingsActivity, also used by deep links. */
internal data class SettingsPage(
    val destination: SettingsDestination,
    val customizationDestination: String? = null,
    val keyboardThemeTarget: String? = null,
    val keyboardThemeTab: String? = null,
    val navModeKeyCode: Int? = null,
    val keyboardsDevicesDestination: KeyboardsDevicesDestination = KeyboardsDevicesDestination.Main
)

internal fun SettingRoute.toSettingsPage() = SettingsPage(
    destination = destination,
    customizationDestination = customizationDestination,
    keyboardThemeTarget = keyboardThemeTarget?.name,
    keyboardThemeTab = keyboardThemeTab?.name,
    keyboardsDevicesDestination = keyboardsDevicesDestination
)

/**
 * App settings screen.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    initialDestination: String? = null,
    initialCustomizationDestination: String? = null,
    initialKeyboardThemeTarget: String? = null,
    settingLinkRequest: SettingLinkRequest? = null
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val currentEntry = remember { context.settingsActivity().intent.settingsPage() }
    val currentDestination = currentEntry.destination
    var highlightSettingId by rememberSaveable { mutableStateOf<String?>(null) }
    var linkSheetEntry by remember { mutableStateOf<SettingEntry?>(null) }

    fun navigateTo(destination: SettingsDestination) {
        openSettingsPage(context, SettingsPage(destination))
    }
    fun navigateBack() { context.settingsActivity().finish() }
    fun openCustomization(destination: String?, keyboardThemeTarget: String? = null, keyboardThemeTab: String? = null) {
        openSettingsPage(context, SettingsPage(SettingsDestination.Customization,
            destination, keyboardThemeTarget, keyboardThemeTab))
    }
    fun navigateToNavMode(keyCode: Int?) {
        openSettingsPage(context, SettingsPage(SettingsDestination.NavMode, navModeKeyCode = keyCode))
    }

    /**
     * Navigates to a settings entry (e.g. from search or a deep link) and asks
     * its row to flash and scroll into view. Never changes any value.
     */
    fun openSettingEntry(entry: SettingEntry) {
        linkSheetEntry = null
        val visibleEntry = SettingLinkRegistry.visibleTarget(context, entry)
        val route = visibleEntry.route
        if (route.symCustomization) {
            context.startActivity(Intent(context, SymCustomizationActivity::class.java).apply {
                putExtra(SymCustomizationActivity.EXTRA_SETTING_ID, visibleEntry.id)
            })
            return
        }
        val target = route.toSettingsPage()
        if (currentEntry != target) {
            context.startActivity(Intent(context, SettingsActivity::class.java).apply {
                data = android.net.Uri.parse(SettingLinkRegistry.buildLink(visibleEntry.id))
            })
            return
        }
        highlightSettingId = visibleEntry.id
    }

    // Settings deep link arriving via intent or onNewIntent.
    LaunchedEffect(settingLinkRequest?.serial) {
        val request = settingLinkRequest ?: return@LaunchedEffect
        val entry = SettingLinkRegistry.byId(request.id)
        if (entry == null) {
            Toast.makeText(context, R.string.settings_link_unavailable_toast, Toast.LENGTH_SHORT)
                .show()
        } else {
            openSettingEntry(entry)
        }
    }
    LaunchedEffect(highlightSettingId) {
        if (highlightSettingId != null) {
            // Must outlast the blink sequence in settingRow (~1.65 s) so the
            // outline fades out gently after the last blink.
            delay(1800)
            highlightSettingId = null
        }
    }

    CompositionLocalProvider(
        LocalSettingHighlightId provides highlightSettingId,
        LocalSettingLinkLongPress provides ({ id -> linkSheetEntry = SettingLinkRegistry.byId(id) })
    ) {
    val entry = currentEntry
        when (entry.destination) {
            SettingsDestination.Main -> {
                SettingsMainScreen(
                    modifier = modifier,
                    context = context,
                    onInputClick = { navigateTo(SettingsDestination.Q25Input) },
                    onDisplayClick = { navigateTo(SettingsDestination.Q25Display) },
                    onAboutClick = { navigateTo(SettingsDestination.About) },
                    onBackClick = { navigateBack() },
                    onOpenSettingEntry = ::openSettingEntry
                )
            }
            SettingsDestination.Q25Input -> Q25SettingsCategoryScreen(
                title = stringResource(R.string.q25_input_settings_title),
                modifier = modifier,
                onBack = { navigateBack() },
                onOpenEntry = ::openSettingEntry,
                entryIds = listOf(
                    SettingLinkIds.MAIN_KEYBOARDS_DEVICES,
                    SettingLinkIds.MAIN_CUSTOM_INPUT_STYLES,
                    SettingLinkIds.MAIN_MODIFIERS,
                    SettingLinkIds.MAIN_AUTO_CORRECTION,
                    SettingLinkIds.MAIN_TEXT_INPUT,
                    SettingLinkIds.MAIN_NAV_MODE,
                    SettingLinkIds.MAIN_LAUNCHER_SHORTCUTS,
                    SettingLinkIds.MAIN_APP_ENTER_BEHAVIOR,
                    SettingLinkIds.MAIN_ACCESSIBILITY
                )
            )
            SettingsDestination.Q25Display -> Q25SettingsCategoryScreen(
                title = stringResource(R.string.q25_display_settings_title),
                modifier = modifier,
                onBack = { navigateBack() },
                onOpenEntry = ::openSettingEntry,
                showThemeSelector = true,
                entryIds = listOf(
                    SettingLinkIds.MAIN_STATUS_BAR_BUTTONS,
                    SettingLinkIds.MAIN_APP_LANGUAGE
                ),
                onOpenAdvancedTheme = { openCustomization(SettingsActivity.CUSTOMIZATION_DESTINATION_KEYBOARD_THEME) }
            )
            SettingsDestination.KeyboardsDevices -> {
                KeyboardsDevicesSettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() },
                    onNavModeSettingsClick = { keyCode ->
                        navigateToNavMode(keyCode)
                    },
                    onOpenKeyboardTheme = {
                        openCustomization(
                            SettingsActivity.CUSTOMIZATION_DESTINATION_KEYBOARD_THEME,
                            SettingsActivity.KEYBOARD_THEME_TARGET_SOFTWARE
                        )
                    },
                    destination = entry.keyboardsDevicesDestination,
                    onDestinationChange = { destination ->
                        if (destination == KeyboardsDevicesDestination.Main) navigateBack()
                        else openSettingsPage(context, entry.copy(keyboardsDevicesDestination = destination))
                    }
                )
            }
            SettingsDestination.TextInput -> {
                TextInputSettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() },
                    onNavModeSettingsClick = { navigateToNavMode(null) }
                )
            }
            SettingsDestination.Accessibility -> {
                AccessibilitySettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() }
                )
            }
            SettingsDestination.AutoCorrection -> {
                AutoCorrectionCategoryScreen(
                    modifier = modifier,
                    onBack = { navigateBack() }
                )
            }
            SettingsDestination.Customization -> {
                CustomizationSettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() },
                    initialDestination = entry.customizationDestination,
                    initialKeyboardThemeTarget = entry.keyboardThemeTarget,
                    initialKeyboardThemeTab = entry.keyboardThemeTab,
                    onOpenModifiers = { navigateTo(SettingsDestination.Modifiers) }
                )
            }
            SettingsDestination.NavMode -> {
                NavModeSettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() },
                    initialKeyCode = entry.navModeKeyCode
                )
            }
            SettingsDestination.Advanced -> {
                AdvancedSettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() }
                )
            }
            SettingsDestination.About -> {
                AboutScreen(
                    modifier = modifier,
                    onBack = { navigateBack() }
                )
            }
            SettingsDestination.CustomInputStyles -> {
                CustomInputStylesScreen(
                    modifier = modifier,
                    onBack = { navigateBack() }
                )
            }
            SettingsDestination.AppLanguage -> {
                AppLanguageSettingsScreen(modifier = modifier, onBack = { navigateBack() })
            }
            SettingsDestination.DeviceSymLayerEditor -> {
                DeviceSymLayerEditorStubScreen(modifier = modifier, onBack = { navigateBack() })
            }
            SettingsDestination.Modifiers -> {
                ModifierSettingsScreen(
                    modifier = modifier,
                    onBack = { navigateBack() },
                    onOpenSymLayers = {
                        context.startActivity(
Intent(context, SymCustomizationActivity::class.java)
                        )
                    },
                    onOpenSymShortcuts = {
                        openCustomization(SettingsActivity.CUSTOMIZATION_DESTINATION_LAUNCHER_SHORTCUTS)
                    },
                    onOpenNavMode = {
                        navigateToNavMode(null)
                    }
                )
            }
        }
    }

    // Share/copy sheet for the settings entry currently being long-pressed
    linkSheetEntry?.let { entry ->
        SettingLinkSheet(entry = entry, onDismiss = { linkSheetEntry = null })
    }
}



@Composable
private fun SettingsMainScreen(
    modifier: Modifier,
    context: Context,
    onInputClick: () -> Unit,
    onDisplayClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBackClick: () -> Unit,
    onOpenSettingEntry: (SettingEntry) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchResults = remember(searchQuery, context) {
        SettingLinkRegistry.search(context, searchQuery)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_content_description)
                        )
                    }
                    SettingsSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            if (searchQuery.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (searchResults.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else {
                        searchResults.forEach { entry ->
                            SettingSearchResultRow(
                                entry = entry,
                                onClick = {
                                    keyboardController?.hide()
                                    searchQuery = ""
                                    onOpenSettingEntry(entry)
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    SettingsCategoryRow(
                        icon = Icons.Filled.Keyboard,
                        title = stringResource(R.string.q25_input_settings_title),
                        linkId = SettingLinkIds.Q25_INPUT,
                        onClick = onInputClick
                    )
                    SettingsCategoryRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.q25_display_settings_title),
                        linkId = SettingLinkIds.Q25_DISPLAY,
                        onClick = onDisplayClick
                    )
                    SettingsCategoryRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.q25_app_info_title),
                        description = stringResource(R.string.settings_about_version_summary, BuildConfig.VERSION_NAME),
                        linkId = SettingLinkIds.MAIN_ABOUT,
                        onClick = onAboutClick
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Q25SettingsCategoryScreen(
    title: String,
    modifier: Modifier,
    onBack: () -> Unit,
    onOpenEntry: (SettingEntry) -> Unit,
    entryIds: List<String>,
    showThemeSelector: Boolean = false,
    onOpenAdvancedTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back_content_description))
                    }
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 2)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            if (showThemeSelector) {
                var themeMode by remember { mutableStateOf(SettingsManager.getQ25ThemeMode(context)) }
                Text(stringResource(R.string.q25_theme_label), modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium)
                listOf(
                    SettingsManager.Q25_THEME_SYSTEM to R.string.q25_theme_system,
                    SettingsManager.Q25_THEME_LIGHT to R.string.q25_theme_light,
                    SettingsManager.Q25_THEME_DARK to R.string.q25_theme_dark,
                    SettingsManager.Q25_THEME_MATCHA to R.string.q25_theme_matcha
                ).forEach { (mode, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        SettingsManager.setQ25ThemeMode(context, mode)
                        themeMode = mode
                    }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = themeMode == mode, onClick = {
                            SettingsManager.setQ25ThemeMode(context, mode)
                            themeMode = mode
                        })
                        Text(stringResource(label), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (themeMode == null) {
                    Text(stringResource(R.string.q25_theme_legacy_notice),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
                SettingsCategoryRow(icon = Icons.Default.Palette,
                    title = stringResource(R.string.q25_advanced_theme_title),
                    description = stringResource(R.string.q25_advanced_theme_description),
                    onClick = onOpenAdvancedTheme)
            }
            entryIds.forEach { id ->
                SettingLinkRegistry.byId(id)?.let { entry ->
                    SettingsCategoryRow(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        title = context.getString(entry.titleRes),
                        description = entry.summaryRes?.let(context::getString),
                        linkId = entry.id,
                        onClick = { onOpenEntry(entry) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    linkId: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (description == null) 52.dp else 64.dp)
            .settingRow(linkId?.takeIf { enabled }, onClick.takeIf { enabled }),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconTint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGroupDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
