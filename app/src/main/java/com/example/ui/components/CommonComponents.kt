package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DiagnosticCardBorder
import com.example.ui.theme.DiagnosticDarkBg
import com.example.ui.theme.DiagnosticSurface
import com.example.ui.theme.DiagnosticSurfaceVariant
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanAccent
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DiagnosticDarkBg,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
fun TestStatusBadge(status: TestStatus) {
    val (bgColor, textColor, icon) = when (status) {
        TestStatus.RUNNING -> Triple(CyanAccent.copy(alpha = 0.2f), CyanAccent, Icons.Default.Refresh)
        TestStatus.PASSED -> Triple(GreenSuccess.copy(alpha = 0.15f), GreenSuccess, Icons.Default.CheckCircle)
        TestStatus.FAILED -> Triple(RedDanger.copy(alpha = 0.15f), RedDanger, Icons.Default.Close)
        TestStatus.WARNING -> Triple(AmberWarning.copy(alpha = 0.15f), AmberWarning, Icons.Default.Warning)
        TestStatus.SKIPPED -> Triple(DiagnosticSurfaceVariant, TextMuted, Icons.AutoMirrored.Filled.HelpOutline)
        TestStatus.UNTESTED -> Triple(DiagnosticSurfaceVariant, TextSecondary, Icons.AutoMirrored.Filled.HelpOutline)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun HealthScoreRing(
    score: Int,
    rating: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 140
) {
    val targetProgress = (score.coerceIn(0, 100)) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "health_progress"
    )

    val ringColor = when {
        score >= 90 -> GreenSuccess
        score >= 70 -> CyanAccent
        score >= 50 -> AmberWarning
        else -> RedDanger
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp.dp)
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(sizeDp.dp),
            color = DiagnosticSurfaceVariant,
            strokeWidth = 10.dp,
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(sizeDp.dp),
            color = ringColor,
            strokeWidth = 10.dp,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score",
                fontSize = (sizeDp / 4).sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "/ 100",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = rating,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }
    }
}

@Composable
fun DiagnosticCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        color = DiagnosticSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DiagnosticCardBorder, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (badge != null) {
                    badge()
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun TestPassFailButtons(
    onPass: () -> Unit,
    onFail: () -> Unit,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onSkip != null) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("skip_test_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text("Skip", fontSize = 13.sp)
            }
        }
        Button(
            onClick = onFail,
            modifier = Modifier
                .weight(1.2f)
                .height(48.dp)
                .testTag("fail_test_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = RedDanger,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Fail", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onPass,
            modifier = Modifier
                .weight(1.4f)
                .height(48.dp)
                .testTag("pass_test_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenSuccess,
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pass", fontWeight = FontWeight.Bold)
        }
    }
}
