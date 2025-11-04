package com.example.demoshemij.component

package com.ghtk.gam.composeui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ghtk.gam.composeui.basetheme.GamTheme

@Composable
fun Heading2Medium(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textPrimary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.title2,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
        textAlign = textAlign
    )
}

@Composable
fun Body1Regular(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textPrimary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.body1Regular,
    textAlign: TextAlign? = null,
    hintText: String? = null,
    hintTextColor: Color = GamTheme.colors.textHint7a7a7a
) {
    if (!hintText.isNullOrEmpty() && text.isEmpty()){
        Text(
            modifier = modifier,
            text = hintText,
            color = hintTextColor,
            style = style,
            overflow = overflow,
            maxLines = maxLines,
            textAlign = textAlign
        )
    }else{
        Text(
            modifier = modifier,
            text = text,
            color = color,
            style = style,
            overflow = overflow,
            maxLines = maxLines,
            textAlign = textAlign
        )
    }
}

@Composable
fun Body1Bold(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textPrimary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.large.copy(fontSize = 15.sp),
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
        textAlign = textAlign
    )
}

@Composable
fun Body1Medium(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textPrimary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.body1,
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
    )
}
@Composable
fun Body2Medium(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textPrimary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.body1.copy(
        fontSize = 13.sp
    ),
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
    )
}

@Composable
fun Body3Medium(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textPrimary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.body1.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.W500,
        fontFamily = FontFamily(
            Font(com.ghtk.gam.composeui.R.font.sf_pro_semibold)
        )
    ),
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
    )
}

@Composable
fun Body2Regular(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.textSecondary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.body2,
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
    )
}

@Composable
fun Body2TextLink(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = GamTheme.colors.primary,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.body2,
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
    )
}

@Composable
fun HeadlineH2(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Black,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.large,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
        textAlign = textAlign
    )
}

@Composable
fun BodyLargeSemibold(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Black,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = GamTheme.typography.bodyLarge,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        style = style,
        overflow = overflow,
        maxLines = maxLines,
        textAlign = textAlign
    )
}
