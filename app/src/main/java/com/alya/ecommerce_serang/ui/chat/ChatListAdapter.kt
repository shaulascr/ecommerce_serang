package com.alya.ecommerce_serang.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.chat.ChatItemList
import com.alya.ecommerce_serang.databinding.ItemChatBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatListAdapter(
    private val chatList: List<ChatItemList>,
    private val onClick: (ChatItemList) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: ChatItemList) {
            binding.txtStoreName.text = chat.storeName
            binding.txtMessage.text = chat.message
            binding.txtTime.text = formatTime(chat.latestMessageTime)

            // Process image URL properly
            val imageUrl = chat.storeImage?.let {
                if (it.startsWith("/")) BASE_URL + it else it
            }

            Glide.with(binding.imgStore.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.placeholder_image)
                .into(binding.imgStore)

            // Handle click event
            binding.root.setOnClickListener {
                onClick(chat)
            }
        }

        private fun formatTime(isoTime: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(isoTime)

                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }
}
