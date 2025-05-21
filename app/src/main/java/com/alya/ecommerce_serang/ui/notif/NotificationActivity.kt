package com.alya.ecommerce_serang.ui.notif

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityNotificationBinding
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "NotificationActivity"

@AndroidEntryPoint
class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private val viewModel: NotifViewModel by viewModels()

    private lateinit var personalAdapter: PersonalNotificationAdapter
    private lateinit var storeAdapter: StoreNotificationAdapter

    private var hasStore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting NotificationActivity")
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAdapters()
        setupTabLayout()
        setupSwipeRefresh()
        setupObservers()

        // Load initial data
        Log.d(TAG, "onCreate: Checking if user has a store")
        viewModel.checkStoreUser()
        Log.d(TAG, "onCreate: Loading personal notifications")
        viewModel.getNotifList()

        // Show personal notifications by default
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupAdapters() {
        Log.d(TAG, "setupAdapters: Creating adapters")

        // Create LayoutManager explicitly
        val layoutManager = LinearLayoutManager(this)
        Log.d(TAG, "setupAdapters: Created LinearLayoutManager")

        // Personal notifications adapter
        personalAdapter = PersonalNotificationAdapter { notifItem ->
            // Handle personal notification click
            Log.d(TAG, "Personal notification clicked: id=${notifItem.id}, type=${notifItem.type}")
        }
        Log.d(TAG, "setupAdapters: Created personalAdapter")

        // Store notifications adapter
        storeAdapter = StoreNotificationAdapter { storeNotifItem ->
            // Handle store notification click
            Log.d(TAG, "Store notification clicked: id=${storeNotifItem.id}, type=${storeNotifItem.type}")
        }
        Log.d(TAG, "setupAdapters: Created storeAdapter")

        // Configure RecyclerView with explicit steps
        binding.recyclerViewNotif.setHasFixedSize(true)
        binding.recyclerViewNotif.layoutManager = layoutManager
        binding.recyclerViewNotif.adapter = personalAdapter

        Log.d(TAG, "setupAdapters: RecyclerView configured with personalAdapter")
        Log.d(TAG, "setupAdapters: RecyclerView visibility: ${binding.recyclerViewNotif.visibility == View.VISIBLE}")
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                Log.d(TAG, "Tab selected: position ${tab.position}")
                when (tab.position) {
                    0 -> {
                        Log.d(TAG, "Showing personal notifications tab")
                        binding.recyclerViewNotif.adapter = personalAdapter
                        showPersonalNotifications()
                    }
                    1 -> {
                        Log.d(TAG, "Showing store notifications tab, hasStore=$hasStore")
                        binding.recyclerViewNotif.adapter = storeAdapter
                        if (hasStore) {
                            viewModel.getNotifStoreList()
                        }
                        showStoreNotifications()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe refresh triggered, current tab: ${binding.tabLayout.selectedTabPosition}")
            when (binding.tabLayout.selectedTabPosition) {
                0 -> viewModel.getNotifList()
                1 -> {
                    if (hasStore) {
                        viewModel.getNotifStoreList()
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        // Observe checkStore to determine if user has a store
        viewModel.checkStore.observe(this) { hasStoreValue ->
            Log.d(TAG, "checkStore observed: $hasStoreValue")
            // Update the local hasStore variable
            hasStore = hasStoreValue

            // If we're on the store tab, update UI based on hasStore value
            if (binding.tabLayout.selectedTabPosition == 1) {
                if (hasStore) {
                    Log.d(TAG, "User has store, loading store notifications")
                    viewModel.getNotifStoreList()
                } else {
                    Log.d(TAG, "User doesn't have store, showing empty state")
                    showEmptyState("Anda belum memiliki toko", true)
                }
            }
        }

        // Observe personal notifications
        viewModel.notifList.observe(this) { result ->
            Log.d(TAG, "notifList observed: ${result.javaClass.simpleName}")
            binding.swipeRefreshLayout.isRefreshing = false

            if (binding.tabLayout.selectedTabPosition == 0) {
                when (result) {
                    is Result.Success -> {
                        val notifications = result.data
                        Log.d(TAG, "Personal notifications received: ${notifications?.size ?: 0}")
                        if (notifications.isNullOrEmpty()) {
                            showEmptyState("Belum Ada Notifikasi", false)
                        } else {
                            hideEmptyState()
                            // Ensure adapter is attached
                            if (binding.recyclerViewNotif.adapter != personalAdapter) {
                                Log.d(TAG, "Re-attaching personalAdapter to RecyclerView")
                                binding.recyclerViewNotif.adapter = personalAdapter
                            }
                            personalAdapter.submitList(notifications)
                            // Force a layout pass
                            binding.recyclerViewNotif.post {
                                Log.d(TAG, "Forcing layout pass on RecyclerView")
                                binding.recyclerViewNotif.requestLayout()
                            }
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error loading personal notifications", result.exception)
                        showEmptyState("Gagal memuat notifikasi", false)
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "Loading personal notifications")
                    }
                }
            }
        }

        // Observe store notifications
        viewModel.notifStoreList.observe(this) { result ->
            Log.d(TAG, "notifStoreList observed: ${result.javaClass.simpleName}")
            binding.swipeRefreshLayout.isRefreshing = false

            if (binding.tabLayout.selectedTabPosition == 1) {
                when (result) {
                    is Result.Success -> {
                        val notifications = result.data
                        Log.d(TAG, "Store notifications received: ${notifications?.size ?: 0}, hasStore=$hasStore")
                        if (!hasStore) {
                            showEmptyState("Anda belum memiliki toko", true)
                        } else if (notifications.isNullOrEmpty()) {
                            showEmptyState("Belum Ada Notifikasi Toko", false)
                        } else {
                            hideEmptyState()
                            // Ensure adapter is attached
                            if (binding.recyclerViewNotif.adapter != storeAdapter) {
                                Log.d(TAG, "Re-attaching storeAdapter to RecyclerView")
                                binding.recyclerViewNotif.adapter = storeAdapter
                            }
                            storeAdapter.submitList(notifications)
                            // Force a layout pass
                            binding.recyclerViewNotif.post {
                                Log.d(TAG, "Forcing layout pass on RecyclerView")
                                binding.recyclerViewNotif.requestLayout()
                            }
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error loading store notifications", result.exception)
                        showEmptyState("Gagal memuat notifikasi toko", false)
                    }
                    is Result.Loading -> {
                        Log.d(TAG, "Loading store notifications")
                    }
                }
            }
        }
    }

    private fun showPersonalNotifications() {
        Log.d(TAG, "showPersonalNotifications called")
        val result = viewModel.notifList.value

        if (result is Result.Success) {
            val notifications = result.data
            Log.d(TAG, "showPersonalNotifications: Success with ${notifications?.size ?: 0} notifications")
            if (notifications.isNullOrEmpty()) {
                showEmptyState("Belum Ada Notifikasi", false)
            } else {
                hideEmptyState()
                if (binding.recyclerViewNotif.adapter != personalAdapter) {
                    Log.d(TAG, "Re-attaching personalAdapter to RecyclerView")
                    binding.recyclerViewNotif.adapter = personalAdapter
                }
                personalAdapter.submitList(notifications)
                // DEBUG: Debug the RecyclerView state
                Log.d(TAG, "RecyclerView visibility: ${binding.recyclerViewNotif.visibility == View.VISIBLE}")
                Log.d(TAG, "RecyclerView adapter item count: ${personalAdapter.itemCount}")
            }
        } else if (result is Result.Error) {
            Log.e(TAG, "showPersonalNotifications: Error", result.exception)
            showEmptyState("Gagal memuat notifikasi", false)
        } else {
            Log.d(TAG, "showPersonalNotifications: No data yet, triggering fetch")
            // If we don't have data yet, trigger a fetch
            viewModel.getNotifList()
        }
    }

    private fun showStoreNotifications() {
        Log.d(TAG, "showStoreNotifications called, hasStore=$hasStore")
        if (!hasStore) {
            showEmptyState("Anda belum memiliki toko", true)
            return
        }

        val result = viewModel.notifStoreList.value

        if (result is Result.Success) {
            val notifications = result.data
            Log.d(TAG, "showStoreNotifications: Success with ${notifications?.size ?: 0} notifications")
            if (notifications.isNullOrEmpty()) {
                showEmptyState("Belum Ada Notifikasi Toko", false)
            } else {
                hideEmptyState()
                // Ensure adapter is attached
                if (binding.recyclerViewNotif.adapter != storeAdapter) {
                    Log.d(TAG, "Re-attaching storeAdapter to RecyclerView")
                    binding.recyclerViewNotif.adapter = storeAdapter
                }
                storeAdapter.submitList(notifications)
                // DEBUG: Debug the RecyclerView state
                Log.d(TAG, "RecyclerView visibility: ${binding.recyclerViewNotif.visibility == View.VISIBLE}")
                Log.d(TAG, "RecyclerView adapter item count: ${storeAdapter.itemCount}")
            }
        } else if (result is Result.Error) {
            Log.e(TAG, "showStoreNotifications: Error", result.exception)
            showEmptyState("Gagal memuat notifikasi toko", false)
        } else {
            Log.d(TAG, "showStoreNotifications: No data yet, triggering fetch")
            // If we don't have data yet, trigger a fetch
            viewModel.getNotifStoreList()
        }
    }

    private fun showEmptyState(message: String, showCreateStoreButton: Boolean) {
        Log.d(TAG, "showEmptyState: message='$message', showCreateStoreButton=$showCreateStoreButton")
        binding.swipeRefreshLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE

        // Set empty state message
        binding.tvEmptyTitle.text = message

        // Show "Create Store" button and description if user doesn't have a store
        if (showCreateStoreButton) {
            binding.tvEmptyDesc.visibility = View.VISIBLE
            binding.btnCreateStore.visibility = View.VISIBLE

            // Set up create store button click listener
            binding.btnCreateStore.setOnClickListener {
                Log.d(TAG, "Create store button clicked")
                // Navigate to create store screen
                // Intent to CreateStoreActivity
            }
        } else {
            binding.tvEmptyDesc.visibility = View.GONE
            binding.btnCreateStore.visibility = View.GONE
        }
    }

    private fun hideEmptyState() {
        Log.d(TAG, "hideEmptyState called")
        binding.swipeRefreshLayout.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        // Ensure recycler view is visible
        binding.recyclerViewNotif.visibility = View.VISIBLE
        Log.d(TAG, "hideEmptyState: Set RecyclerView visibility to VISIBLE")
    }
}