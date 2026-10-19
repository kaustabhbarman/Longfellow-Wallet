package org.multipaz.samples.wallet.cmp

import kotlinx.io.bytestring.ByteString
import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.documenttype.DocumentTypeRepository
import org.multipaz.provisioning.DocumentProvisioningHandler
import org.multipaz.provisioning.ProvisioningMetadata
import org.multipaz.provisioning.CredentialMetadata
import org.multipaz.provisioning.Display
import org.multipaz.securearea.SecureArea
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.samples.wallet.cmp.ui.CardArtRenderer
import org.multipaz.util.Logger

class CustomDocumentProvisioningHandler(
    documentStore: DocumentStore,
    secureArea: SecureArea,
    private val documentTypeRepository: DocumentTypeRepository
) : DocumentProvisioningHandler(
    documentStore = documentStore,
    secureArea = secureArea
) {

    companion object {
        private const val TAG = "CustomDocumentProvisioning"
    }

    override suspend fun updateDocument(
        document: Document,
        display: Display?,
        documentAuthorizationData: ByteString?
    ) {
        // First let Multipaz perform its normal update.
        super.updateDocument(
            document = document,
            display = display,
            documentAuthorizationData = documentAuthorizationData
        )

        try {
            val credential = document
                .getCertifiedCredentials()
                .firstOrNull()

            if (credential !is MdocCredential) {
                Logger.w(
                    TAG,
                    "No certified MdocCredential found; keeping existing card art"
                )
                return
            }

            // The credential has already been certified at this point.
            val claims = credential.getClaims(
                documentTypeRepository = documentTypeRepository
            )
            val documentType =
                documentTypeRepository.getDocumentTypeForMdoc(credential.docType)

            Logger.i(
                "CustomDocumentProvisioning",
                "DCAPI CHECK: docType=${credential.docType}, " +
                    "documentTypeFound=${documentType != null}, " +
                    "namespaces=${documentType?.mdocDocumentType?.namespaces?.keys}"
            )

            for (claim in claims) {
                val dataElement = documentType
                    ?.mdocDocumentType
                    ?.namespaces
                    ?.get(claim.namespaceName)
                    ?.dataElements
                    ?.get(claim.dataElementName)

                Logger.i(
                    "CustomDocumentProvisioning",
                    "DCAPI CHECK: ${claim.dataElementName} -> " +
                        "dataElementFound=${dataElement != null}"
                )
            }
            Logger.i(
                TAG,
                "Rendering personalized card art from ${claims.size} claims"
            )

            val renderedCardArt = CardArtRenderer.render(claims)

            if (renderedCardArt != null) {
                document.edit {
                    cardArt = ByteString(renderedCardArt)
                }

                Logger.i(
                    TAG,
                    "Personalized card art stored: ${renderedCardArt.size} bytes"
                )
            } else {
                Logger.w(
                    TAG,
                    "CardArtRenderer returned null"
                )
            }

        } catch (e: Throwable) {
            Logger.e(
                TAG,
                "Failed to generate personalized card art",
                e
            )
        }
    }
}
