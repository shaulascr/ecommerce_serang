package com.alya.ecommerce_serang.ui.notif

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.data.api.response.auth.NotifstoreItem
import com.alya.ecommerce_serang.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale


class StoreNotificationAdapter(
    private val onNotificationClick: (NotifstoreItem) -> Unit
) : ListAdapter<NotifstoreItem, StoreNotificationAdapter.ViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder: Creating ViewHolder")
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onNotificationClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        Log.d(TAG, "onBindViewHolder: Binding store notification at position $position, id=${item.id}")
        holder.bind(item)
    }

    override fun submitList(list: List<NotifstoreItem>?) {
        Log.d(TAG, "submitList: Received list with ${list?.size ?: 0} items")
        super.submitList(list)
    }

    class ViewHolder(
        private val binding: ItemNotificationBinding,
        private val onNotificationClick: (NotifstoreItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotifstoreItem) {
            binding.apply {
                tvNotificationType.text = notification.type
                tvTitle.text = notification.title
                tvDescription.text = notification.message

                // Format the date to show just the time
                formatTimeDisplay(notification.createdAt)

                // Handle notification click
                root.setOnClickListener {
                    Log.d(TAG, "ViewHolder: Store notification clicked, id=${notification.id}")
                    onNotificationClick(notification)
                }
            }
        }

        private fun formatTimeDisplay(createdAt: String) {
            try {
                // Parse the date with the expected format from API
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val date = inputFormat.parse(createdAt)
                date?.let {
                    binding.tvTime.text = outputFormat.format(it)
                }
            } catch (e: Exception) {
                // If date parsing fails, just display the raw value
                Log.e(TAG, "formatTimeDisplay: Error parsing date", e)
                binding.tvTime.text = createdAt
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotifstoreItem>() {
        override fun areItemsTheSame(oldItem: NotifstoreItem, newItem: NotifstoreItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotifstoreItem, newItem: NotifstoreItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object{
        private const val TAG = "StoreNotifAdapter"

    }
}