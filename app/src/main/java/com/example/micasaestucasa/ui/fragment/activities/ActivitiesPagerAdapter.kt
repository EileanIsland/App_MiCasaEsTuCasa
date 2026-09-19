package com.example.micasaestucasa.ui.fragment.activities
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ActivitiesPagerAdapter (
    fragment: Fragment
): FragmentStateAdapter(fragment){

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {

        return when (position) {
            0 -> GuestActivitiesFragment()
            1 -> OwnerActivitiesFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}