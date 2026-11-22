package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SignalStatus
import com.example.model.TradingSignal
import com.example.ui.theme.TerminalTheme
import com.example.util.ShareCardType
import com.example.util.SignalCardImageGenerator

@Composable
fun ShareSignalCardDialog(
    signal: TradingSignal,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val colors = TerminalTheme.colors
    val context = LocalContext.current

    var cardType by remember(signal.id, signal.status) {
        mutableStateOf(
            if (signal.parsedStatus == SignalStatus.ACTIVE) {
                ShareCardType.ORDER_ENTRY
            } else {
                ShareCardType.RESULT_HIT
            }
        )
    }

    val previewBitmap = remember(signal, cardType, colors) {
        SignalCardImageGenerator.renderSignalCardBitmap(
            signal = signal,
            cardType = cardType,
            palette = colors
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shareable Status Card",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Generated inside app · Zero Firebase cost",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close card preview",
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ShareCardType.entries.forEachIndexed { idx, type ->
                        SegmentedButton(
                            selected = cardType == type,
                            onClick = { cardType = type },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = idx,
                                count = ShareCardType.entries.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = if (idx == 0) colors.emeraldContainer else colors.goldContainer,
                                activeContentColor = if (idx == 0) colors.emeraldBuy else colors.goldAccent,
                                inactiveContainerColor = colors.surfaceElevated,
                                inactiveContentColor = colors.textSecondary
                            ),
                            modifier = Modifier.testTag("card_type_${type.name.lowercase()}")
                        ) {
                            Text(type.label, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "${signal.symbol} shareable trading card",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1080f / 1350f)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            SignalCardImageGenerator.saveSignalCardToGallery(
                                context = context,
                                bitmap = previewBitmap,
                                signal = signal,
                                cardType = cardType
                            ).onSuccess { msg ->
                                onShowMessage(msg)
                            }.onFailure { err ->
                                onShowMessage(err.localizedMessage ?: "Failed to save image")
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.goldAccent.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldAccent),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("save_card_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PNG", maxLines = 1)
                    }

                    Button(
                        onClick = {
                            SignalCardImageGenerator.shareSignalCardImage(
                                context = context,
                                bitmap = previewBitmap,
                                signal = signal,
                                cardType = cardType
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldBuy,
                            contentColor = colors.background
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("share_card_whatsapp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Status", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
