//package com.alya.ecommerce_serang.ui.profile.mystore.chat
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
//import com.alya.ecommerce_serang.data.repository.ChatRepository
//import com.alya.ecommerce_serang.data.repository.Result
//import com.alya.ecommerce_serang.databinding.FragmentChatListBinding
//import com.alya.ecommerce_serang.ui.chat.ChatViewModel
//import com.alya.ecommerce_serang.ui.chat.SocketIOService
//import com.alya.ecommerce_serang.utils.BaseViewModelFactory
//import com.alya.ecommerce_serang.utils.SessionManager
//
//class ChatListStoreFragment : Fragment() {
//
//    private var _binding: FragmentChatListBinding? = null
//
//    private val  binding get() = _binding!!
//    private lateinit var socketService: SocketIOService
//    private lateinit var sessionManager: SessionManager
//
//    private val viewModel: com.alya.ecommerce_serang.ui.chat.ChatViewModel by viewModels {
//        BaseViewModelFactory {
//            val apiService = ApiConfig.getApiService(sessionManager)
//            val chatRepository = ChatRepository(apiService)
//            ChatViewModel(chatRepository, socketService, sessionManager)
//        }
//    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        sessionManager = SessionManager(requireContext())
//        socketService = SocketIOService(sessionManager)
//
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentChatListBinding.inflate(inflater, container, false)
//        return _binding!!.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        viewModel.getChatListStore()
//        observeChatList()
//    }
//
//    private fun observeChatList() {
//        viewModel.chatListStore.observe(viewLifecycleOwner) { result ->
//            when (result) {
//                is Result.Success -> {
//                    val adapter = ChatListAdapter(result.data) { chatItem ->
//                        // Use the ChatActivity.createIntent factory method for proper navigation
//                        ChatStoreActivity.createIntent(
//                            context = requireActivity(),
//                            storeId = chatItem.storeId,
//                            productId = 0, // Default value since we don't have it in ChatListItem
//                            productName = null, // Null is acceptable as per ChatActivity
//                            productPrice = "",
//                            productImage = null,
//                            productRating = null,
//                            storeName = chatItem.storeName,
//                            chatRoomId = chatItem.chatRoomId,
//                            storeImage = chatItem.storeImage
//                        )
//                    }
//                    binding.chatListRecyclerView.adapter = adapter
//                }
//                is Result.Error -> {
//                    Toast.makeText(requireContext(), "Failed to load chats", Toast.LENGTH_SHORT).show()
//                }
//                Result.Loading -> {
//                    // Optional: show progress bar
//                }
//            }
//        }
//    }
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    companion object{
//
//    }
//}