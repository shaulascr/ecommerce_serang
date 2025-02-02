package com.alya.ecommerce_serang.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.databinding.ActivityMainBinding

//@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        observeDestinationChanges()

    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_item_fragment -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.chat_item_fragment -> {
                    navController.navigate(R.id.chatFragment)
                    true
                }
                R.id.profile_item_fragment -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeDestinationChanges() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = when (destination.id) {
                R.id.homeFragment, R.id.chatFragment, R.id.profileFragment -> true
                else -> false // Bottom Navigation tidak terlihat di layar lain
            }
        }
    }
}