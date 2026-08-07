package com.cnsoftware.synapse.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.cnsoftware.synapse.domain.LabelIcon

/**
 * `LabelIcon` -> çizilebilir ikon eşlemesi.
 *
 * Enum veritabanında adıyla saklandığı için eşleme tek bir yerde tutuluyor;
 * yeni ikon eklerken enum'a bir sabit, buraya bir satır yeterli.
 */
val LabelIcon.imageVector: ImageVector
    get() = when (this) {
        LabelIcon.LABEL -> Icons.Default.Label
        LabelIcon.STAR -> Icons.Default.Star
        LabelIcon.FAVORITE -> Icons.Default.Favorite
        LabelIcon.WORK -> Icons.Default.Work
        LabelIcon.HOME -> Icons.Default.Home
        LabelIcon.PERSON -> Icons.Default.Person
        LabelIcon.SHOPPING -> Icons.Default.ShoppingCart
        LabelIcon.FLIGHT -> Icons.Default.Flight
        LabelIcon.SCHOOL -> Icons.Default.School
        LabelIcon.FITNESS -> Icons.Default.FitnessCenter
        LabelIcon.RESTAURANT -> Icons.Default.Restaurant
        LabelIcon.MUSIC -> Icons.Default.MusicNote
        LabelIcon.IDEA -> Icons.Default.Lightbulb
        LabelIcon.MONEY -> Icons.Default.AttachMoney
        LabelIcon.EVENT -> Icons.Default.Event
        LabelIcon.CHECK -> Icons.Default.CheckCircle
    }
