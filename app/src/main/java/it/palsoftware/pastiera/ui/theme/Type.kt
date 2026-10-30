package it.palsoftware.pastiera.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import it.palsoftware.pastiera.R

val EBGaramondFontFamily = FontFamily(
    Font(R.font.eb_garamond, weight = FontWeight.Normal),
    Font(R.font.eb_garamond, weight = FontWeight.Medium)
)

val NotoSerifJPFontFamily = FontFamily(
    Font(R.font.noto_serif_jp, weight = FontWeight.Normal),
    Font(R.font.noto_serif_jp, weight = FontWeight.Medium),
    Font(R.font.noto_serif_jp, weight = FontWeight.Bold)
)

fun typographyForLanguage(japanese: Boolean) = Typography.copy(
    displayLarge = Typography.displayLarge.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    displayMedium = Typography.displayMedium.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    displaySmall = Typography.displaySmall.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    headlineLarge = Typography.headlineLarge.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    headlineSmall = Typography.headlineSmall.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    titleLarge = Typography.titleLarge.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    titleMedium = Typography.titleMedium.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    titleSmall = Typography.titleSmall.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily, fontWeight = FontWeight.Medium),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily, fontWeight = FontWeight.Medium),
    bodySmall = Typography.bodySmall.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily, fontWeight = FontWeight.Medium),
    labelLarge = Typography.labelLarge.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    labelMedium = Typography.labelMedium.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily),
    labelSmall = Typography.labelSmall.copy(fontFamily = if (japanese) NotoSerifJPFontFamily else EBGaramondFontFamily)
)

/** Display style reserved for the Latin home title and future app wordmark use. */
val Q25TitleTextStyle = TextStyle(
    fontFamily = EBGaramondFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.01.sp
)

// Compact type scale shared by home and settings screens.
val Typography = Typography(
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
)
