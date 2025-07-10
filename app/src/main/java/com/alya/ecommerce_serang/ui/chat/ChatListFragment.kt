package com.alya.ecommerce_serang.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ChatRepository
import com.alya.ecommerce_serang.data.repository.Result
import com.alya.ecommerce_serang.databinding.FragmentChatListBinding
import com.alya.ecommerce_serang.utils.BaseViewModelFactory
import com.alya.ecommerce_serang.utils.SessionManager

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null

    private val  binding get() = _binding!!
    private lateinit var socketService: SocketIOService
    private lateinit var sessionManager: SessionManager

    private val viewModel: com.alya.ecommerce_serang.ui.chat.ChatViewModel by viewModels {
        BaseViewModelFactory {
            val apiService = ApiConfig.getApiService(sessionManager)
            val chatRepository = ChatRepository(apiService)
            ChatViewModel(chatRepository, socketService, sessionManager)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
        socketService = SocketIOService(sessionManager)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getChatList()
        observeChatList()
    }

    private fun observeChatList() {
        viewModel.chatList.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val data = result.data

                    binding.tvEmptyChat.visibility = View.GONE
                    if (data.isNotEmpty()) {
                        val adapter = ChatListAdapter(data) { chatItem ->
                            ChatActivity.createIntent(
                                context       = requireActivity(),
                                storeId       = chatItem.storeId,
                                productId     = 0,
                                productName   = null,
                                productPrice  = "",
                                productImage  = null,
                                productRating = null,
                                storeName     = chatItem.storeName,
                                chatRoomId    = chatItem.chatRoomId,
                                storeImage    = chatItem.storeImage
                            )
                        }
                        binding.chatListRecyclerView.adapter = adapter
                    } else {
                        binding.tvEmptyChat.visibility = View.VISIBLE
                    }
                }
                is Result.Error -> {
                    binding.tvEmptyChat.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Failed to load chats", Toast.LENGTH_SHORT).show()
                }
                Result.Loading -> {
                    binding.progressBarChat.visibility = View.VISIBLE
                    // Optional: show progress bar
                }
            }
        }
        //loading chat list
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarChat?.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d(TAG, "Loading state: $isLoading")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object{
        private var TAG = "ChatListFragment"
    }
}