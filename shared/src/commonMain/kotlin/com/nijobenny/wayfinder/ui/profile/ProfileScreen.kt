package com.nijobenny.wayfinder.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.util.ClockFormat
import com.nijobenny.wayfinder.domain.util.Format
import com.nijobenny.wayfinder.ui.components.BackGlyph
import com.nijobenny.wayfinder.ui.components.CloseGlyph
import com.nijobenny.wayfinder.ui.components.GearGlyph
import com.nijobenny.wayfinder.ui.components.IconBubble
import com.nijobenny.wayfinder.ui.components.StatTile
import com.nijobenny.wayfinder.ui.components.bounceClick
import com.nijobenny.wayfinder.ui.components.collectAsStateLifecycle
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors
import kotlinx.coroutines.delay

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlace: (Place) -> Unit,
) {
    val colors = LocalWayfinderColors.current
    val state by viewModel.state.collectAsStateLifecycle()
    val units = state.settings.units

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ProfileHeader(state, onBack, onOpenSettings, viewModel::rename) }

        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(state.trips.size.toString(), "Trips")
                StatTile(Format.distance(state.totalMeters, units), "Travelled")
                StatTile(state.saved.size.toString(), "Saved")
            }
        }

        item { SectionTitle("Achievements") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.achievements) { a -> AchievementBadge(a) }
            }
        }

        item { SectionTitle("Saved places") }
        if (state.saved.isEmpty()) {
            item { EmptyHint("Tap ☆ on any place to save it here.") }
        } else {
            items(state.saved, key = { "saved-" + it.place.key }) { s ->
                ListRow(
                    emoji = s.emoji,
                    title = s.label,
                    subtitle = s.place.subtitle,
                    onClick = { onOpenPlace(s.place) },
                    trailing = { IconBubble(onClick = { viewModel.unsave(s.place) }, size = 32.dp) { CloseGlyph(colors.textMuted, size = 12.dp) } },
                )
            }
        }

        item { SectionTitle("Recent trips") }
        if (state.trips.isEmpty()) {
            item { EmptyHint("Your trips appear here. Try a Demo drive from the route planner!") }
        } else {
            items(state.trips) { t ->
                ListRow(
                    emoji = t.mode.emoji,
                    title = t.title,
                    subtitle = "${ClockFormat.date(t.finishedAtMillis)} · ${Format.distance(t.distanceMeters, units)} · ${Format.duration(t.durationSeconds)}",
                    onClick = null,
                    trailing = null,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(state: ProfileUiState, onBack: () -> Unit, onOpenSettings: () -> Unit, onRename: (String) -> Unit) {
    val transition = rememberInfiniteTransition(label = "header")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    var editing by remember { mutableStateOf(false) }
    var draft by remember(state.settings.displayName) { mutableStateOf(state.settings.displayName) }
    val level = 1 + (state.totalMeters / 10_000).toInt() + state.trips.size / 3

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(Brand.Teal, Brand.Sky, Brand.Indigo), start = Offset(size.width * shift * 0.4f, 0f), end = Offset(size.width, size.height)))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.7f, center = Offset(size.width * (0.9f - 0.1f * shift), 0f))
                drawCircle(Color.White.copy(alpha = 0.06f), radius = size.height * 0.45f, center = Offset(size.width * (0.1f + 0.1f * shift), size.height))
            }
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).bounceClick(onClick = onBack), contentAlignment = Alignment.Center) { BackGlyph(Color.White) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).bounceClick(onClick = onOpenSettings), contentAlignment = Alignment.Center) { GearGlyph(Color.White) }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(state.initials, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(12.dp))
                if (editing) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it.take(30) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(color = Color.White, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth(0.8f),
                    )
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f))
                            .bounceClick { onRename(draft); editing = false }.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                } else {
                    Text(state.settings.displayName, style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.bounceClick { editing = true })
                    Text("Level $level navigator · tap name to edit", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(a: Achievement) {
    val colors = LocalWayfinderColors.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(a.unlocked) {
        delay(150)
        pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 300f))
    }
    Column(
        Modifier
            .width(120.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = if (a.unlocked) 1f else 0.55f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(if (a.unlocked) Brand.Sky.copy(alpha = 0.14f) else colors.surfaceHigh)
            .border(1.dp, if (a.unlocked) Brand.Sky.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (a.unlocked) a.emoji else "🔒", fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(a.title, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(a.description, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = LocalWayfinderColors.current.textPrimary, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalWayfinderColors.current.textMuted, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun ListRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
) {
    val colors = LocalWayfinderColors.current
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glass)
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Sky.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

