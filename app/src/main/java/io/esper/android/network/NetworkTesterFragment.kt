package io.esper.android.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.esper.android.files.R
import io.esper.android.files.databinding.NetworkTesterFragmentBinding
import io.esper.android.files.util.GeneralUtils
import io.esper.android.network.model.ResultItem
import kotlinx.coroutines.launch

class NetworkTesterFragment : Fragment() {
    private lateinit var networkResultAdapter: NetworkResultAdapter
    private val resultItems = mutableListOf<ResultItem>()
    private lateinit var menuBinding: MenuBinding
    private lateinit var binding: NetworkTesterFragmentBinding
    private lateinit var progressBar: ProgressBar
    private var networkCheckTask: NetworkCheckTask? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View =
        NetworkTesterFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.title = getString(R.string.network_tester_title)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        view?.let { init(it) }
    }

    private fun init(view: View) {
        networkResultAdapter = NetworkResultAdapter(resultItems)
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewResults.adapter = networkResultAdapter

        progressBar = view.findViewById(R.id.progress_dialog)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menuBinding = MenuBinding.inflate(menu, inflater)

        if (GeneralUtils.hasActiveInternetConnection(requireContext())) {
            if (GeneralUtils.getTenant(requireContext()) == null) {
                GeneralUtils.showTenantDialog(requireContext()) { tenantInput ->
                    GeneralUtils.saveTenantForNetworkTester(requireContext(), tenantInput)
                    Thread.sleep(1000)
                    refreshData()
                }
                return
            } else {
                val tenantName = GeneralUtils.getTenant(requireContext())
                    ?.let { GeneralUtils.getTargetFromUrl(it) }
                tenantName?.let { GeneralUtils.saveTenantForNetworkTester(requireContext(), it) }
                Thread.sleep(1000)
                refreshData()
            }
        } else {
            GeneralUtils.showNoInternetDialog(requireContext(), true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private class MenuBinding private constructor(
        val menu: Menu, val refreshItem: MenuItem
    ) {
        companion object {
            fun inflate(menu: Menu, inflater: MenuInflater): MenuBinding {
                inflater.inflate(R.menu.network_tester_menu, menu)
                return MenuBinding(
                    menu, menu.findItem(R.id.action_refresh)
                )
            }
        }
    }

    private fun refreshData() {
        resultItems.clear()
        networkResultAdapter.notifyDataSetChanged()

        progressBar.visibility = View.VISIBLE
        menuBinding.refreshItem.isEnabled = false
        binding.toolbar.subtitle = null

        networkCheckTask?.cancel()
        networkCheckTask = NetworkCheckTask(networkResultAdapter, resultItems)
        networkCheckTask?.start()

        // Hide progress bar when all tasks are done
        networkCheckTask?.let { task ->
            lifecycleScope.launch {
                task.job?.join()
                progressBar.visibility = View.GONE
                menuBinding.refreshItem.isEnabled = true

                // Notify adapter and scroll to the last position
                networkResultAdapter.notifyDataSetChanged()
                networkResultAdapter.scrollToLastPosition(binding.recyclerViewResults)

                // Display success and failure counts
                val successCount = task.successCount
                val failureCount = task.failureCount
                binding.toolbar.subtitle = getString(
                    R.string.network_tester_subtitle, successCount, failureCount
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkCheckTask?.cancel()
    }
}
