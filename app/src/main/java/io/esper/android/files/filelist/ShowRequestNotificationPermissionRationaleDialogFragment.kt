package io.esper.android.files.filelist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.esper.android.files.R
import io.esper.android.files.util.show

class ShowRequestNotificationPermissionRationaleDialogFragment : AppCompatDialogFragment() {
    private val listener: Listener
        get() = requireParentFragment() as Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(R.string.notification_permission_rationale_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                listener.onShowRequestNotificationPermissionRationaleResult(true)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                listener.onShowRequestNotificationPermissionRationaleResult(false)
            }
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        listener.onShowRequestNotificationPermissionRationaleResult(false)
    }

    companion object {
        fun show(fragment: Fragment) {
            ShowRequestNotificationPermissionRationaleDialogFragment().show(fragment)
        }
    }

    interface Listener {
        fun onShowRequestNotificationPermissionRationaleResult(shouldRequest: Boolean)
    }
}
