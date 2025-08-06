package com.alya.ecommerce_serang.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.databinding.ItemDateHeaderBinding
import com.alya.ecommerce_serang.databinding.ItemMessageProductReceivedBinding
import com.alya.ecommerce_serang.databinding.ItemMessageProductSentBinding
import com.alya.ecommerce_serang.databinding.ItemMessageReceivedBinding
import com.alya.ecommerce_serang.databinding.ItemMessageSentBinding
import com.alya.ecommerce_serang.utils.Constants
import com.bumptech.glide.Glide

class ChatAdapter(
    private val onProductClick: ((ProductInfo) -> Unit)? = null
) : ListAdapter<ChatDisplayItem, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
        private const val VIEW_TYPE_PRODUCT_SENT = 3
        private const val VIEW_TYPE_PRODUCT_RECEIVED = 4
        private const val VIEW_TYPE_DATE_HEADER = 5
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is ChatDisplayItem.DateHeaderItem -> VIEW_TYPE_DATE_HEADER
            is ChatDisplayItem.MessageItem -> {
                val message = item.chatUiMessage
                when {
                    message.messageType == MessageType.PRODUCT && message.isSentByMe -> VIEW_TYPE_PRODUCT_SENT
                    message.messageType == MessageType.PRODUCT && !message.isSentByMe -> VIEW_TYPE_PRODUCT_RECEIVED
                    message.isSentByMe -> VIEW_TYPE_MESSAGE_SENT
                    else -> VIEW_TYPE_MESSAGE_RECEIVED
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(inflater, parent, false)
                DateHeaderViewHolder(binding)
            }
            VIEW_TYPE_MESSAGE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_PRODUCT_SENT -> {
                val binding = ItemMessageProductSentBinding.inflate(inflater, parent, false)
                SentProductViewHolder(binding)
            }
            VIEW_TYPE_PRODUCT_RECEIVED -> {
                val binding = ItemMessageProductReceivedBinding.inflate(inflater, parent, false)
                ReceivedProductViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is DateHeaderViewHolder -> {
                if (item is ChatDisplayItem.DateHeaderItem) {
                    holder.bind(item)
                }
            }
            is SentMessageViewHolder -> {
                if (item is ChatDisplayItem.MessageItem) {
                    holder.bind(item.chatUiMessage)
                }
            }
            is ReceivedMessageViewHolder -> {
                if (item is ChatDisplayItem.MessageItem) {
                    holder.bind(item.chatUiMessage)
                }
            }
            is SentProductViewHolder -> {
                if (item is ChatDisplayItem.MessageItem) {
                    holder.bind(item.chatUiMessage)
                }
            }
            is ReceivedProductViewHolder -> {
                if (item is ChatDisplayItem.MessageItem) {
                    holder.bind(item.chatUiMessage)
                }
            }
        }
    }

    /**
     * ViewHolder for regular messages sent by the current user
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

                val fullImageUrl = when (val img = message.attachment) {
                    is String -> {
                        if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                    }
                    else -> R.drawable.placeholder_image
                }

                Glide.with(binding.root.context)
                    .load(fullImageUrl)
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
     * ViewHolder for regular messages received from other users
     */
    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatUiMessage) {
            binding.tvMessage.text = message.message
            binding.tvTimestamp.text = message.time

            // Handle attachment if exists
            val fullImageUrl = when (val img = message.attachment) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> R.drawable.placeholder_image
            }

            if (message.attachment?.isNotEmpty() == true) {
                binding.imgAttachment.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(fullImageUrl)
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

    /**
     * ViewHolder for product messages sent by the current user
     */
    inner class SentProductViewHolder(private val binding: ItemMessageProductSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatUiMessage) {
            // For product bubble, we don't show the text message here
            binding.tvTimestamp.text = message.time

            // Show message status
            val statusIcon = when (message.status) {
                Constants.STATUS_SENT -> R.drawable.check_single_24
                Constants.STATUS_DELIVERED -> R.drawable.check_double_24
                Constants.STATUS_READ -> R.drawable.check_double_read_24
                else -> R.drawable.check_single_24
            }
            binding.imgStatus.setImageResource(statusIcon)

            // Bind product info
            message.productInfo?.let { product ->
                binding.tvProductName.text = product.productName
                binding.tvProductPrice.text = product.productPrice

                // Load product image
                val fullImageUrl = if (product.productImage!!.startsWith("/")) {
                    BASE_URL + product.productImage.substring(1)
                } else {
                    product.productImage
                }

                Glide.with(binding.root.context)
                    .load(fullImageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.imgProduct)

                // Handle product click
                binding.layoutProduct.setOnClickListener {
                    onProductClick?.invoke(product)
                }
            }
        }
    }

    /**
     * ViewHolder for product messages received from other users
     */
    inner class ReceivedProductViewHolder(private val binding: ItemMessageProductReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatUiMessage) {
            // For product bubble, we don't show the text message here
            binding.tvTimestamp.text = message.time

            // Bind product info
            message.productInfo?.let { product ->
                binding.tvProductName.text = product.productName
                binding.tvProductPrice.text = product.productPrice

                // Load product image
                val fullImageUrl = if (product.productImage!!.startsWith("/")) {
                    BASE_URL + product.productImage.substring(1)
                } else {
                    product.productImage
                }

                Glide.with(binding.root.context)
                    .load(fullImageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(binding.imgProduct)

                // Handle product click
                binding.layoutProduct.setOnClickListener {
                    onProductClick?.invoke(product)
                }
            }
        }
    }

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatDisplayItem.DateHeaderItem) {
            binding.tvDate.text = item.formattedDate
        }
    }
}

/**
 * DiffUtil callback for optimizing RecyclerView updates
 */
class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatDisplayItem>() {
    override fun areItemsTheSame(oldItem: ChatDisplayItem, newItem: ChatDisplayItem): Boolean {
        return when {
            oldItem is ChatDisplayItem.MessageItem && newItem is ChatDisplayItem.MessageItem ->
                oldItem.chatUiMessage.id == newItem.chatUiMessage.id
            oldItem is ChatDisplayItem.DateHeaderItem && newItem is ChatDisplayItem.DateHeaderItem ->
                oldItem.date == newItem.date
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ChatDisplayItem, newItem: ChatDisplayItem): Boolean {
        return oldItem == newItem
    }
}

sealed class ChatDisplayItem {
    data class MessageItem(val chatUiMessage: ChatUiMessage) : ChatDisplayItem()
    data class DateHeaderItem(val date: String, val formattedDate: String) : ChatDisplayItem()
}