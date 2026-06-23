package com.xanticious.androidgames.games.cards

import com.xanticious.androidgames.model.games.cards.Card
import com.xanticious.androidgames.model.games.cards.CardColor
import com.xanticious.androidgames.model.games.cards.Decks
import com.xanticious.androidgames.model.games.cards.Rank
import com.xanticious.androidgames.model.games.cards.Suit
import com.xanticious.androidgames.model.games.cards.deal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsTest {

    @Test
    fun standardDeck_hasFiftyTwoUniqueCards() {
        assertEquals(52, Decks.standard52.toSet().size)
    }

    @Test
    fun aceHighValue_isFourteen() {
        assertEquals(14, Rank.ACE.highValue)
    }

    @Test
    fun heartsAndDiamonds_areRed() {
        assertEquals(CardColor.RED, Suit.HEARTS.color)
        assertEquals(CardColor.RED, Suit.DIAMONDS.color)
    }

    @Test
    fun clubsAndSpades_areBlack() {
        assertEquals(CardColor.BLACK, Suit.CLUBS.color)
        assertEquals(CardColor.BLACK, Suit.SPADES.color)
    }

    @Test
    fun shuffledWithSeed_isDeterministic() {
        assertEquals(Decks.shuffled(42L), Decks.shuffled(42L))
    }

    @Test
    fun shuffledFaceDown_dealsAllFaceDown() {
        assertTrue(Decks.shuffledFaceDown(1L).all { !it.faceUp })
    }

    @Test
    fun deal_splitsCountFromFront() {
        val (dealt, rest) = Decks.standard52.deal(7)
        assertEquals(7, dealt.size)
        assertEquals(45, rest.size)
    }

    @Test
    fun faceDownThenFaceUp_restoresVisibility() {
        val card = Card(Rank.ACE, Suit.SPADES)
        assertFalse(card.faceDown().faceUp)
        assertTrue(card.faceDown().faceUp().faceUp)
    }

    @Test
    fun label_combinesRankAndSuitSymbol() {
        assertEquals("A\u2660", Card(Rank.ACE, Suit.SPADES).label)
    }
}
