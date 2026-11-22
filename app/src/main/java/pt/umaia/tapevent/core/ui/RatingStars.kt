package pt.umaia.tapevent.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pt.umaia.tapevent.core.model.MAX_RATING

/**
 * Row of 5 stars. Pass onRatingChange to let the user pick (Feedback form);
 * leave it null to only show a rating (organizer and admin feedback lists).
 */
@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,
    starSize: Dp = 36.dp
) {
    Row(modifier) {
        for (value in 1..MAX_RATING) {
            val tint = if (value <= rating) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
            if (onRatingChange != null) {
                IconButton(onClick = { onRatingChange(value) }) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "$value de $MAX_RATING",
                        tint = tint,
                        modifier = Modifier.size(starSize)
                    )
                }
            } else {
                Icon(Icons.Filled.Star, contentDescription = null, tint = tint, modifier = Modifier.size(starSize))
            }
        }
    }
}
