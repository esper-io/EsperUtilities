package io.esper.android.files.provider.linux

import java8.nio.file.attribute.UserPrincipalLookupService
import java8.nio.file.attribute.UserPrincipalNotFoundException
import io.esper.android.files.provider.common.ByteString
import io.esper.android.files.provider.common.PosixGroup
import io.esper.android.files.provider.common.PosixUser
import io.esper.android.files.provider.common.toByteString
import io.esper.android.files.provider.linux.syscall.Syscall
import io.esper.android.files.provider.linux.syscall.SyscallException
import java.io.IOException

internal object LinuxUserPrincipalLookupService : UserPrincipalLookupService() {
    @Throws(IOException::class)
    override fun lookupPrincipalByName(name: String): PosixUser =
        lookupPrincipalByName(name.toByteString())

    @Throws(IOException::class)
    fun lookupPrincipalByName(name: ByteString): PosixUser {
        val passwd = try {
            Syscall.getpwnam(name)
        } catch (e: SyscallException) {
            throw e.toFileSystemException(null)
        } ?: throw UserPrincipalNotFoundException(name.toString())
        return PosixUser(passwd.pw_uid, passwd.pw_name)
    }

    @Throws(IOException::class)
    fun lookupPrincipalById(id: Int): PosixUser =
        try {
            getUserById(id)
        } catch (e: SyscallException) {
            throw e.toFileSystemException(null)
        }

    @Throws(SyscallException::class)
    fun getUserById(uid: Int): PosixUser {
        val passwd = Syscall.getpwuid(uid)
        return PosixUser(uid, passwd?.pw_name)
    }

    @Throws(IOException::class)
    override fun lookupPrincipalByGroupName(group: String): PosixGroup =
        lookupPrincipalByGroupName(group.toByteString())

    @Throws(IOException::class)
    fun lookupPrincipalByGroupName(group: ByteString): PosixGroup {
        val groupStruct = try {
            Syscall.getgrnam(group)
        } catch (e: SyscallException) {
            throw e.toFileSystemException(null)
        } ?: throw UserPrincipalNotFoundException(group.toString())
        return PosixGroup(groupStruct.gr_gid, groupStruct.gr_name)
    }

    @Throws(IOException::class)
    fun lookupPrincipalByGroupId(groupId: Int): PosixGroup =
        try {
            getGroupById(groupId)
        } catch (e: SyscallException) {
            throw e.toFileSystemException(null)
        }

    @Throws(SyscallException::class)
    fun getGroupById(gid: Int): PosixGroup {
        val group = Syscall.getgrgid(gid)
        return PosixGroup(gid, group?.gr_name)
    }
}
