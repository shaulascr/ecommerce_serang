package com.alya.ecommerce_serang.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.alya.ecommerce_serang.data.api.retrofit.ApiConfig
import com.alya.ecommerce_serang.data.repository.ChatRepository
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

        setupView()
    }

    private fun setupView(){
        binding.btnTrial.setOnClickListener{
            val intent = Intent(requireContext(), ChatActivity::class.java)
            startActivity(intent)
        }
    }
}