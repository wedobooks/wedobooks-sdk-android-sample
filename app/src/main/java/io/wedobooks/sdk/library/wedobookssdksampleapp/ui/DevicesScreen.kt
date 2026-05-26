package io.wedobooks.sdk.library.wedobookssdksampleapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.wedobooks.sdk.WeDoBooksSdk
import io.wedobooks.sdk.R
import io.wedobooks.sdk.models.WdbDevice
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val lastUsedFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

@Composable
fun DevicesScreen(
    goBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val devicesData by remember { WeDoBooksSdk.devicesManager.devicesFlow() }.collectAsState(null)
    var removingDeviceId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = goBack
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = devicesData?.devices.orEmpty(),
                    key = { it.id }
                ) { device ->
                    DeviceItem(
                        device = device,
                        isCurrentDevice = devicesData?.currentDevice?.id == device.id,
                        isRemoving = removingDeviceId == device.id,
                        onRemove = {
                            coroutineScope.launch {
                                removingDeviceId = device.id
                                WeDoBooksSdk.devicesManager.removeDevice(device.id)
                                removingDeviceId = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: WdbDevice,
    isCurrentDevice: Boolean,
    isRemoving: Boolean,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        border = if (isCurrentDevice) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name.ifEmpty { "Unnamed device" },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Last used: ${lastUsedFormatter.format(device.lastUsed)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isCurrentDevice) {
                    Text(
                        text = "Current device",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            IconButton(
                enabled = !isRemoving,
                onClick = onRemove
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        }
    }
}
