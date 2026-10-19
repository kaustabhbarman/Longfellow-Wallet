package org.multipaz.samples.wallet.cmp.ui

import org.multipaz.claim.Claim

expect object CardArtRenderer {
    fun render(claims: List<Claim>): ByteArray?
}
