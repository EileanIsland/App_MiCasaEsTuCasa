package com.example.micasaestucasa

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

//TODO:
// 1 aggiornare i repository per usere Result<Unit>
// 2 - Rivedere startDestination dinamico forse non è la soluzione migliore?
// 3- migliorare gestione navigazione dopo log out mi da errori e chiude l'app qundo dovrebbe rimandarmi su login sempre


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isCheckingSession = true

    override fun onCreate(savedInstanceState: Bundle?) {

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
                    val startDest = if (isAdmin) R.id.AdminStatistiche else R.id.homeFragment
                    navigationSetUp(navGraph, startDest, isAdmin)

                }.onFailure{
                    navigationSetUp(navGraph, R.id.loginFragment, isAdmin = false)
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

            val authFragments = setOf(R.id.loginFragment, R.id.registerFragment)
            if (destination.id in authFragments) {
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
            val adminFragments = setOf(R.id.AdminStatistiche, R.id.AdminGestioneUtenti, R.id.AdminGestioneCase)
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

}







