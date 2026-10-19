package org.multipaz.samples.wallet.cmp

import org.multipaz.util.Logger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.multipaz.presentment.CredentialMatchSourceOpenID4VP
import org.multipaz.presentment.CredentialMatchSourceIso18013
import org.multipaz.presentment.CredentialPresentmentSelection
import org.multipaz.prompt.ConsentPromptDialogModel
import org.multipaz.prompt.PromptDialogModel
import org.multipaz.prompt.PromptDismissedException

private const val CONSENT_TAG = "ClaimConsent"

@Composable
internal fun ClaimConsentPromptDialog(
    model: PromptDialogModel<
        ConsentPromptDialogModel.ConsentPromptRequest,
        CredentialPresentmentSelection
    >,
    coroutineScope: CoroutineScope
) {
    val state by model.dialogState.collectAsState(
        PromptDialogModel.NoDialogState()
    )
    LaunchedEffect(state) {
    Logger.i(
        CONSENT_TAG,
        "Dialog state changed: ${state::class.simpleName}"
    )
    }
    if (state !is PromptDialogModel.DialogShownState) return

    @Suppress("UNCHECKED_CAST")
    val shownState = state as PromptDialogModel.DialogShownState<
        ConsentPromptDialogModel.ConsentPromptRequest,
        CredentialPresentmentSelection
    >

    val parameters = shownState.parameters
    val selection = remember(parameters) {
        parameters.credentialPresentmentData.select(
            parameters.preselectedDocuments
        )
    }

    val preselectionValid = remember(selection, parameters.preselectedDocuments) {
        val preselected = parameters.preselectedDocuments
        if (preselected.isEmpty()) {
            true
        } else {
            val selectedDocuments = selection.matches.map { it.credential.document }
            selectedDocuments.size == preselected.size &&
                selectedDocuments.toSet() == preselected.toSet()
        }
    }
    Logger.i(
        CONSENT_TAG,
        "Preselection check: " +
            "preselectedCount=${parameters.preselectedDocuments.size}, " +
            "selectedCount=${selection.matches.size}, " +
            "valid=$preselectionValid, " +
            "selectedDocumentIds=${selection.matches.map { it.credential.document.identifier }}"
    )

    LaunchedEffect(shownState, selection, preselectionValid) {
        if (!preselectionValid) {
            Logger.e(
                CONSENT_TAG,
                "Dismissing consent prompt because preselection is invalid"
            )
            shownState.resultChannel.close(PromptDismissedException())
        } else {
            Logger.i(
                CONSENT_TAG,
                "Preselection valid; notifying documents in focus"
            )
            parameters.onDocumentsInFocus(
                selection.matches.map { it.credential.document }
            )
        }
    }

    if (!preselectionValid) return

    val openIdMatches = remember(selection) {
        selection.matches.mapIndexedNotNull { index, match ->
            if (match.source is CredentialMatchSourceOpenID4VP) {
                index to match
            } else {
                null
            }
        }
    }

    val approved = remember(selection) {
        mutableStateMapOf<Int, Set<org.multipaz.request.RequestedClaim>>().apply {
            selection.matches.forEachIndexed { index, match ->
                put(index, match.claims.keys.toSet())
            }
        }
    }

    val validationMessage = remember(approved.toMap()) {
        openIdMatches.firstNotNullOfOrNull { (index, match) ->
            val source = match.source as CredentialMatchSourceOpenID4VP
            val query = source.credentialQuery
            val claims = approved[index].orEmpty()
            val requestedIds = query.claims.mapNotNull { it.id }.toSet()
            val approvedIds = claims.mapNotNull { it.id }.toSet()
            val claimSets = query.claimSets.map { it.claimIdentifiers.toSet() }

            val valid = if (query.claimSets.isEmpty()) {
                claims.containsAll(query.claims)
            } else {
                approvedClaimsSatisfyDcql(
                    requestedClaimIds = requestedIds,
                    claimSetIds = claimSets,
                    approvedClaimIds = approvedIds
                )
            }

            if (claims.isEmpty() || !valid) {
                "Select claims that satisfy at least one permitted request option."
            } else {
                null
            }
        }
    }

    val isoSelectionInvalid = remember(approved.toMap()) {
        selection.matches.mapIndexedNotNull { index, match ->
            if (match.source is CredentialMatchSourceIso18013 &&
                approved[index].orEmpty().isEmpty()
            ) index else null
        }.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = {
            coroutineScope.launch {
                shownState.resultChannel.close(PromptDismissedException())
            }
        },
        title = {
            Text("Review requested information")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Choose the information you agree to share. " +
                        "Only approved claims should be included in the response."
                )

                selection.matches.forEachIndexed { matchIndex, match ->
                    Text(
                        match.credential.document.displayName ?: "Digital credential",
                        style = MaterialTheme.typography.titleSmall
                    )

                    match.claims.forEach { (requestedClaim, claim) ->
                        val isOpenId = match.source is CredentialMatchSourceOpenID4VP
                        val isChecked = requestedClaim in approved[matchIndex].orEmpty()
                        Logger.i(
                            CONSENT_TAG,
                            "Checkbox: claim=${requestedClaim.id}, " +
                                "source=${match.source::class.simpleName}, " +
                                "enabled=$isOpenId, checked=$isChecked"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                enabled = true,
                                onCheckedChange = { checked ->
                                    Logger.i(
                                        CONSENT_TAG,
                                        "Checkbox tapped: claim=${requestedClaim.id}, checked=$checked"
                                    )
                                    val current = approved[matchIndex].orEmpty()
                                    approved[matchIndex] = if (checked) {

                                        current + requestedClaim
                                    } else {
                                        current - requestedClaim
                                    }
                                }
                            )
                            Column {
                                Text(claim.displayName)
                                Text(
                                    if (requestedClaim.id != null) {
                                        "Request ID: ${requestedClaim.id}"
                                    } else {
                                        "Requested attribute"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                if (validationMessage != null || isoSelectionInvalid) {
                    Text(
                        validationMessage
                            ?: "Select at least one ISO 18013 claim to continue.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = validationMessage == null && !isoSelectionInvalid,
                onClick = {
                    val filtered = try {
                        filterApprovedOpenId4VpClaims(
                            selection,
                            approved.toMap()
                        )
                    } catch (_: IllegalArgumentException) {
                        return@Button
                    } catch (_: IllegalStateException) {
                        return@Button
                    }

                    coroutineScope.launch {
                        parameters.onDocumentsInFocus(
                            filtered.matches.map { it.credential.document }
                        )
                        shownState.resultChannel.send(filtered)
                    }
                }
            ) {
                Text("Approve and continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        shownState.resultChannel.close(PromptDismissedException())
                    }
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
