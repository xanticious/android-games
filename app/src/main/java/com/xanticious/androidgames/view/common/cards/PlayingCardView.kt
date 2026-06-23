package com.xanticious.androidgames.view.common.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.CardColor
import com.xanticious.androidgames.ui.theme.CardBack
import com.xanticious.androidgames.ui.theme.CardBackPattern
import com.xanticious.androidgames.ui.theme.CardBlack
import com.xanticious.androidgames.ui.theme.CardBorder
import com.xanticious.androidgames.ui.theme.CardFace
import com.xanticious.androidgames.ui.theme.CardHighlight
import com.xanticious.androidgames.ui.theme.CardRed
import com.xanticious.androidgames.ui.theme.CardSlot

/**
 * Shared playing-card rendering used by every game in the Card category.
 *
 * Pure presentation: it draws a [Card] (face up or down) or an empty slot using
 * only the card palette tokens from `ui/theme/Color.kt`. All game rules and
 * selection logic live in the controllers; these composables just render state
 * and forward clicks.
 *
 * The standard playing-card aspect ratio is 5:7 (width:height).
 */
const val CardAspectRatio: Float = 5f / 7f

private fun pipColor(color: CardColor): Color = if (color == CardColor.RED) CardRed else CardBlack

/**
 * Renders a single [card]. Face-up cards show the rank and suit in opposite
 * corners; face-down cards show the patterned back. A [selected] card gains a
 * highlighted border. [onClick], when non-null, makes the card tappable.
 */
@Composable
fun PlayingCardView(
    card: Card,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(10)
    val border = if (selected) BorderStroke(3.dp, CardHighlight) else BorderStroke(1.dp, CardBorder)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .aspectRatio(CardAspectRatio)
            .then(clickModifier),
        shape = shape,
        color = if (card.faceUp) CardFace else CardBack,
        border = border
    ) {
        if (card.faceUp) {
            CardFaceContent(card)
        } else {
            CardBackContent()
        }
    }
}

/** Face-down card back; also used for stock piles and hidden hands. */
@Composable
fun CardBackView(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(10)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .aspectRatio(CardAspectRatio)
            .then(clickModifier),
        shape = shape,
        color = CardBack,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        CardBackContent()
    }
}

/**
 * An empty pile placeholder (foundation slot, empty tableau column, discard).
 * An optional [label] (e.g. a suit symbol or "K") hints what belongs here.
 */
@Composable
fun EmptyCardSlot(
    modifier: Modifier = Modifier,
    label: String? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(10)
    val border = if (highlighted) BorderStroke(3.dp, CardHighlight) else BorderStroke(1.dp, CardBorder)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .aspectRatio(CardAspectRatio)
            .then(clickModifier),
        shape = shape,
        color = CardSlot,
        border = border
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (label != null) {
                Text(text = label, color = CardBorder, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardFaceContent(card: Card) {
    val color = pipColor(card.color)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = card.rank.label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = card.suit.symbol, color = color, fontSize = 12.sp)
        }
        Text(
            text = card.suit.symbol,
            color = color,
            fontSize = 26.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = card.suit.symbol, color = color, fontSize = 12.sp)
            Text(text = card.rank.label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CardBackContent() {
    Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8),
            color = CardBackPattern,
            border = BorderStroke(1.dp, CardBack)
        ) {}
    }
}

/** Convenience for fixed-size card layouts (hands, HUDs). */
@Composable
fun PlayingCardView(card: Card, width: androidx.compose.ui.unit.Dp, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    PlayingCardView(card = card, modifier = Modifier.size(width, width / CardAspectRatio), selected = selected, onClick = onClick)
}
