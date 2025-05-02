package com.alya.ecommerce_serang.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.databinding.ItemMessageReceivedBinding
import com.alya.ecommerce_serang.databinding.ItemMessageSentBinding
import com.alya.ecommerce_serang.utils.Constants
import com.bumptech.glide.Glide

class ChatAdapter : ListAdapter<ChatUiMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageViewHolder).bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.isSentByMe) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    /**
     * ViewHolder for messages sent by the current user
     */
    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatUiMessage) {
            binding.tvMessage.text = message.message
            binding.tvTimestamp.text = message.time

            // Show message status
            val statusIcon = when (message.status) {
                Constants.STATUS_SENT -> R.drawable.check_single_24
                Constants.STATUS_DELIVERED -> R.drawable.check_double_24
                Constants.STATUS_READ -> R.drawable.check_double_read_24
                else -> R.drawable.check_single_24
            }
            binding.imgStatus.setImageResource(statusIcon)

            // Handle attachment if exists
            if (message.attachment?.isNotEmpty() == true) {
                binding.imgAttachment.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(BASE_URL + message.attachment)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.imgAttachment)
            } else {
                binding.imgAttachment.visibility = View.GONE
            }
        }
    }

    /**
     * ViewHolder for messages received from other users
     */
    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatUiMessage) {
            binding.tvMessage.text = message.message
            binding.tvTimestamp.text = message.time

            // Handle attachment if exists
            if (message.attachment?.isNotEmpty() == true) {
                binding.imgAttachment.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(BASE_URL + message.attachment)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.imgAttachment)
            } else {
                binding.imgAttachment.visibility = View.GONE
            }

            // Load avatar image
            Glide.with(binding.root.context)
                .load(R.drawable.placeholder_image) // Replace with actual avatar URL if available
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }
}

/**
 * DiffUtil callback for optimizing RecyclerView updates
 */
class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatUiMessage>() {
    override fun areItemsTheSame(oldItem: ChatUiMessage, newItem: ChatUiMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatUiMessage, newItem: ChatUiMessage): Boolean {
        return oldItem == newItem
    }
}