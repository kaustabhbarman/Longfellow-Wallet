package org.multipaz.samples.wallet.cmp

import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.request.RequestedClaim

/**
 * Filters OpenID4VP matches to claims explicitly approved by the user.
 *
 * The response generator reads credentialQuery.claims, so filtering only
 * CredentialPresentmentSetOptionMemberMatch.claims is not sufficient.
 *
 * Request validity, including DCQL claim-set requirements, must be checked
 * before this helper is called.
 */
internal fun filterApprovedOpenId4VpClaims(
    selection: CredentialPresentmentSelection,
    approvedClaimsByMatch: Map<Int, Set<RequestedClaim>>
): CredentialPresentmentSelection {
    val filteredMatches = selection.matches.mapIndexed { index, match ->
        val approvedClaims = requireNotNull(approvedClaimsByMatch[index]) {
            "Missing explicit claim-consent decision for match $index"
        }

        when (val source = match.source) {
            is CredentialMatchSourceOpenID4VP -> {
                val filteredQuery = filterApprovedCredentialQuery(
                    query = source.credentialQuery,
                    availableClaims = match.claims.keys,
                    approvedClaims = approvedClaims
                )

                match.copy(
                    claims = match.claims.filterKeys { it in approvedClaims },
                    source = source.copy(credentialQuery = filteredQuery)
                )
            }

            is CredentialMatchSourceIso18013 -> {
                require(approvedClaims.isNotEmpty()) {
                    "At least one ISO 18013 claim must be approved"
                }
                require(approvedClaims.all { it in match.claims.keys }) {
                    "Approved ISO 18013 claims must come from the selected credential match"
                }

                // The ISO 18013 response generator uses match.claims.keys
                // for response claims and ZKP claim selection.
                // Preserve the source and transaction data.
                match.copy(
                    claims = match.claims.filterKeys { it in approvedClaims }
                )
            }
        }
    }

    return selection.copy(matches = filteredMatches)
}

/**
 * Returns whether the user's approved claim IDs satisfy the DCQL request.
 *
 * Without claim sets, every requested claim must be approved.
 * With claim sets, at least one complete alternative must be approved.
 */
internal fun approvedClaimsSatisfyDcql(
    requestedClaimIds: Set<String>,
    claimSetIds: List<Set<String>>,
    approvedClaimIds: Set<String>
): Boolean {
    if (!approvedClaimIds.all { it in requestedClaimIds }) {
        return false
    }

    return if (claimSetIds.isEmpty()) {
        approvedClaimIds.containsAll(requestedClaimIds)
    } else {
        claimSetIds.any { claimSet ->
            claimSet.isNotEmpty() && approvedClaimIds.containsAll(claimSet)
        }
    }
}


/**
 * Validates explicit claim consent and returns the query used for presentation.
 * Original DCQL claim sets are retained so the request's alternatives remain intact.
 */
internal fun filterApprovedCredentialQuery(
    query: org.multipaz.openid.dcql.DcqlCredentialQuery,
    availableClaims: Set<RequestedClaim>,
    approvedClaims: Set<RequestedClaim>
): org.multipaz.openid.dcql.DcqlCredentialQuery {
    require(approvedClaims.isNotEmpty()) {
        "No claims approved; cancel presentation instead"
    }
    require(approvedClaims.all { it in availableClaims }) {
        "Approved claims must come from the selected credential match"
    }
    require(approvedClaims.all { it in query.claims }) {
        "Approved claims must come from the original OpenID4VP query"
    }

    if (query.claimSets.isEmpty()) {
        require(approvedClaims.containsAll(query.claims)) {
            "All requested claims must be approved when DCQL claim_sets is absent"
        }
    } else {
        val requestedIds = query.claims.mapNotNull { it.id }.toSet()
        val approvedIds = approvedClaims.mapNotNull { it.id }.toSet()
        val claimSetIds = query.claimSets.map { it.claimIdentifiers.toSet() }

        require(approvedClaimsSatisfyDcql(requestedIds, claimSetIds, approvedIds)) {
            "Approved claims must satisfy at least one complete DCQL claim set"
        }
    }

    val filteredQuery = query.copy(
        claims = query.claims.filter { it in approvedClaims }
    )
    require(filteredQuery.claims.size == approvedClaims.size) {
        "Some approved claims are not present in the original OpenID4VP query"
    }

    return filteredQuery
}
