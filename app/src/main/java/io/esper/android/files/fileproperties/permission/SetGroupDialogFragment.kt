package io.esper.android.files.fileproperties.permission

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import java8.nio.file.Path
import io.esper.android.files.R
import io.esper.android.files.file.FileItem
import io.esper.android.files.filejob.FileJobService
import io.esper.android.files.provider.common.PosixFileAttributes
import io.esper.android.files.provider.common.PosixGroup
import io.esper.android.files.provider.common.toByteString
import io.esper.android.files.util.SelectionLiveData
import io.esper.android.files.util.putArgs
import io.esper.android.files.util.show
import io.esper.android.files.util.viewModels

class SetGroupDialogFragment : SetPrincipalDialogFragment() {
    override val viewModel: SetPrincipalViewModel by viewModels { { SetGroupViewModel() } }

    @StringRes
    override val titleRes: Int = R.string.file_properties_permission_set_group_title

    override fun createAdapter(selectionLiveData: SelectionLiveData<Int>): PrincipalListAdapter =
        GroupListAdapter(selectionLiveData)

    override val PosixFileAttributes.principal
        get() = group()!!

    override fun setPrincipal(path: Path, principal: PrincipalItem, recursive: Boolean) {
        val group = PosixGroup(principal.id, principal.name?.toByteString())
        FileJobService.setGroup(path, group, recursive, requireContext())
    }

    companion object {
        fun show(file: FileItem, fragment: Fragment) {
            SetGroupDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }
}
