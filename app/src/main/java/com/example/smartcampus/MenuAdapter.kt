//package com.example.smartcampus
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import android.widget.RatingBar
//import androidx.recyclerview.widget.RecyclerView
//import com.example.smartcampus.databinding.ItemMenuBinding
//
//class MenuAdapter(
//    private val onItemClick: (MenuItem) -> Unit,
//    // Add a new lambda for rating changes
//    private val onRatingChanged: (MenuItem, Float) -> Unit
//) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {
//
//    private var menuItems = listOf<MenuItem>()
//    private var filteredItems = listOf<MenuItem>()
//    // This will hold the ratings from Firestore
//    private var ratingsMap = mapOf<String, Pair<Double, Int>>()
//
//    fun updateMenu(newMenu: List<MenuItem>) {
//        menuItems = newMenu
//        filteredItems = newMenu
//        notifyDataSetChanged()
//    }
//
//    fun filterByCategory(category: String) {
//        filteredItems = if (category == "All") {
//            menuItems
//        } else {
//            menuItems.filter { it.category == category }
//        }
//        notifyDataSetChanged()
//    }
//
//    // New function to receive the ratings map
//    fun updateRatings(newRatings: Map<String, Pair<Double, Int>>) {
//        ratingsMap = newRatings
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
//        val binding = ItemMenuBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return MenuViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
//        holder.bind(filteredItems[position])
//    }
//
//    override fun getItemCount(): Int = filteredItems.size
//
//    inner class MenuViewHolder(
//        private val binding: ItemMenuBinding
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(menuItem: MenuItem) {
//            binding.apply {
//                itemNameText.text = menuItem.name
//                itemPriceText.text = menuItem.price
//                itemCategoryText.text = menuItem.category
//                itemDescriptionText.text = menuItem.description
//                // This now sets your fast-food.png
//                itemIconImageView.setImageResource(menuItem.iconRes)
//
//                // --- THIS IS THE RATING LOGIC ---
//                // Get the rating from the map
//                val docId = menuItem.name.replace(" ", "_").lowercase()
//                val ratingInfo = ratingsMap[docId]
//
//                val currentRating = (ratingInfo?.first ?: 0.0).toFloat()
//
//                // Set the rating on the bar
//                itemRatingBar.rating = currentRating
//
//                // Handle item click
//                root.setOnClickListener {
//                    onItemClick(menuItem)
//                }
//
//                // Handle rating bar change
//                itemRatingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
//                    if (fromUser) {
//                        onRatingChanged(menuItem, rating)
//                    }
//                }
//
//                // (Your animation code is good)
//                root.alpha = 0f
//                root.animate()
//                    .alpha(1f)
//                    .setDuration(300)
//                    .setStartDelay(adapterPosition * 50L)
//                    .start()
//            }
//        }
//    }
//}

package com.example.smartcampus

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RatingBar
import androidx.recyclerview.widget.RecyclerView
import com.example.smartcampus.databinding.ItemMenuBinding

class MenuAdapter(
    private val onItemClick: (MenuItem) -> Unit,
    private val onRatingChanged: (MenuItem, Float) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private var menuItems = listOf<MenuItem>()
    private var filteredItems = listOf<MenuItem>()
    private var ratingsMap = mapOf<String, Pair<Double, Int>>()

    fun updateMenu(newMenu: List<MenuItem>) {
        menuItems = newMenu
        filteredItems = newMenu
        notifyDataSetChanged()
    }

    fun filterByCategory(category: String) {
        filteredItems = if (category == "All") {
            menuItems
        } else {
            menuItems.filter { it.category == category }
        }
        notifyDataSetChanged()
    }

    fun updateRatings(newRatings: Map<String, Pair<Double, Int>>) {
        ratingsMap = newRatings
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(filteredItems[position])
    }

    override fun getItemCount(): Int = filteredItems.size

    inner class MenuViewHolder(
        private val binding: ItemMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(menuItem: MenuItem) {
            binding.apply {
                itemNameText.text = menuItem.name
                itemPriceText.text = menuItem.price
                itemCategoryText.text = menuItem.category
                itemDescriptionText.text = menuItem.description
                itemIconImageView.setImageResource(menuItem.iconRes)

                val docId = menuItem.name.replace(" ", "_").lowercase()
                val ratingInfo = ratingsMap[docId]
                val currentRating = (ratingInfo?.first ?: 0.0).toFloat()

                // *** ROBUSTNESS FIX ***
                // Set listener to null *before* setting the rating
                // This prevents the listener from firing accidentally
                itemRatingBar.onRatingBarChangeListener = null
                itemRatingBar.rating = currentRating

                root.setOnClickListener {
                    onItemClick(menuItem)
                }

                // Now, set the real listener
                itemRatingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
                    if (fromUser) {
                        onRatingChanged(menuItem, rating)
                    }
                }

                root.alpha = 0f
                root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(adapterPosition * 50L)
                    .start()
            }
        }
    }

    // <-- *** FIX: ADDED THIS MISSING FUNCTION ***
    fun findItemPosition(itemName: String): Int {
        return filteredItems.indexOfFirst { it.name == itemName }
    }
}