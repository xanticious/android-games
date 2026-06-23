package com.xanticious.androidgames.model.games.loveletter

/** Each of the 8 unique card types in the Love Letter 16-card deck. */
enum class LoveLetterCard(val value: Int, val displayName: String, val effectDescription: String) {
    GUARD(1, "Guard", "Name a non-Guard card. If the target holds it, they are eliminated."),
    PRIEST(2, "Priest", "Look at an opponent's hand."),
    BARON(3, "Baron", "Compare hands with an opponent; the lower card is eliminated."),
    HANDMAID(4, "Handmaid", "You are immune to card effects until your next turn."),
    PRINCE(5, "Prince", "Choose any player (including yourself) to discard their hand and draw anew."),
    KING(6, "King", "Trade hands with an opponent."),
    COUNTESS(7, "Countess", "Must be played if you also hold the King or Prince."),
    PRINCESS(8, "Princess", "If you ever discard this card, you are eliminated.")
}
