package com.nijobenny.wayfinder.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.wayfinder.domain.model.Category
import com.nijobenny.wayfinder.domain.model.LatLng
import com.nijobenny.wayfinder.domain.model.Place
import com.nijobenny.wayfinder.domain.model.SavedPlace
import com.nijobenny.wayfinder.domain.model.Units
import com.nijobenny.wayfinder.domain.util.Format
import com.nijobenny.wayfinder.domain.util.GeoMath
import com.nijobenny.wayfinder.ui.components.BackGlyph
import com.nijobenny.wayfinder.ui.components.Chip
import com.nijobenny.wayfinder.ui.components.CloseGlyph
import com.nijobenny.wayfinder.ui.components.GlassPanel
import com.nijobenny.wayfinder.ui.components.IconBubble
import com.nijobenny.wayfinder.ui.components.SearchGlyph
import com.nijobenny.wayfinder.ui.components.bounceClick
import com.nijobenny.wayfinder.ui.components.collectAsStateLifecycle
import com.nijobenny.wayfinder.ui.theme.Brand
import com.nijobenny.wayfinder.ui.theme.LocalWayfinderColors
import kotlinx.coroutines.delay

/** Why the search was opened: choosing where to go, or adding another stop to a route. */
enum class SearchPurpose { Destination, AddStop }

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    purpose: SearchPurpose,
    myLocation: LatLng?,
    units: Units,
    onPlaceChosen: (Place) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalWayfinderColors.current
    val state by viewModel.state.collectAsStateLifecycle()
    val recents by viewModel.recents.collectAsStateLifecycle()
    val saved by viewModel.saved.collectAsStateLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(myLocation) { viewModel.setNear(myLocation) }
    LaunchedEffect(Unit) {
        delay(250)
        focusRequester.requestFocus()
    }

    fun choose(place: Place) {
        focusManager.clearFocus()
        onPlaceChosen(place)
        viewModel.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Search field
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBubble(onClick = {
                focusManager.clearFocus()
                viewModel.clear()
                onClose()
            }) { BackGlyph(colors.textPrimary) }
            Spacer(Modifier.width(12.dp))
            GlassPanel(modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(26.dp), elevation = 6.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SearchGlyph(Brand.Sky, size = 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (state.query.isEmpty()) {
                            Text(
                                if (purpose == SearchPurpose.AddStop) "Add a stop…" else "Search places or addresses",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textMuted,
                            )
                        }
                        BasicTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(Brand.Sky),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        )
                    }
                    if (state.query.isNotEmpty()) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(50)).bounceClick { viewModel.onQueryChange("") }, contentAlignment = Alignment.Center) {
                            CloseGlyph(colors.textMuted, size = 14.dp)
                        }
                    }
                }
            }
        }

        // Categories
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            items(Category.entries) { category ->
                Chip(
                    text = "${category.emoji} ${category.label}",
                    selected = state.activeCategory == category,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onCategory(category)
                    },
                )
            }
        }

        AnimatedContent(
            targetState = state.results,
            contentKey = { it::class.simpleName },
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            modifier = Modifier.weight(1f),
            label = "results",
        ) { results ->
            when (results) {
                SearchResults.Idle -> IdleContent(saved, recents, myLocation, units, ::choose, viewModel::clearRecents)
                SearchResults.Loading -> LoadingList()
                SearchResults.Empty -> Message("🔍", "No places found", "Try a different name or check the spelling.")
                SearchResults.Error -> Message("📡", "Can't reach search", "Check your internet connection and try again.")
                is SearchResults.Ready -> ResultList(results.title, results.places, myLocation, units, ::choose)
            }
        }
    }
}

@Composable
private fun IdleContent(
    saved: List<SavedPlace>,
    recents: List<Place>,
    myLocation: LatLng?,
    units: Units,
    onChoose: (Place) -> Unit,
    onClearRecents: () -> Unit,
) {
    val colors = LocalWayfinderColors.current
    if (saved.isEmpty() && recents.isEmpty()) {
        Message("🧭", "Where would you like to go?", "Search for a place, or tap a category above to explore what's nearby.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (saved.isNotEmpty()) {
            item { SectionLabel("Saved") }
            itemsIndexed(saved, key = { _, it -> "saved-" + it.place.key }) { index, s ->
                PlaceRow(s.place, s.emoji, myLocation, units, index, onChoose)
            }
        }
        if (recents.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Recent", Modifier.weight(1f))
                    Text("Clear", style = MaterialTheme.typography.labelLarge, color = Brand.Sky, modifier = Modifier.bounceClick(onClick = onClearRecents).padding(8.dp))
                }
            }
            itemsIndexed(recents, key = { _, it -> "recent-" + it.key }) { index, place ->
                PlaceRow(place, "🕘", myLocation, units, index, onChoose)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text(
                "Tip: long-press anywhere on the map to drop a pin.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ResultList(title: String?, places: List<Place>, myLocation: LatLng?, units: Units, onChoose: (Place) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) item { SectionLabel(title) }
        itemsIndexed(places, key = { _, it -> it.key }) { index, place ->
            PlaceRow(place, categoryEmoji(place.category), myLocation, units, index, onChoose)
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalWayfinderColors.current.textMuted, modifier = modifier.padding(start = 4.dp, bottom = 2.dp))
}

/** One result. Rows slide and fade in with a small stagger based on [index]. */
@Composable
private fun PlaceRow(place: Place, emoji: String, myLocation: LatLng?, units: Units, index: Int, onChoose: (Place) -> Unit) {
    val colors = LocalWayfinderColors.current
    val appear = remember(place.key) { Animatable(0f) }
    LaunchedEffect(place.key) {
        delay(index.coerceAtMost(8) * 35L)
        appear.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }
    val distance = myLocation?.let { GeoMath.distanceMeters(it.latitude, it.longitude, place.latitude, place.longitude) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 24.dp.toPx()
            }
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glass)
            .bounceClick { onChoose(place) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Sky.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(place.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (place.subtitle.isNotBlank()) {
                Text(place.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (distance != null) {
            Spacer(Modifier.width(8.dp))
            Text(Format.distance(distance, units), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        }
    }
}

@Composable
private fun LoadingList() {
    val colors = LocalWayfinderColors.current
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha by transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(6) {
            Box(
                Modifier.fillMaxWidth().height(68.dp).graphicsLayer { this.alpha = alpha }
                    .clip(RoundedCornerShape(18.dp)).background(colors.surfaceHigh)
            )
        }
    }
}

@Composable
private fun Message(emoji: String, title: String, body: String) {
    val colors = LocalWayfinderColors.current
    val transition = rememberInfiniteTransition(label = "float")
    val dy by transition.animateFloat(-6f, 6f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 48.sp, modifier = Modifier.graphicsLayer { translationY = dy.dp.toPx() })
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

fun categoryEmoji(category: String?): String = when (category) {
    "cafe" -> "☕"
    "restaurant", "fast_food", "food_court" -> "🍽️"
    "fuel" -> "⛽"
    "atm", "bank" -> "🏧"
    "hospital", "clinic", "pharmacy" -> "🏥"
    "hotel", "guest_house", "hostel" -> "🏨"
    "school", "college", "university" -> "🎓"
    "park" -> "🌳"
    "station", "bus_station", "bus_stop" -> "🚉"
    "airport", "aerodrome" -> "✈️"
    else -> "📍"
}
