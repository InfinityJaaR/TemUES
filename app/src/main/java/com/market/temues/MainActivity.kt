package com.market.temues

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.market.temues.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var menuVisible = false
    private var badgeListener: ListenerRegistration? = null
    private var totalNoLeidos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar)

        solicitarPermisoNotificaciones()
        observeAuthState(navController)

        val authDestinations = setOf(
            R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuthScreen = destination.id in authDestinations
            val adminDestinations = setOf(
                R.id.adminDashboardFragment, R.id.categoryListFragment,
                R.id.adminCreateCategoryFragment
            )
            val fullScreenDestinations = setOf(R.id.chatDetailFragment, R.id.cameraFragment)
            val hideBottomNav = isAuthScreen || destination.id in fullScreenDestinations
            binding.toolbar.isVisible = !isAuthScreen && destination.id != R.id.cameraFragment
            binding.bottomNavigation.isVisible = !hideBottomNav
            supportActionBar?.setDisplayHomeAsUpEnabled(!isAuthScreen)
            menuVisible = !isAuthScreen && destination.id !in adminDestinations && destination.id != R.id.cartFragment
            invalidateOptionsMenu()
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }

    private fun observeAuthState(navController: androidx.navigation.NavController) {
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                iniciarBadgeChat(user.uid)
                lifecycleScope.launch {
                    try {
                        val doc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .get()
                            .await()
                        val isAdmin = doc.getBoolean("isAdmin") ?: false
                        updateNavForAdmin(isAdmin, navController)
                        val destId = if (isAdmin) R.id.adminDashboardFragment
                                     else R.id.homeFragment
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                        navController.navigate(destId, null, navOptions)
                    } catch (_: Exception) {
                        updateNavForAdmin(false, navController)
                    }
                }
            } else {
                detenerBadgeChat()
                updateNavForAdmin(false, navController)
            }
        }
    }

    private fun iniciarBadgeChat(uid: String) {
        badgeListener?.remove()
        badgeListener = FirebaseFirestore.getInstance()
            .collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, _ ->
                totalNoLeidos = snapshot?.documents?.sumOf { doc ->
                    val counts = doc.get("unreadCounts") as? Map<*, *>
                    (counts?.get(uid) as? Long)?.toInt() ?: 0
                } ?: 0
                aplicarBadge()
            }
    }

    private fun aplicarBadge() {
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.chatFragment)
        badge.isVisible = totalNoLeidos > 0
        if (totalNoLeidos > 0) badge.number = totalNoLeidos
    }

    private fun detenerBadgeChat() {
        badgeListener?.remove()
        badgeListener = null
        totalNoLeidos = 0
        binding.bottomNavigation.removeBadge(R.id.chatFragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        badgeListener?.remove()
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
            aplicarBadge()
        }
        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        manejarIntentNotificacion(intent)
    }

    private fun manejarIntentNotificacion(intent: Intent) {
        val chatId = intent.getStringExtra("chatId") ?: return
        val destino = intent.getStringExtra("destino") ?: return
        if (destino == "chatDetail" && chatId.isNotBlank()) {
            val args = Bundle().apply { putString("chatId", chatId) }
            findNavController(R.id.nav_host_fragment).navigate(R.id.chatDetailFragment, args)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_cart)?.isVisible = menuVisible
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cart -> {
                val navController = findNavController(R.id.nav_host_fragment)
                if (navController.currentDestination?.id != R.id.cartFragment) {
                    navController.navigate(R.id.cartFragment)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
