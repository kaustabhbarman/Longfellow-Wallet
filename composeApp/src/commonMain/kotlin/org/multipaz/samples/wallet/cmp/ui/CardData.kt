package org.multipaz.samples.wallet.cmp.ui

import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.claim.Claim
import org.multipaz.claim.JsonClaim
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
            fun claimName(claim: Claim): String? =
                when (claim) {
                    is MdocClaim -> claim.dataElementName
                    is JsonClaim ->
                        claim.claimPath.lastOrNull()?.jsonPrimitive?.content
                }

            fun find(vararg names: String): Claim? =
                claims.firstOrNull { claim ->
                    claimName(claim) in names
                }

            return CardData(
                portraitClaim = find("portrait", "picture"),
                givenNameClaim = find("given_name"),
                familyNameClaim = find("family_name"),
                birthDateClaim = find("birth_date", "birthdate"),
                documentNumberClaim = find("document_number"),
                issueDateClaim = find("issue_date", "issuance_date"),
                expiryDateClaim = find("expiry_date"),
                issuingAuthorityClaim = find("issuing_authority"),
            )
        }
    }
}
