package org.multipaz.samples.wallet.cmp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import org.multipaz.openid.dcql.DcqlClaimSet
import org.multipaz.openid.dcql.DcqlCredentialQuery
import org.multipaz.request.MdocRequestedClaim

class ClaimConsentFilterTest {

    @Test
    fun allClaimsAreRequiredWhenNoClaimSetsExist() {
        assertTrue(
            approvedClaimsSatisfyDcql(
                requestedClaimIds = setOf("name", "birth_date"),
                claimSetIds = emptyList(),
                approvedClaimIds = setOf("name", "birth_date")
            )
        )
    }

    @Test
    fun decliningAClaimFailsWhenNoClaimSetsExist() {
        assertFalse(
            approvedClaimsSatisfyDcql(
                requestedClaimIds = setOf("name", "birth_date"),
                claimSetIds = emptyList(),
                approvedClaimIds = setOf("name")
            )
        )
    }

    @Test
    fun oneCompleteAlternativeClaimSetIsEnough() {
        assertTrue(
            approvedClaimsSatisfyDcql(
                requestedClaimIds = setOf("name", "birth_date", "age_over_18"),
                claimSetIds = listOf(
                    setOf("name", "birth_date"),
                    setOf("name", "age_over_18")
                ),
                approvedClaimIds = setOf("name", "age_over_18")
            )
        )
    }

    @Test
    fun incompleteAlternativeClaimSetsAreRejected() {
        assertFalse(
            approvedClaimsSatisfyDcql(
                requestedClaimIds = setOf("name", "birth_date", "age_over_18"),
                claimSetIds = listOf(
                    setOf("name", "birth_date"),
                    setOf("name", "age_over_18")
                ),
                approvedClaimIds = setOf("name")
            )
        )
    }

    @Test
    fun unrequestedClaimsAreRejected() {
        assertFalse(
            approvedClaimsSatisfyDcql(
                requestedClaimIds = setOf("name"),
                claimSetIds = emptyList(),
                approvedClaimIds = setOf("name", "birth_date")
            )
        )
    }

    private fun requestedClaim(id: String) = MdocRequestedClaim(
        id = id,
        docType = "org.iso.18013.5.1.mDL",
        namespaceName = "org.iso.18013.5.1",
        dataElementName = id,
        intentToRetain = false
    )

    private fun testQuery(
        claims: List<MdocRequestedClaim>,
        claimSets: List<DcqlClaimSet> = emptyList()
    ) = DcqlCredentialQuery(
        id = "test-query",
        format = "mso_mdoc",
        meta = JsonObject(emptyMap()),
        mdocDocType = "org.iso.18013.5.1.mDL",
        claims = claims,
        claimSets = claimSets,
        claimIdToClaim = claims.associateBy { requireNotNull(it.id) }
    )

    @Test
    fun filteredQueryContainsOnlyApprovedClaims() {
        val name = requestedClaim("name")
        val birthDate = requestedClaim("birth_date")
        val ageOver18 = requestedClaim("age_over_18")
        val query = testQuery(
            claims = listOf(name, birthDate, ageOver18),
            claimSets = listOf(
                DcqlClaimSet(listOf("name", "birth_date")),
                DcqlClaimSet(listOf("name", "age_over_18"))
            )
        )

        val filtered = filterApprovedCredentialQuery(
            query = query,
            availableClaims = query.claims.toSet(),
            approvedClaims = setOf(name, ageOver18)
        )

        assertTrue(filtered.claims.toSet() == setOf(name, ageOver18))
        assertFalse(filtered.claims.contains(birthDate))
        assertTrue(filtered.claimSets == query.claimSets)
    }

    @Test
    fun filteredQueryRejectsDecliningRequiredClaimWithoutAlternatives() {
        val name = requestedClaim("name")
        val birthDate = requestedClaim("birth_date")
        val query = testQuery(claims = listOf(name, birthDate))

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            filterApprovedCredentialQuery(
                query = query,
                availableClaims = query.claims.toSet(),
                approvedClaims = setOf(name)
            )
        }
    }

    @Test
    fun filteredQueryRejectsClaimsOutsideAvailableMatch() {
        val name = requestedClaim("name")
        val birthDate = requestedClaim("birth_date")
        val query = testQuery(claims = listOf(name, birthDate))

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            filterApprovedCredentialQuery(
                query = query,
                availableClaims = setOf(name),
                approvedClaims = setOf(name, birthDate)
            )
        }
    }

}
