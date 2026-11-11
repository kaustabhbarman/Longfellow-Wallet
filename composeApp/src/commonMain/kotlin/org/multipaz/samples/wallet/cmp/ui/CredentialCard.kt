package org.multipaz.samples.wallet.cmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import org.multipaz.claim.Claim
import org.multipaz.compose.claim.RenderClaimValue

@Composable
fun CredentialCard(
    claims: List<Claim>,
    credentialType: CredentialType,
    modifier: Modifier = Modifier
) {
    val data = CardData.fromClaims(claims)

    when (credentialType) {
        CredentialType.DRIVING_LICENCE ->
            DrivingLicenceIdentityCard(data, modifier)

        CredentialType.GERMAN_PERSONAL_ID ->
            GermanPersonalIdCard(data, modifier)

        CredentialType.EU_PERSONAL_ID ->
            EuPersonalIdCard(data, modifier)

        CredentialType.UNKNOWN ->
            GenericCredentialCard(data, modifier)
    }
}

@Composable
private fun GermanPersonalIdCard(
    data: CardData,
    modifier: Modifier
) {
    IdentityCard(
        title = "PERSONAL ID CARD",
        subtitle = "FEDERAL REPUBLIC OF GERMANY",
        accent = Color(0xFF172B45),
        marker = "germany",
        familyNameLabel = "FAMILY NAME",
        givenNameLabel = "GIVEN NAME",
        birthDateLabel = "DATE OF BIRTH",
        issueDateLabel = "ISSUED",
        expiryDateLabel = "EXPIRES",
        authorityLabel = "ISSUING AUTHORITY",
        data = data,
        modifier = modifier,
        cardColors = listOf(
            Color(0xFFE8E4D9),
            Color(0xFFDFDBCF),
            Color(0xFFC2BFBB)
        ),
        cardBorderColor = Color(0xFFB5AEA2)
    )
}

@Composable
private fun EuPersonalIdCard(
    data: CardData,
    modifier: Modifier
) {
    IdentityCard(
        title = "EU PERSONAL ID",
        subtitle = "EUROPEAN IDENTITY",
        accent = Color(0xFF173B70),
        marker = "europe",
        familyNameLabel = "FAMILY NAME",
        givenNameLabel = "GIVEN NAME",
        birthDateLabel = "DATE OF BIRTH",
        issueDateLabel = "ISSUED",
        expiryDateLabel = "EXPIRES",
        authorityLabel = "ISSUING AUTHORITY",
        data = data,
        modifier = modifier
    )
}

@Composable
private fun DrivingLicenceIdentityCard(
    data: CardData,
    modifier: Modifier
) {
    IdentityCard(
        title = "DIGITAL DRIVING LICENCE",
        subtitle = "EUROPEAN DIGITAL CREDENTIAL",
        accent = Color(0xFF173B70),
        marker = "europe",
        familyNameLabel = "FAMILY NAME",
        givenNameLabel = "GIVEN NAME",
        birthDateLabel = "DATE OF BIRTH",
        issueDateLabel = "DATE OF ISSUE",
        expiryDateLabel = "VALID UNTIL",
        authorityLabel = "ISSUING AUTHORITY",
        documentNumberLabel = "LICENCE NO.",
        data = data,
        modifier = modifier,
        cardColors = listOf(
            Color(0xFFE1BCC7),
            Color(0xFFAAC6C1),
            Color(0xFF96C5C7)
        ),
        cardBorderColor = Color(0xFF69959D)
    )
}

@Composable
private fun GenericCredentialCard(
    data: CardData,
    modifier: Modifier
) {
    IdentityCard(
        title = "DIGITAL CREDENTIAL",
        subtitle = "DIGITAL IDENTITY",
        accent = Color(0xFF394957),
        marker = "neutral",
        familyNameLabel = "FAMILY NAME",
        givenNameLabel = "GIVEN NAME",
        birthDateLabel = "DATE OF BIRTH",
        issueDateLabel = "ISSUED",
        expiryDateLabel = "EXPIRES",
        authorityLabel = "ISSUING AUTHORITY",
        data = data,
        modifier = modifier
    )
}

@Composable
private fun IdentityCard(
    title: String,
    subtitle: String,
    accent: Color,
    marker: String,
    familyNameLabel: String,
    givenNameLabel: String,
    birthDateLabel: String,
    issueDateLabel: String,
    expiryDateLabel: String,
    authorityLabel: String,
    documentNumberLabel: String? = null,
    data: CardData,
    modifier: Modifier,
    cardColors: List<Color> = listOf(
        Color(0xFFFAFAF8),
        Color(0xFFF0F0EC)
    ),
    cardBorderColor: Color = Color(0xFFD8DDE2)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.40f),
        shape = RoundedCornerShape(16.dp),
        color = cardColors.first(),
        border = BorderStroke(1.dp, cardBorderColor),
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardColors))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subtitle,
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = title,
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (marker) {
                    "germany" -> {
                        Column(
                            modifier = Modifier
                                .width(17.dp)
                                .height(20.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color.Black)
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFFDD0000))
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFFFFCE00))
                            )
                        }
                    }
                    "europe" -> {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    Color(0xFF173B70),
                                    RoundedCornerShape(50.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✦",
                                color = Color(0xFFFFD54F),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    Color(0xFFE8EBEF),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ID",
                                color = accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(7.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFD8DDE2))
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (data.portraitClaim != null) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 70.dp)
                            .border(
                                1.dp,
                                Color(0xFFD8DDE2),
                                RoundedCornerShape(5.dp)
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RenderClaimValue(
                            claim = data.portraitClaim.withPictureAttributeForCard()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IdentityCardField(familyNameLabel, data.familyNameClaim)
                    IdentityCardField(givenNameLabel, data.givenNameClaim)
                    IdentityCardField(birthDateLabel, data.birthDateClaim)
                    if (documentNumberLabel != null) {
                        IdentityCardField(documentNumberLabel, data.documentNumberClaim)
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IdentityCardField(
                    issueDateLabel,
                    data.issueDateClaim,
                    Modifier.weight(1f)
                )
                IdentityCardField(
                    expiryDateLabel,
                    data.expiryDateClaim,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(5.dp))

            IdentityCardField(
                authorityLabel,
                data.issuingAuthorityClaim
            )
        }
    }
}

@Composable
private fun IdentityCardField(
    label: String,
    claim: Claim?,
    modifier: Modifier = Modifier
) {
    if (claim == null) return

    Column(modifier = modifier) {
        Text(
            text = label,
            color = Color(0xFF65717D),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = claim.render(),
            color = Color(0xFF172B45),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
