package io.esper.android.files.fileproperties.permission

import androidx.annotation.DrawableRes
import io.esper.android.files.R
import io.esper.android.files.util.SelectionLiveData

class GroupListAdapter(
    selectionLiveData: SelectionLiveData<Int>
) : PrincipalListAdapter(selectionLiveData) {
    @DrawableRes
    override val principalIconRes: Int = R.drawable.people_icon_control_normal_24dp
}
