package org.multipaz.samples.wallet.cmp.ui

import org.multipaz.claim.Claim
import org.multipaz.claim.MdocClaim

data class CardData(
    val portraitClaim: Claim?,
    val givenNameClaim: Claim?,
    val familyNameClaim: Claim?,
    val birthDateClaim: Claim?,
    val documentNumberClaim: Claim?,
    val issueDateClaim: Claim?,
    val expiryDateClaim: Claim?,
    val issuingAuthorityClaim: Claim?,
) {
    companion object {
        fun fromClaims(claims: List<Claim>): CardData {
            fun find(name: String): Claim? =
                claims.firstOrNull {
                    it is MdocClaim && it.dataElementName == name
                }

            return CardData(
                portraitClaim = find("portrait"),
                givenNameClaim = find("given_name"),
                familyNameClaim = find("family_name"),
                birthDateClaim = find("birth_date"),
                documentNumberClaim = find("document_number"),
                issueDateClaim = find("issue_date"),
                expiryDateClaim = find("expiry_date"),
                issuingAuthorityClaim = find("issuing_authority"),
            )
        }
    }
}
