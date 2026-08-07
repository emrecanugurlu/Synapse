package com.cnsoftware.synapse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Expressive dilin tipografi anlayışı: başlıklar daha kalın ve sıkı,
 * gövde metni daha rahat satır aralığına sahip.
 * Sistem fontunu kullanır; ağırlık ve ölçekle karakter kazandırılır.
 */
private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val SynapseTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = lineHeightStyle,
    ),
    headlineMedium = TextStyle(
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
        lineHeightStyle = lineHeightStyle,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = lineHeightStyle,
    ),
    titleLarge = TextStyle(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = lineHeightStyle,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        lineHeightStyle = lineHeightStyle,
    ),

    // --- Expressive "emphasized" ölçeği ---
    // Odak isteyen anlar için (ekran başlıkları, seçili öğeler, boş durum
    // başlıkları). Normal karşılıklarından daha kalın ve daha sıkı.
    headlineSmallEmphasized = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
        lineHeightStyle = lineHeightStyle,
    ),
    titleLargeEmphasized = TextStyle(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = lineHeightStyle,
    ),
    titleMediumEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    labelLargeEmphasized = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp,
        lineHeightStyle = lineHeightStyle,
    ),
    bodyLargeEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        lineHeightStyle = lineHeightStyle,
    ),
)
