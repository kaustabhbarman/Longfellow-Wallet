package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.jsonPrimitive
import meinewallet.composeapp.generated.resources.Res
import meinewallet.composeapp.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
import org.multipaz.claim.MdocClaim
import org.multipaz.compose.claim.RenderClaimValue
import org.multipaz.documenttype.DocumentAttribute
import org.multipaz.documenttype.DocumentAttributeType
import org.multipaz.compose.document.DocumentModel
import org.multipaz.presentment.PresentmentSource
import org.multipaz.prompt.PromptModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentId: String,
    documentModel: DocumentModel,
    promptModel: PromptModel,
    presentmentSource: PresentmentSource,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val documentInfo = documentModel.documentInfos.collectAsState().value.find {
        it.document.identifier == documentId
    }

    var claims by remember {
        mutableStateOf<List<Claim>>(emptyList())
    }

    LaunchedEffect(documentId, documentInfo) {
        val document = documentInfo?.document ?: return@LaunchedEffect

        val credentials = document.getCertifiedCredentials()
        val credential = credentials.firstOrNull() ?: return@LaunchedEffect

        claims = credential.getClaims(
            presentmentSource.documentTypeRepository
        )
    }

    val portraitClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "portrait"
    }

    val givenNameClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "given_name"
    }

    val familyNameClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "family_name"
    }

    val birthDateClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "birth_date"
    }

    val documentNumberClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "document_number"
    }

    val issueDateClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "issue_date"
    }

    val expiryDateClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "expiry_date"
    }

    val issuingAuthorityClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "issuing_authority"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(
                                Res.string.more_options
                            )
                        )
                    }
                }
            )
        }
    ) { padding ->

        documentInfo?.let {

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                DigitalDrivingLicenceCard(
                    portraitClaim = portraitClaim,
                    givenNameClaim = givenNameClaim,
                    familyNameClaim = familyNameClaim,
                    birthDateClaim = birthDateClaim,
                    documentNumberClaim = documentNumberClaim,
                    issueDateClaim = issueDateClaim,
                    expiryDateClaim = expiryDateClaim,
                    issuingAuthorityClaim = issuingAuthorityClaim
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {

                    val jsonIgnoredClaims = setOf(
                        "iss",
                        "vct",
                        "iat",
                        "nbf",
                        "exp",
                        "cnf",
                        "status"
                    )

                    claims.forEachIndexed { index, claim ->

                        if (claim is JsonClaim) {
                            val name = claim.claimPath[0]
                                .jsonPrimitive
                                .content

                            if (jsonIgnoredClaims.contains(name)) {
                                return@forEachIndexed
                            }
                        }

                        Text(
                            text = claim.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        RenderClaimValue(
                            claim = claim
                        )

                        if (index != claims.lastIndex) {

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                        } else {

                            Spacer(
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * ---------------------------------------------------------
 * Digital Driving Licence Card - List<Claim> adapter
 * ---------------------------------------------------------
 *
 * WalletScreen loads the complete List<Claim>.
 *
 * This function extracts the individual claims and passes
 * them to the actual card renderer below.
 */
@Composable
fun DigitalDrivingLicenceCard(
    claims: List<Claim>,
    modifier: Modifier = Modifier
) {
    val portraitClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "portrait"
    }

    val givenNameClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "given_name"
    }

    val familyNameClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "family_name"
    }

    val birthDateClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "birth_date"
    }

    val documentNumberClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "document_number"
    }

    val issueDateClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "issue_date"
    }

    val expiryDateClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "expiry_date"
    }

    val issuingAuthorityClaim = claims.firstOrNull {
        it is MdocClaim && it.dataElementName == "issuing_authority"
    }

    DigitalDrivingLicenceCard(
        portraitClaim = portraitClaim,
        givenNameClaim = givenNameClaim,
        familyNameClaim = familyNameClaim,
        birthDateClaim = birthDateClaim,
        documentNumberClaim = documentNumberClaim,
        issueDateClaim = issueDateClaim,
        expiryDateClaim = expiryDateClaim,
        issuingAuthorityClaim = issuingAuthorityClaim,
        modifier = modifier
    )
}


/*
 * ---------------------------------------------------------
 * Digital Driving Licence Card - actual renderer
 * ---------------------------------------------------------
 *
 * This is the single visual implementation used by both
 * the Home screen and the Document Details screen.
 */
@Composable
fun DigitalDrivingLicenceCard(
    portraitClaim: Claim?,
    givenNameClaim: Claim?,
    familyNameClaim: Claim?,
    birthDateClaim: Claim?,
    documentNumberClaim: Claim?,
    issueDateClaim: Claim?,
    expiryDateClaim: Claim?,
    issuingAuthorityClaim: Claim?,
    modifier: Modifier = Modifier
) {
    // ---------------------------------------------------------
    // German / European inspired digital driving licence card.
    // Prototype design - not an official German licence.
    // ---------------------------------------------------------

    val darkBlue = Color(0xFF123A63)
    val mediumBlue = Color(0xFF1D5C91)
    val lightBlue = Color(0xFFE8F2F8)

    val white = Color.White
    val darkText = Color(0xFF17324D)
    val secondaryText = Color(0xFF5D7183)
    val borderColor = Color(0xFFB7C9D6)

    /*
     * ---------------------------------------------------------
     * RESPONSIVE CARD
     * ---------------------------------------------------------
     *
     * Internal design:
     *
     *      360 × 227 dp
     *
     * Everything inside this area is deliberately compact so
     * that the complete licence fits inside the card.
     */

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .aspectRatio(1.33f)
    ) {

        val designWidth = 360.dp
        val designHeight = 270.dp

        val scale = maxWidth.value / designWidth.value

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(
                        width = designWidth,
                        height = designHeight
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(lightBlue)
            ) {

                CompositionLocalProvider(
                    LocalContentColor provides darkText
                ) {

                    // =================================================
                    // BACKGROUND
                    // =================================================

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF5FAFD),
                                        Color(0xFFE4F0F7),
                                        Color(0xFFD7E9F3)
                                    )
                                )
                            )
                    )

                    // Large security circle
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.CenterEnd)
                            .offset(x = 90.dp)
                            .clip(CircleShape)
                            .background(
                                darkBlue.copy(alpha = 0.055f)
                            )
                    )

                    // Small security circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .align(Alignment.BottomStart)
                            .offset(
                                x = (-45).dp,
                                y = 45.dp
                            )
                            .clip(CircleShape)
                            .background(
                                mediumBlue.copy(alpha = 0.06f)
                            )
                    )

                    // =================================================
                    // MAIN CARD CONTENT
                    // =================================================

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                    ) {

                        // =================================================
                        // HEADER
                        // =================================================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // EU badge
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(darkBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "EU",
                                    color = white,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "BUNDESREPUBLIK DEUTSCHLAND",
                                    color = darkBlue,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )

                                Text(
                                    text = "DIGITALER FÜHRERSCHEIN",
                                    color = darkText,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // DE identifier
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = darkBlue.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(
                                        horizontal = 7.dp,
                                        vertical = 3.dp
                                    )
                            ) {
                                Text(
                                    text = "DE",
                                    color = darkBlue,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        // Header separator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(borderColor)
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        // =================================================
                        // PERSONAL INFORMATION
                        // =================================================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {

                            // -------------------------------------------------
                            // PORTRAIT
                            // -------------------------------------------------

                            Box(
                                modifier = Modifier
                                    .size(
                                        width = 60.dp,
                                        height = 68.dp
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clip(
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(Color.White)
                                    .padding(2.dp)
                            ) {

                                portraitClaim?.let { claim ->

                                    RenderClaimValue(
                                        claim = claim.withPictureAttribute()
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            // -------------------------------------------------
                            // NAME + LICENCE NUMBER
                            // -------------------------------------------------

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "SURNAME / FAMILY NAME",
                                    color = secondaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )

                                familyNameClaim?.let {
                                    RenderClaimValue(
                                        claim = it
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(2.dp)
                                )

                                Text(
                                    text = "GIVEN NAME",
                                    color = secondaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )

                                givenNameClaim?.let {
                                    RenderClaimValue(
                                        claim = it
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(2.dp)
                                )

                                Text(
                                    text = "LICENCE NO.",
                                    color = secondaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )

                                documentNumberClaim?.let {
                                    RenderClaimValue(
                                        claim = it
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        // =================================================
                        // DATES
                        // =================================================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            LicenceInfoBlock(
                                modifier = Modifier.weight(1f),
                                title = "DATE OF BIRTH",
                                claim = birthDateClaim
                            )

                            LicenceInfoBlock(
                                modifier = Modifier.weight(1f),
                                title = "DATE OF ISSUE",
                                claim = issueDateClaim
                            )

                            LicenceInfoBlock(
                                modifier = Modifier.weight(1f),
                                title = "VALID UNTIL",
                                claim = expiryDateClaim
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        // =================================================
                        // FOOTER
                        // =================================================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "ISSUING AUTHORITY",
                                    color = secondaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )

                                issuingAuthorityClaim?.let {
                                    RenderClaimValue(
                                        claim = it
                                    )
                                }
                            }

                            Text(
                                text = "mDL",
                                color = darkBlue,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/*
 * ---------------------------------------------------------
 * Date / information block
 * ---------------------------------------------------------
 */
@Composable
private fun LicenceInfoBlock(
    modifier: Modifier,
    title: String,
    claim: Claim?
) {
    Column(
        modifier = modifier
    ) {

        Text(
            text = title,
            color = Color(0xFF5D7183),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )

        claim?.let {
            RenderClaimValue(
                claim = it
            )
        }
    }
}

/*
 * ---------------------------------------------------------
 * Portrait rendering helper
 * ---------------------------------------------------------
 *
 * Multipaz normally determines how a claim should be
 * rendered from its DocumentAttribute.
 *
 * The portrait needs to be explicitly treated as a Picture
 * so RenderClaimValue() uses the image renderer.
 */
private fun Claim.withPictureAttribute(): Claim {

    if (attribute?.type == DocumentAttributeType.Picture) {
        return this
    }

    if (this is MdocClaim) {

        return copy(
            attribute = DocumentAttribute(
                type = DocumentAttributeType.Picture,
                identifier = dataElementName,
                displayName = displayName,
                description = "",
                icon = null,
                sampleValueMdoc = null,
                sampleValueJson = null,
                parentAttribute = null,
                embeddedAttributes = emptyList()
            )
        )
    }

    return this
}