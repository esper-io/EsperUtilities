package io.esper.android.files.provider.sftp.client

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.esper.android.files.provider.common.UriAuthority
import io.esper.android.files.util.takeIfNotEmpty
import net.schmizz.sshj.SSHClient

@Parcelize
data class Authority(
    val host: String,
    val port: Int,
    val username: String
) : Parcelable {
    fun toUriAuthority(): UriAuthority {
        val userInfo = username.takeIfNotEmpty()
        val uriPort = port.takeIf { it != DEFAULT_PORT }
        return UriAuthority(userInfo, host, uriPort)
    }

    override fun toString(): String = toUriAuthority().toString()

    companion object {
        const val DEFAULT_PORT = SSHClient.DEFAULT_PORT
    }
}
