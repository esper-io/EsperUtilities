package io.esper.android.files.fileproperties.permission

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import java8.nio.file.Path
import io.esper.android.files.R
import io.esper.android.files.file.FileItem
import io.esper.android.files.filejob.FileJobService
import io.esper.android.files.provider.common.PosixFileAttributes
import io.esper.android.files.provider.common.PosixPrincipal
import io.esper.android.files.provider.common.PosixUser
import io.esper.android.files.provider.common.toByteString
import io.esper.android.files.util.SelectionLiveData
import io.esper.android.files.util.putArgs
import io.esper.android.files.util.show
import io.esper.android.files.util.viewModels

class SetOwnerDialogFragment : SetPrincipalDialogFragment() {
    override val viewModel: SetPrincipalViewModel by viewModels { { SetOwnerViewModel() } }

    @StringRes
    override val titleRes: Int = R.string.file_properties_permission_set_owner_title

    override fun createAdapter(selectionLiveData: SelectionLiveData<Int>): PrincipalListAdapter =
        UserListAdapter(selectionLiveData)

    override val PosixFileAttributes.principal: PosixPrincipal
        get() = owner()!!

    override fun setPrincipal(path: Path, principal: PrincipalItem, recursive: Boolean) {
        val owner = PosixUser(principal.id, principal.name?.toByteString())
        FileJobService.setOwner(path, owner, recursive, requireContext())
    }

    companion object {
        fun show(file: FileItem, fragment: Fragment) {
            SetOwnerDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }
}
