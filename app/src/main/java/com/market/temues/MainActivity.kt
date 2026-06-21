package com.market.temues

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.market.temues.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar)

        observeAuthState(navController)

        val authDestinations = setOf(
            R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuthScreen = destination.id in authDestinations
            binding.toolbar.isVisible = !isAuthScreen
            binding.bottomNavigation.isVisible = !isAuthScreen
            supportActionBar?.setDisplayHomeAsUpEnabled(!isAuthScreen)
        }
    }

    private fun observeAuthState(navController: androidx.navigation.NavController) {
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                lifecycleScope.launch {
                    try {
                        val doc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .get()
                            .await()
                        val isAdmin = doc.getBoolean("isAdmin") ?: false
                        updateNavForAdmin(isAdmin, navController)
                    } catch (_: Exception) {
                        updateNavForAdmin(false, navController)
                    }
                }
            } else {
                updateNavForAdmin(false, navController)
            }
        }
    }

    private fun updateNavForAdmin(isAdmin: Boolean, navController: androidx.navigation.NavController) {
        if (isAdmin) {
            binding.bottomNavigation.menu.clear()
            binding.bottomNavigation.inflateMenu(R.menu.admin_bottom_nav_menu)
            val adminTopLevel = setOf(
                R.id.adminDashboardFragment,
                R.id.categoryListFragment,
                R.id.adminCreateCategoryFragment,
                R.id.profileFragment
            )
            val appBarConfig = AppBarConfiguration(adminTopLevel)
            setupActionBarWithNavController(navController, appBarConfig)
        } else {
            binding.bottomNavigation.menu.clear()
            binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu)
            val userTopLevel = setOf(
                R.id.homeFragment,
                R.id.favoritesFragment,
                R.id.chatFragment,
                R.id.profileFragment
            )
            val appBarConfig = AppBarConfiguration(userTopLevel)
            setupActionBarWithNavController(navController, appBarConfig)
        }
        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
