package io.esper.android.network

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.esper.android.files.R
import io.esper.android.files.databinding.NetworkTesterFragmentBinding
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.android.network.model.UrlAndPortItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkTesterFragment : Fragment(), GeneralUtils.BaseStackNameCallback {
    private lateinit var networkResultAdapter: NetworkResultAdapter
    private val urlAndPortItems = mutableListOf<UrlAndPortItem>()
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
        val sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        if (!sharedPrefManaged!!.getBoolean(
                Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_NETWORK_TESTER, false
            )
        ) {
            activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        setHasOptionsMenu(true)
        view?.let { init(it) }
    }

    private fun init(view: View) {
        networkResultAdapter = NetworkResultAdapter(urlAndPortItems)
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewResults.adapter = networkResultAdapter

        progressBar = view.findViewById(R.id.progress_dialog)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menuBinding = MenuBinding.inflate(menu, inflater)

        initTenant()
    }

    private fun initTenant() {
        progressBar.visibility = View.GONE
        menuBinding.refreshItem.isEnabled = false
        if (GeneralUtils.hasActiveInternetConnection(requireContext())) {
            if (GeneralUtils.useCustomTenantForNetworkTester(requireContext())) {
                GeneralUtils.showTenantDialog(requireContext()) { tenantInput, streamerAvailable ->
                    if (tenantInput.isEmpty()) {
                        initTenant()
                        return@showTenantDialog
                    }
                    GeneralUtils.saveTenantForNetworkTester(requireContext(), tenantInput)
                    GeneralUtils.setStreamerAvailability(requireContext(), streamerAvailable)
                    progressBar.visibility = View.VISIBLE
                    GeneralUtils.fetchAndStoreBaseStackName(requireContext(), tenantInput, this)
                }
            } else {
                val tenant = context?.let { GeneralUtils.getTenant(it) }
                val tenantInput = tenant?.let { GeneralUtils.getTargetFromUrl(it) }
                tenantInput?.let { GeneralUtils.saveTenantForNetworkTester(requireContext(), it) }
                GeneralUtils.setStreamerAvailability(requireContext(), false)
                progressBar.visibility = View.VISIBLE
                tenantInput?.let {
                    GeneralUtils.fetchAndStoreBaseStackName(
                        requireContext(), it, this
                    )
                }
            }
        } else {
            menuBinding.refreshItem.isEnabled = false
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
        urlAndPortItems.clear()
        networkResultAdapter.notifyDataSetChanged()

        progressBar.visibility = View.VISIBLE
        menuBinding.refreshItem.isEnabled = false
        binding.toolbar.subtitle = null

        val itemsToCheck = listOf(
            if (GeneralUtils.isStreamerAvailable(requireContext())) {
                UrlAndPortItem("streamer.esper.io", 443, false)
            } else {
                UrlAndPortItem("firebasecrashlyticssymbols.googleapis.com", 443, false)
            },
            if (!GeneralUtils.getBaseStackName(requireContext()).isNullOrEmpty()) {
                UrlAndPortItem(
                    "scapi-${GeneralUtils.getBaseStackName(requireContext())}-static-and-media-files.s3.amazonaws.com",
                    443,
                    false
                )
            } else {
                UrlAndPortItem("firebaseinstallations.googleapis.com", 443, false)
            },
            UrlAndPortItem("remoteviewer.esper.cloud", 3478, false),
            UrlAndPortItem("services.shoonyacloud.com", 443, false),
            UrlAndPortItem("mqtt.shoonyacloud.com", 1883, false),
            UrlAndPortItem("turn.shoonyacloud.com", 3478, false),
            UrlAndPortItem("mqtt-telemetry-prod.esper.cloud", 1883, false),
            UrlAndPortItem("downloads.esper.cloud", 443, false),
            UrlAndPortItem("dpcdownloads.esper.cloud", 443, false),
            UrlAndPortItem("eea-services.esper.cloud", 443, false),
            UrlAndPortItem(
                "${GeneralUtils.getTenantForNetworkTester(requireContext())}-api.esper.cloud",
                443,
                false
            ),
            UrlAndPortItem("authn2.esper.cloud", 443, false),
            UrlAndPortItem("id.esper.cloud", 443, false),
            UrlAndPortItem("ping.esper.cloud", 443, false),
            UrlAndPortItem("mqtt.esper.cloud", 443, false),
            UrlAndPortItem("statserv.esper.cloud", 443, false),
            UrlAndPortItem("id.esper.cloud", 443, false),
            UrlAndPortItem("onboarding.esper.cloud", 443, false),
            UrlAndPortItem("ota.esper.cloud", 443, false),
            UrlAndPortItem("eea-services.esper.cloud", 443, false),
            UrlAndPortItem("ota.esper.io", 443, false),
            UrlAndPortItem("shoonya-firebase.firebaseio.com", 443, false),
            UrlAndPortItem("crashlyticsreports-pa.googleapis.com", 443, false),
            UrlAndPortItem("8.8.8.8", 443, false),
            UrlAndPortItem("ip-api.com", 80, false)
        )

        networkCheckTask?.cancel()
        networkCheckTask = NetworkCheckTask(networkResultAdapter, urlAndPortItems, itemsToCheck)
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
                val successCount = task.successCountValue
                val failureCount = task.failureCountValue
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

    override fun onBaseStackNameFetched(baseStackName: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            refreshData()
        }
    }

    override fun onError(e: Exception) {
        Log.e(Constants.NetworkTesterFragmentTag, "Error fetching base stack name", e)
        lifecycleScope.launch(Dispatchers.Main) {
            if (e.message?.contains("No data found") == true) {
                Toast.makeText(context, R.string.tenant_not_available, Toast.LENGTH_LONG).show()
                initTenant()
            } else {
                progressBar.visibility = View.GONE
                Toast.makeText(context, R.string.stack_details_not_available, Toast.LENGTH_LONG).show()
                GeneralUtils.showStackDialog(requireContext()) { stackInput ->
                    GeneralUtils.setBaseStackName(requireContext(), stackInput)
                    refreshData()
                }
            }
        }
    }
}
