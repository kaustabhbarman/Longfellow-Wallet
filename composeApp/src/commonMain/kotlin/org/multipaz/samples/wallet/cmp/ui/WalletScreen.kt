package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.activity
import meinewallet.composeapp.generated.resources.add_document_hint
import meinewallet.composeapp.generated.resources.app_icon
import meinewallet.composeapp.generated.resources.app_name
import meinewallet.composeapp.generated.resources.no_documents_added
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.multipaz.claim.Claim
import org.multipaz.compose.document.DocumentInfo
import org.multipaz.compose.document.DocumentModel
import org.multipaz.presentment.PresentmentSource
import org.multipaz.samples.wallet.cmp.SettingsModel
import org.multipaz.util.Logger

private const val TAG = "WalletScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    documentModel: DocumentModel,
    settingsModel: SettingsModel,
    presentmentSource: PresentmentSource,
    onDocumentSelected: (DocumentInfo) -> Unit,
    onActivity: () -> Unit = {},
    onCreateTestMdl: () -> Unit = {},
    onCredentialOfferScanned: (String) -> Unit = {},
) {

    /*
     * ---------------------------------------------------------
     * LOAD DOCUMENTS
     * ---------------------------------------------------------
     */

    val documentInfos by documentModel
        .documentInfos
        .collectAsState()

    /*
     * ---------------------------------------------------------
     * MAIN SCREEN
     * ---------------------------------------------------------
     */
    var showQrScanner by remember { mutableStateOf(false) }
    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {

                    Text(
                        text = stringResource(
                            Res.string.app_name
                        ),
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    Icon(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(32.dp),

                        painter =
                            painterResource(
                                Res.drawable.app_icon
                            ),

                        contentDescription = null,

                        tint = Color.Unspecified
                    )
                },

                actions = {
                    TextButton(onClick = { showQrScanner = true }) {
                        Text(text = "Scan QR")
                    }

                    TextButton(onClick = onActivity) {
                        Text(text = stringResource(Res.string.activity))
                    }
                }
            )
        }

    ) { padding ->

        /*
         * -----------------------------------------------------
         * EMPTY WALLET
         * -----------------------------------------------------
         */

        if (documentInfos.isEmpty()) {

            EmptyWalletState()

        } else {

            /*
             * -------------------------------------------------
             * INITIAL PAGE
             * -------------------------------------------------
             *
             * Restore the previously focused document if it
             * still exists.
             */

            val initialPage =
                documentInfos
                    .indexOfFirst {

                        it.identifier ==
                                settingsModel
                                    .currentlyFocusedDocumentId
                                    .value
                    }
                    .let { index ->

                        if (index >= 0) {
                            index
                        } else {
                            0
                        }
                    }

            /*
             * -------------------------------------------------
             * PAGER
             * -------------------------------------------------
             */

            val pagerState =
                rememberPagerState(

                    initialPage = initialPage,

                    pageCount = {
                        documentInfos.size
                    }
                )

            /*
             * -------------------------------------------------
             * CLAIM STORAGE
             * -------------------------------------------------
             *
             * Every document has its own claims.
             */

            val claimsByDocument =
                remember {

                    mutableStateMapOf<
                        String,
                        List<Claim>
                    >()
                }

            /*
             * -------------------------------------------------
             * LOAD CLAIMS
             * -------------------------------------------------
             */

            documentInfos.forEach { documentInfo ->

                LaunchedEffect(
                    documentInfo.identifier
                ) {

                    try {

                        val document =
                            documentInfo.document

                        val credentials =
                            document.getCertifiedCredentials()

                        val credential =
                            credentials.firstOrNull()

                        if (credential != null) {

                            val documentClaims =
                                credential.getClaims(
                                    presentmentSource
                                        .documentTypeRepository
                                )

                            claimsByDocument[
                                documentInfo.identifier
                            ] = documentClaims
                        }

                    } catch (e: Throwable) {

                        Logger.e(
                            TAG,
                            "Unable to load claims for document " +
                                    documentInfo.identifier,
                            e
                        )
                    }
                }
            }

            /*
             * -------------------------------------------------
             * UPDATE FOCUSED DOCUMENT
             * -------------------------------------------------
             *
             * Whenever the user swipes to another document,
             * remember it as the focused document.
             */

            LaunchedEffect(
                pagerState.currentPage,
                documentInfos
            ) {

                val currentPage =
                    pagerState.currentPage

                if (
                    currentPage >= 0 &&
                    currentPage < documentInfos.size
                ) {

                    val focusedDocument =
                        documentInfos[currentPage]

                    settingsModel
                        .currentlyFocusedDocumentId
                        .value =
                        focusedDocument.identifier
                }
            }

            /*
             * -------------------------------------------------
             * WALLET CONTENT
             * -------------------------------------------------
             */

            Column(

                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                /*
                 * -------------------------------------------------
                 * TOP SPACING
                 * -------------------------------------------------
                 *
                 * Small amount of space below the app bar.
                 */

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                /*
                 * -------------------------------------------------
                 * CARD AREA
                 * -------------------------------------------------
                 *
                 * weight() keeps the card area balanced on
                 * different phone screen sizes.
                 */

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    verticalArrangement =
                        Arrangement.Center,

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    /*
                     * -------------------------------------------------
                     * HORIZONTAL CARD PAGER
                     * -------------------------------------------------
                     */

                    HorizontalPager(

                        state =
                            pagerState,

                        modifier =
                            Modifier.fillMaxWidth(),

                        pageSpacing =
                            12.dp,

                        contentPadding =
                            PaddingValues(
                                horizontal = 10.dp
                            )
                    ) { page ->

                        val documentInfo =
                            documentInfos[page]

                        val claims =
                            claimsByDocument[
                                documentInfo.identifier
                            ]

                        /*
                         * -------------------------------------------------
                         * INDIVIDUAL CARD
                         * -------------------------------------------------
                         */

                        Box(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        onDocumentSelected(
                                            documentInfo
                                        )
                                    },

                            contentAlignment =
                                Alignment.Center
                        ) {

                            if (
                                claims != null &&
                                claims.isNotEmpty()
                            ) {

                                /*
                                 * Your custom German-style
                                 * Digital Driving Licence.
                                 */

                                DigitalDrivingLicenceCard(
                                    claims = claims
                                )

                            } else {

                                /*
                                 * Claims are still loading.
                                 */

                                Text(
                                    text =
                                        "Loading document...",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                /*
                 * -------------------------------------------------
                 * PAGE INDICATOR AREA
                 * -------------------------------------------------
                 */

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    /*
                     * -------------------------------------------------
                     * DOT INDICATOR
                     * -------------------------------------------------
                     */

                    if (documentInfos.size > 1) {

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.Center,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            documentInfos.forEachIndexed {
                                    index,
                                    _ ->

                                val selected =
                                    pagerState.currentPage ==
                                            index

                                Text(

                                    text =
                                        if (selected) {
                                            "●"
                                        } else {
                                            "○"
                                        },

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,

                                    color =
                                        if (selected) {

                                            MaterialTheme
                                                .colorScheme
                                                .primary

                                        } else {

                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        },

                                    modifier =
                                        Modifier.padding(
                                            horizontal = 3.dp
                                        )
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )

                        /*
                         * -------------------------------------------------
                         * DOCUMENT COUNT
                         * -------------------------------------------------
                         */

                        Text(

                            text =
                                "${pagerState.currentPage + 1} " +
                                        "of " +
                                        "${documentInfos.size}",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )

                        /*
                         * -------------------------------------------------
                         * SWIPE HINT
                         * -------------------------------------------------
                         */

                        Text(

                            text =
                                "Swipe to view your documents",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                                    .copy(
                                        alpha = 0.7f
                                    ),

                            textAlign =
                                TextAlign.Center
                        )
                    }
                }

                /*
                 * -------------------------------------------------
                 * BOTTOM SPACING
                 * -------------------------------------------------
                 */

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )
            }
        }
    }
    if (showQrScanner) {
        val uriHandler = LocalUriHandler.current

        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onPresentationUriScanned = { uri ->
                showQrScanner = false
                uriHandler.openUri(uri)
            },
            onCredentialOfferScanned = { uri ->
                showQrScanner = false
                onCredentialOfferScanned(uri)
            }
        )
    }
}


/*
 * -------------------------------------------------------------
 * EMPTY WALLET STATE
 * -------------------------------------------------------------
 */

@Composable
private fun EmptyWalletState() {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 32.dp
                ),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(

            text =
                stringResource(
                    Res.string.no_documents_added
                ),

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(

            text =
                stringResource(
                    Res.string.add_document_hint
                ),

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
                    .copy(
                        alpha = 0.7f
                    ),

            textAlign =
                TextAlign.Center
        )
    }
}