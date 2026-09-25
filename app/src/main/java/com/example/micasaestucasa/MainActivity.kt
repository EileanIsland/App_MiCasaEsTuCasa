package com.example.micasaestucasa

import android.os.Bundle
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

//TODO:
// 1 aggiornare i repository per usere Result<Unit>
// 2 - Rivedere startDestination dinamico forse non è la soluzione migliore?
// 3- migliorare gestione navigazione dopo log out mi da errori e chiude l'app qundo dovrebbe rimandarmi su login sempre


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private var isCheckingSession = true

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition{isCheckingSession}

        //enableEdgeToEdge() mi dava problemi con la barra di navigazione

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initAppNavigation()


        //val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
       // navController = navHostFragment.navController
        //val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        //bottomNav.setupWithNavController(navController)

        //setupDestinationSecurity()


    }


    private fun initAppNavigation(){
        val uid = UsersRepository.getCurrentUid()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)

        if(uid == null){
            navGraph.setStartDestination(R.id.loginFragment)
            navController.graph = navGraph
            setUpNavigationMenu(isAdmin = false, navController)
            isCheckingSession = false
        }else{
            lifecycleScope.launch{
                val result = UsersRepository.loadAndCacheProfile(uid)

                result.onSuccess {
                    val isAdmin = UsersRepository.isAdmin()

                    if(isAdmin){
                        navGraph.setStartDestination(R.id.AdminStatistiche)
                    }else{
                        navGraph.setStartDestination(R.id.homeFragment)
                    }

                    navController.graph = navGraph
                    setUpNavigationMenu(isAdmin, navController)
                    setupDestinationSecurity()

                    isCheckingSession = false

                }.onFailure{
                    navGraph.setStartDestination(R.id.loginFragment)
                    navController.graph = navGraph
                    setUpNavigationMenu(isAdmin = false, navController)

                    isCheckingSession = false

                }
            }
        }
    }

    private fun setUpNavigationMenu(isAdmin : Boolean, navController: NavController){
        val navView: BottomNavigationView = binding.bottomNavigation
        navView.menu.clear()

        if(isAdmin){
            navView.inflateMenu(R.menu.menu_admin)
        }else{
            navView.inflateMenu(R.menu.bottom_nav_menu)
        }

        //TODO CONTROLLARE SE FUNZIONA  E CAPIRE CODICE
        val navOptions = NavOptions.Builder().
            setLaunchSingleTop(true).
            setRestoreState(true).
            setPopUpTo(navController.graph.findStartDestination().id, false).build()

        navView.setOnItemSelectedListener { item ->
            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener true
            }

            try {
                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = navView.menu
            for (i in 0..<menu.size) {
                val item = menu[i]
                if (item.itemId == destination.id) {
                    item.isChecked = true
                }
            }
        }



    }


    private fun setupDestinationSecurity(){
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val adminDestinations = setOf(
                R.id.AdminStatistiche,
                R.id.AdminGestioneUtenti,
                R.id.AdminGestioneCase
            )

            if(destination.id in adminDestinations){
                if(!UsersRepository.isAdmin()){
                    Toast.makeText(this, "Accesso negato - area riservata", Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.homeFragment)
                }

            }
        }

    }
}







