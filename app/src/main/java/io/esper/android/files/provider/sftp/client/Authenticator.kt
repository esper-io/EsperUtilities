package io.esper.android.files.provider.sftp.client

interface Authenticator {
    fun getAuthentication(authority: Authority): Authentication?
}
