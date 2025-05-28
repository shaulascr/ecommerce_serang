package com.alya.ecommerce_serang.ui.profile.mystore.chat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.api.retrofit.ApiService
import com.alya.ecommerce_serang.data.repository.ChatRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.ActivityChatListStoreBinding
import com.alya.ecommerce_serang.ui.chat.ChatViewModel
import com.alya.ecommerce_serang.ui.chat.SocketIOService
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class ChatListStoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatListStoreBinding
    private lateinit var socketService: SocketIOService
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    private val TAG = "ChatListStoreActivity"

    private val viewModel: ChatViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val chatRepository = ChatRepository(apiService)
            ChatViewModel(chatRepository, socketService, sessionManager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager and SocketService
        sessionManager = SessionManager(this)
        socketService = SocketIOService(sessionManager)

        // Inflate the layout and set content view
        binding = ActivityChatListStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        apiService = ApiConfig.getApiService(sessionManager)

        enableEdgeToEdge()

        setupToolbar()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            windowInsets
        }

        Log.d(TAG, "Fetching chat list from ViewModel")
        viewModel.getChatListStore()
        observeChatList()
    }

    private fun setupToolbar(){
        binding.header.headerLeftIcon.setOnClickListener{
            finish()
        }
        binding.header.headerTitle.text = "Pesan"
    }

    private fun observeChatList() {
        viewModel.chatListStore.observe(this) { result ->
            Log.d(TAG, "Observer triggered with result: $result")

            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Chat list fetch success. Data size: ${result.data.size}")
                    val adapter = ChatListAdapter(result.data) { chatItem ->
                        Log.d(TAG, "Chat item clicked: storeId=${chatItem.storeId}, chatRoomId=${chatItem.chatRoomId}")
                        val intent = ChatStoreActivity.createIntent(
                            context = this,
                            storeId = chatItem.storeId,
                            productId = 0,
                            productName = null,
                            productPrice = "",
                            productImage = null,
                            productRating = null,
                            storeName = chatItem.storeName,
                            chatRoomId = chatItem.chatRoomId,
                            storeImage = chatItem.storeImage
                        )
                        startActivity(intent)
                    }
                    binding.chatListRecyclerView.adapter = adapter
                    Log.d(TAG, "Adapter set successfully")
                }

                is Result.Error -> {
                    Log.e(TAG, "Failed to load chats: ${result.exception.message}")
                    Toast.makeText(this, "Failed to load chats", Toast.LENGTH_SHORT).show()
                }

                Result.Loading -> {
                    Log.d(TAG, "Chat list is loading...")
                }
            }
        }
    }
}