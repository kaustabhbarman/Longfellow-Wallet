package org.multipaz.samples.wallet.cmp.ui

import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.sdjwt.credential.SdJwtVcCredential

enum class CredentialType {
    GERMAN_PERSONAL_ID,
    EU_PERSONAL_ID,
    DRIVING_LICENCE,
    UNKNOWN;

    companion object {
        private const val GERMAN_PID_VCT = "https://example.bmi.bund.de/credential/pid/1.0"

        private const val EU_PID_VCT =
            "urn:eudi:pid:1"

        private const val SD_JWT_DRIVING_LICENCE_VCT =
            "https://issuer.example.org/credentials/mDL-sd-jwt"

        private const val EU_PID_DOCTYPE =
            "eu.europa.ec.eudi.pid.1"

        private const val DRIVING_LICENCE_DOCTYPE =
            "org.iso.18013.5.1.mDL"

        fun fromCredential(credential: Any): CredentialType {
            return when (credential) {
                is MdocCredential -> when (credential.docType) {
                    DRIVING_LICENCE_DOCTYPE ->
                        DRIVING_LICENCE

                    EU_PID_DOCTYPE ->
                        EU_PERSONAL_ID

                    else -> UNKNOWN
                }

                is SdJwtVcCredential -> when (credential.vct) {
                    GERMAN_PID_VCT ->
                        GERMAN_PERSONAL_ID

                    EU_PID_VCT ->
                        EU_PERSONAL_ID

                    SD_JWT_DRIVING_LICENCE_VCT ->
                        DRIVING_LICENCE

                    else -> UNKNOWN
                }

                else -> UNKNOWN
            }
        }
    }
}
