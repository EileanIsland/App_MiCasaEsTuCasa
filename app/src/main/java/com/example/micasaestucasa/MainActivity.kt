package com.example.micasaestucasa

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.micasaestucasa.data.repository.UsersRepository
import com.example.micasaestucasa.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import androidx.core.view.size
import androidx.core.view.get
import androidx.navigation.NavGraph
import com.example.micasaestucasa.data.local.ThemePreferences
import com.example.micasaestucasa.data.repository.BookingRepository
import com.example.micasaestucasa.data.repository.ChatRepository
import com.example.micasaestucasa.utils.NotificationHelper


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isCheckingSession = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted){
            Toast.makeText(this, "Notifiche disattivate. Non riceverai avvisi per i nuovi messaggi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val themePrefs = ThemePreferences(this)
        val currentTheme = themePrefs.getTheme()
        AppCompatDelegate.setDefaultNightMode(currentTheme)

        NotificationHelper.createNotificationChannels(this)

        installSplashScreen().setKeepOnScreenCondition{isCheckingSession}
        super.onCreate(savedInstanceState)

        //enableEdgeToEdge() mi dava problemi con la barra di navigazione

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initAppNavigation()
        askNotificationPermission()
        handleNotificationIntent(intent)

    }


    private fun initAppNavigation(){
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val uid = UsersRepository.getCurrentUid()

        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        if(uid == null){
            navigationSetUp(navGraph, R.id.loginFragment, isAdmin = false)
        }else{
            lifecycleScope.launch{
                val result = UsersRepository.loadAndCacheProfile(uid)

                result.onSuccess {
                    val isAdmin = UsersRepository.isAdmin()

                    val startDest = if (isAdmin) R.id.statisticheFragment else R.id.homeFragment
                    navigationSetUp(navGraph, startDest, isAdmin)

                    UsersRepository.startUserStatusListener(uid)
                    startAccountMonitoring()
                    BookingRepository.startBookingListener(applicationContext, uid)
                    ChatRepository.startGlobalChatListener(applicationContext, uid)

                }.onFailure { exception ->
                    val errorMessage = exception.message ?: "Errore durante il caricamento del profilo"
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()

                    navigationSetUp(navGraph, R.id.loginFragment, isAdmin = false)

                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    UsersRepository.clearCache()
                }
            }
        }
    }

    /**
     * Imposta la destinazione iniziale, assegna il grafo e configura la UI
     */
    private fun navigationSetUp(navGraph: NavGraph, startDestId: Int, isAdmin: Boolean) {
        navGraph.setStartDestination(startDestId)
        navController.graph = navGraph

        setupBottomNavigationListeners()
        setUpNavigationMenu(isAdmin)

        isCheckingSession = false
    }


    private fun setupBottomNavigationListeners() {
        navController.addOnDestinationChangedListener { _, destination, _ ->

            val noBottomNavFragments = setOf(R.id.loginFragment, R.id.registerFragment, R.id.detailedChatFragment)
            if (destination.id in noBottomNavFragments) {
                binding.bottomNavigation.visibility = View.GONE
            } else {
                binding.bottomNavigation.visibility = View.VISIBLE
            }

            val protectedFragments = setOf(
                R.id.profileFragment,
                R.id.ownerActivitiesFragment,
                R.id.publishFragment,
                R.id.bookingFragment
            )

            if (destination.id in protectedFragments && UsersRepository.getCurrentUid() == null) {
                navController.navigate(R.id.loginFragment) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                return@addOnDestinationChangedListener
            }

            //  SICUREZZA ADMIN
            val adminFragments = setOf(R.id.statisticheFragment, R.id.AdminGestioneUtenti, R.id.AdminTag)
            if (destination.id in adminFragments && !UsersRepository.isAdmin()) {
                Toast.makeText(this, "Accesso negato", Toast.LENGTH_SHORT).show()
                navController.navigate(R.id.homeFragment)
            }

            // Sincronizzazione visiva dei tasti selezionati
            syncMenuSelection(destination.id)
        }
    }



    private fun setUpNavigationMenu(isAdmin: Boolean) {
        val navView: BottomNavigationView = binding.bottomNavigation
        navView.menu.clear()
        navView.inflateMenu(if (isAdmin) R.menu.menu_admin else R.menu.bottom_nav_menu)

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
            .build()

        navView.setOnItemSelectedListener { item ->
            if (item.itemId != navController.currentDestination?.id) {
                try {
                    navController.navigate(item.itemId, null, navOptions)
                } catch (e: Exception) { /* ignore */ }
            }
            true
        }
    }


    private fun syncMenuSelection(destinationId: Int) {
        val menu = binding.bottomNavigation.menu
        for (i in 0 until menu.size) {
            val item = menu[i]
            if (item.itemId == destinationId) {
                item.isChecked = true
            }
        }
    }

    private fun askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS

            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        val type = intent?.getStringExtra("notification_type") ?: return

        lifecycleScope.launch {
            while (isCheckingSession) kotlinx.coroutines.delay(100)

            when (type) {
                "CHAT" -> {
                    val chatId = intent.getStringExtra("chatId")
                    val otherId = intent.getStringExtra("otherUserId") ?: ""
                    val bundle = Bundle().apply {
                        putString("chatId", chatId)
                        putString("otherUserId", otherId)
                    }
                    navController.navigate(R.id.detailedChatFragment, bundle)
                }
                "BOOKING" -> {
                    val isAdmin = UsersRepository.isAdmin()
                    if (!isAdmin) {
                        navController.navigate(R.id.ownerActivitiesFragment)
                    }
                }
            }
        }
    }


    private fun startAccountMonitoring() {
        lifecycleScope.launch {
            UsersRepository.currentUserProfile.collect { user ->
                if (user != null && user.isRestricted()) {
                    val message = user.getStatusMessage() ?: "Il tuo account è stato sospeso."
                    handleForcedLogout(message)
                }
            }
        }
    }

    private fun handleForcedLogout(message: String) {
        if (navController.currentDestination?.id == R.id.loginFragment) return

        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        UsersRepository.clearCache()
        UsersRepository.stopUserStatusListener()
        ChatRepository.stopGlobalChatListener()
        BookingRepository.stopBookingListener()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        if (navController.currentDestination?.id != R.id.loginFragment) {
            navController.navigate(R.id.loginFragment) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        ChatRepository.stopGlobalChatListener()
        BookingRepository.stopBookingListener()
    }

}







