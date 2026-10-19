
package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.multipaz.compose.camera.CameraCaptureResolution
import org.multipaz.compose.camera.CameraSelection
import org.multipaz.compose.permissions.rememberCameraPermissionState
import org.multipaz.compose.qrcode.QrCodeScanner

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onPresentationUriScanned: (String) -> Unit,
    onCredentialOfferScanned: (String) -> Unit,
) {
    val cameraPermission = rememberCameraPermissionState()
    val coroutineScope = rememberCoroutineScope()

    var permissionRequested by remember { mutableStateOf(false) }
    var handled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.isGranted && !permissionRequested) {
            permissionRequested = true
            cameraPermission.launchPermissionRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan QR code") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    cameraPermission.isGranted -> {
                        Text("Point your camera at an issuer or verifier QR code.")

                        QrCodeScanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            cameraSelection = CameraSelection.DEFAULT_BACK_CAMERA,
                            captureResolution = CameraCaptureResolution.MEDIUM,
                            showCameraPreview = true,
                            onCodeScanned = { code ->
                                if (!handled && !code.isNullOrBlank()) {
                                    val normalized = code.trim()
                                    val scheme = normalized.substringBefore(":").lowercase()

                                    when (scheme) {
                                        "openid4vp", "haip-vp" -> {
                                            handled = true
                                            onPresentationUriScanned(normalized)
                                        }
                                        "openid-credential-offer", "haip-vci" -> {
                                            handled = true
                                            onCredentialOfferScanned(normalized)
                                        }
                                        "fido" -> {
                                            handled = true
                                            onPresentationUriScanned(normalized)
                                        }
                                        else -> {
                                            errorMessage = "Unsupported QR URI scheme: $scheme"
                                        }
                                    }
                                }
                            },
                        )
                    }

                    else -> {
                        Text(
                            "Camera permission is required to scan a verifier QR code."
                        )
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!cameraPermission.isGranted) {
                        coroutineScope.launch {
                            cameraPermission.launchPermissionRequest()
                        }
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(if (cameraPermission.isGranted) "Close" else "Grant camera access")
            }
        },
    )
}
