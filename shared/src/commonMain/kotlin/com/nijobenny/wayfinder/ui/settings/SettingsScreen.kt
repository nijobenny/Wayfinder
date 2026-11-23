package com.nijobenny.wayfinder.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nijobenny.wayfinder.domain.model.ThemeMode
import com.nijobenny.wayfinder.domain.model.TravelMode
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.ui.components.BackGlyph
import com.nijobenny.wayfinder.ui.components.GlassPanel
import com.nijobenny.wayfinder.ui.components.IconBubble
import com.nijobenny.wayfinder.ui.components.SegmentedSelector
import com.nijobenny.wayfinder.ui.components.ToggleSwitch
import com.nijobenny.wayfinder.ui.components.bounceClick
import com.nijobenny.wayfinder.ui.components.collectAsStateLifecycle
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val colors = LocalWayfinderColors.current
    val s by viewModel.settings.collectAsStateLifecycle()
    var confirmation by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(onClick = onBack) { BackGlyph(colors.textPrimary) }
            Spacer(Modifier.width(14.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        }

        Section("Appearance") {
            Label("Theme")
            SegmentedSelector(
                options = ThemeMode.entries,
                selected = s.themeMode,
                label = { when (it) { ThemeMode.System -> "Auto"; ThemeMode.Light -> "☀️ Light"; ThemeMode.Dark -> "🌙 Dark" } },
                onSelect = viewModel::setTheme,
            )
        }

        Section("Navigation") {
            Label("Units")
            SegmentedSelector(Units.entries, s.units, { if (it == Units.Metric) "Kilometres" else "Miles" }, viewModel::setUnits)
            Spacer(Modifier.height(12.dp))
            Label("Default travel mode")
            SegmentedSelector(TravelMode.entries, s.defaultMode, { "${it.emoji} ${it.label}" }, viewModel::setDefaultMode)
            Spacer(Modifier.height(12.dp))
            ToggleRow("Avoid tolls", "Prefer free roads when driving", s.avoidTolls, viewModel::setAvoidTolls)
            ToggleRow("Avoid highways", "Take scenic local roads", s.avoidHighways, viewModel::setAvoidHighways)
            Spacer(Modifier.height(12.dp))
            Label("Demo drive speed")
            SegmentedSelector(listOf(1, 3, 6), s.demoSpeed, { "${it}×" }, viewModel::setDemoSpeed)
        }

        Section("Your data") {
            ActionRow("Clear search history") {
                viewModel.clearSearchHistory()
                confirmation = "Search history cleared"
            }
            ActionRow("Clear trip history") {
                viewModel.clearTrips()
                confirmation = "Trip history cleared"
            }
            confirmation?.let { Text("✓ $it", style = MaterialTheme.typography.labelLarge, color = Brand.Teal, modifier = Modifier.padding(top = 6.dp)) }
        }

        Section("About") {
            Text("Wayfinder 1.0", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(
                "Built with Kotlin Multiplatform & Compose Multiplatform by Nijo Benny.\n" +
                    "Map data © OpenStreetMap contributors · Tiles: OpenFreeMap · Search: Photon · Routing: Valhalla (FOSSGIS) · Rendering: MapLibre",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = LocalWayfinderColors.current
    Column {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = colors.textMuted, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 4.dp) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = LocalWayfinderColors.current.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalWayfinderColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        }
        ToggleSwitch(checked, onChange)
    }
}

@Composable
private fun ActionRow(title: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).bounceClick(onClick = onClick).padding(vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Brand.Rose)
    }
}
