package io.esper.android.files.about

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.esper.android.files.databinding.AboutFragmentBinding
import io.esper.android.files.ui.LicensesDialogFragment
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.UploadDownloadUtils
import java.io.File

class AboutFragment : Fragment() {
    private lateinit var binding: AboutFragmentBinding
    private var clickCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = AboutFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Todo: un-hide the licenses layout if required.
        binding.licensesLayout.visibility = View.GONE
        binding.licensesLayout.setOnClickListener { LicensesDialogFragment.show(this) }
        binding.authorNameLayout.setOnClickListener {
            clickCount++
            if (clickCount == 7) {
                clickCount = 0
                Toast.makeText(
                    requireContext(), "App developed by: Karthik", Toast.LENGTH_SHORT
                ).show()
            }
        }

        val dpcLogFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "dpc_logs.zip"
        )

        // Rename the file to include the device name
        val deviceName = GeneralUtils.getDeviceName(requireContext())
        val newFileName = "${deviceName} - ${dpcLogFile.name}"
        val renamedFile = File(dpcLogFile.parent, newFileName)

        if (dpcLogFile.exists()) {
            // Rename the file
            if (dpcLogFile.renameTo(renamedFile)) {
                Log.i("AboutFragment", "File renamed to: $newFileName")
                binding.dpcLogsLayout.visibility = View.VISIBLE
            } else {
                Log.e("AboutFragment", "File renaming failed")
                binding.dpcLogsLayout.visibility = View.GONE
            }
        } else {
            binding.dpcLogsLayout.visibility = View.GONE
        }

        binding.dpcLogsUpload.setOnClickListener {
            Log.i("AboutFragment", "DPC logs Available")
            if (GeneralUtils.hasActiveInternetConnection(requireContext())) {
                Toast.makeText(requireContext(), "Uploading Esper Agent logs...", Toast.LENGTH_SHORT).show()
                UploadDownloadUtils.uploadFile(
                    renamedFile.path, renamedFile.name, requireContext(), viewLifecycleOwner, true
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "No active internet connection, try again after sometime.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}
