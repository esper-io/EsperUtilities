package io.esper.android.files.filelist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.esper.android.files.R
import io.esper.android.files.adapter.ContentAdapter
import io.esper.android.files.databinding.DlcFragmentBinding
import io.esper.android.files.ui.FixQueryChangeSearchView
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.viewmodel.DlcViewModel

class DlcFragment : Fragment() {

    private lateinit var binding: DlcFragmentBinding
    private lateinit var menuBinding: MenuBinding
    private lateinit var dlcViewModel: DlcViewModel
    private var mContentAdapter: ContentAdapter? = null
    private var mEmptyDialogView: RelativeLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = DlcFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.title = getString(R.string.file_list_action_dlc)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        init()
    }

    private fun init() {
        mEmptyDialogView = binding.layoutEmptyViewDialog

        mContentAdapter = ContentAdapter()
        binding.recyclerView.adapter = mContentAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(
            context, LinearLayoutManager.VERTICAL, false
        )

        dlcViewModel = ViewModelProvider(this).get(DlcViewModel::class.java)

        observeViewModel()
    }

    private fun observeViewModel() {
        dlcViewModel.allContent.observe(viewLifecycleOwner) { contentList ->
            if (contentList.isNotEmpty()) {
                binding.toolbar.subtitle = "${contentList.size} files"
                mContentAdapter?.setContentItems(requireContext(), contentList.toMutableList())
                setEmptyViewVisibility(View.GONE)
                setRecyclerViewVisibility(View.VISIBLE)
            } else {
                binding.toolbar.subtitle = getString(R.string.file_list_empty)
                setEmptyViewVisibility(View.VISIBLE)
                setRecyclerViewVisibility(View.GONE)
            }
        }

        dlcViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                startRefreshAnim()
            } else {
                stopRefreshAnim()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menuBinding = MenuBinding.inflate(menu, inflater)
        setUpSearchView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                mContentAdapter?.filter?.filter("")
                collapseSearchView()
                dlcViewModel.refreshContent()
                true
            }

            R.id.action_search -> {
                menuBinding.searchItem.expandActionView()
                true
            }

            R.id.sort_item -> {
                collapseSearchView()
                GeneralUtils.hideKeyboard(requireActivity())

                val sharedPrefManaged = requireContext().getSharedPreferences(
                    Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
                )
                val ascending = sharedPrefManaged.getBoolean(Constants.SORT_ASCENDING, true)
                mContentAdapter?.sortItems(!ascending)

                sharedPrefManaged.edit().putBoolean(Constants.SORT_ASCENDING, !ascending).apply()

                val sortMessage = if (!ascending) {
                    getString(R.string.sort_ascending)
                } else {
                    getString(R.string.sort_descending)
                }
                Toast.makeText(context, sortMessage, Toast.LENGTH_SHORT).show()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpSearchView() {
        val searchView = menuBinding.searchItem.actionView as FixQueryChangeSearchView
        menuBinding.searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                context?.let {
                    mContentAdapter?.setContentItems(
                        it, mContentAdapter?.mItemContentListOriginal
                    )
                }
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                GeneralUtils.hideKeyboard(requireActivity())
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (query.isEmpty()) {
                    context?.let {
                        mContentAdapter?.setContentItems(
                            it, mContentAdapter?.mItemContentListOriginal
                        )
                    }
                } else {
                    mContentAdapter?.filter?.filter(query)
                }
                return false
            }
        })
    }

    private fun collapseSearchView() {
        if (this::menuBinding.isInitialized && menuBinding.searchItem.isActionViewExpanded) {
            menuBinding.searchItem.collapseActionView()
        }
    }

    private class MenuBinding private constructor(
        val menu: Menu, val searchItem: MenuItem, val refreshItem: MenuItem, val sortItem: MenuItem
    ) {
        companion object {
            fun inflate(menu: Menu, inflater: MenuInflater): MenuBinding {
                inflater.inflate(R.menu.dlc_menu, menu)
                return MenuBinding(
                    menu,
                    menu.findItem(R.id.action_search),
                    menu.findItem(R.id.action_refresh),
                    menu.findItem(R.id.sort_item)
                )
            }
        }
    }

    private fun startRefreshAnim() {
        binding.progressDialog.visibility = View.VISIBLE
        disableRefreshBtn()
    }

    private fun stopRefreshAnim() {
        binding.progressDialog.visibility = View.GONE
        enableRefreshBtn()
    }

    private fun disableRefreshBtn() {
        if (this::menuBinding.isInitialized) {
            menuBinding.refreshItem.isEnabled = false
        }
    }

    private fun enableRefreshBtn() {
        if (this::menuBinding.isInitialized) {
            menuBinding.refreshItem.isEnabled = true
        }
    }

    private fun setEmptyViewVisibility(visibility: Int) {
        mEmptyDialogView?.visibility = visibility
    }

    private fun setRecyclerViewVisibility(visibility: Int) {
        binding.recyclerView.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        GeneralUtils.hideKeyboard(requireActivity())
    }
}
