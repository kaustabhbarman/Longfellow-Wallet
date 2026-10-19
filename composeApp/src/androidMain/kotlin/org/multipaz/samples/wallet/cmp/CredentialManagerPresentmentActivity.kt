package org.multipaz.samples.wallet.cmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import coil3.ImageLoader
import meinewallet.composeapp.generated.resources.Res
import android.util.Log
import org.multipaz.compose.digitalcredentials.CredentialManagerPresentmentActivity
import org.multipaz.compose.prompt.PromptDialogs
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.ConsentPromptDialogModel
import org.multipaz.prompt.getConsentPromptDialogModel

class CredentialManagerPresentmentActivity :
    CredentialManagerPresentmentActivity() {

    @Composable
    override fun PromptDialogContent(
        promptModel: AndroidPromptModel,
        imageLoader: ImageLoader
    ) {
        val consentPromptScope = rememberCoroutineScope()

        PromptDialogs(
            promptModel = promptModel,
            imageLoader = imageLoader,
            excludeTypes = listOf(
                ConsentPromptDialogModel.DialogType
            )
        )

        ClaimConsentPromptDialog(
            model = promptModel.getConsentPromptDialogModel(),
            coroutineScope = consentPromptScope
        )
    }

    override suspend fun getSettings(): Settings {
        val app = App.getInstance()
        app.init()
        Log.i("DCAPI_TRACE", "PromptDialogContent() composed")
        return Settings(
            source = app.presentmentSource,
            privilegedAllowList =
                Res.readBytes("files/privilegedUserAgents.json").decodeToString()
        )
    }
}
