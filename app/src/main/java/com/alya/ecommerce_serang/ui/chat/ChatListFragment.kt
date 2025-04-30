package com.alya.ecommerce_serang.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.utils.viewmodel.ChatViewModel

class ChatListFragment : Fragment() {

    companion object {
        fun newInstance() = ChatListFragment()
    }

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }
}