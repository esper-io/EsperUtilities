package io.esper.android.files.filelist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.esper.android.files.R
import io.esper.android.files.util.show

class ShowRequestStoragePermissionInSettingsRationaleDialogFragment : AppCompatDialogFragment() {
    private val listener: Listener
        get() = requireParentFragment() as Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(R.string.storage_permission_rationale_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                listener.onShowRequestStoragePermissionInSettingsRationaleResult(true)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                listener.onShowRequestStoragePermissionInSettingsRationaleResult(false)
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        listener.onShowRequestStoragePermissionInSettingsRationaleResult(false)
    }

    companion object {
        fun show(fragment: Fragment) {
            ShowRequestStoragePermissionInSettingsRationaleDialogFragment().show(fragment)
        }
    }

    interface Listener {
        fun onShowRequestStoragePermissionInSettingsRationaleResult(shouldRequest: Boolean)
    }
}
