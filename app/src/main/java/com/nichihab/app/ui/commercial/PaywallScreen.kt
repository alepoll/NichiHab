package com.nichihab.app.ui.commercial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import android.app.Activity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.nichihab.app.util.LocalStrings

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val context = LocalContext.current as? Activity
    val s = LocalStrings.current
    val isRestoring by viewModel.isRestoring.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    val prices by viewModel.productPrices.collectAsState()

    var isYearly by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        s.get("nichihab_pro"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        s.get("unlock_potential"),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    FeatureRow(Icons.Default.AllInclusive, s.get("unlimited_tasks"), s.get("ai_verify_unlimited"))
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureRow(Icons.Default.Shield, s.get("hard_mode_feature"), s.get("anti_uninstall_strict"))
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureRow(Icons.Default.CalendarMonth, s.get("weekly_calendar_feature"), s.get("plan_success"))

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlanOption(
                            label = s.get("monthly_label"),
                            price = prices.monthly,
                            description = s.get("cancel_anytime"),
                            isSelected = !isYearly,
                            onClick = { isYearly = false },
                            modifier = Modifier.weight(1f)
                        )
                        PlanOption(
                            label = s.get("yearly_label"),
                            price = prices.yearly,
                            description = s.get("save_with_annual"),
                            isSelected = isYearly,
                            onClick = { isYearly = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            context?.let {
                                if (isYearly) viewModel.launchYearlyPurchase(it)
                                else viewModel.launchMonthlyPurchase(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            if (isYearly) s.get("unlock_pro_yearly").replace("%s", prices.yearly)
                            else s.get("unlock_pro_monthly").replace("%s", prices.monthly),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.restorePurchases() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isRestoring,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(s.get("restore_purchases"))
                    }

                    if (restoreMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            s.get(restoreMessage!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onDismiss) {
                        Text(s.get("maybe_later"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanOption(
    label: String,
    price: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) Color(0xFFFFA500).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFFFFA500)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}