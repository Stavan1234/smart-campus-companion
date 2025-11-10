//package com.example.smartcampus
//
//import android.os.Bundle
//import android.view.animation.AnimationUtils
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.GridLayoutManager
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.smartcampus.databinding.ActivityCanteenMenuBinding
//import com.google.firebase.firestore.FirebaseFirestore
//
//class CanteenMenuActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityCanteenMenuBinding
//    private lateinit var menuAdapter: MenuAdapter
//    private var isGridView = false
//
//    private val firestore = FirebaseFirestore.getInstance()
//    private var ratingsMap = mutableMapOf<String, Pair<Double, Int>>()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityCanteenMenuBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setupToolbar()
//        setupMenuRecyclerView()
//        setupClickListeners()
//        fetchRatingsAndLoadMenu()
//        startAnimations()
//    }
//
//    private fun setupToolbar() {
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.title = "Canteen Menu"
//    }
//
//    private fun setupMenuRecyclerView() {
//        menuAdapter = MenuAdapter(
//            onItemClick = { menuItem ->
//                Toast.makeText(this, "${menuItem.name} - ${menuItem.price}", Toast.LENGTH_SHORT).show()
//            },
//            onRatingChanged = { menuItem, newRating ->
//                submitRating(menuItem, newRating)
//            }
//        )
//
//        binding.menuRecyclerView.apply {
//            layoutManager = LinearLayoutManager(this@CanteenMenuActivity)
//            adapter = menuAdapter
//        }
//    }
//
//    private fun submitRating(menuItem: MenuItem, rating: Float) {
//        val docId = menuItem.name.replace(" ", "_").lowercase()
//        val ratingRef = firestore.collection("canteen_ratings").document(docId)
//
//        firestore.runTransaction { transaction ->
//            val snapshot = transaction.get(ratingRef)
//
//            val newRating: Double
//            val newTotalRatings: Int
//
//            if (snapshot.exists()) {
//                val currentRating = snapshot.getDouble("rating") ?: 0.0
//                val totalRatings = snapshot.getLong("totalRatings")?.toInt() ?: 0
//
//                newTotalRatings = totalRatings + 1
//                newRating = ((currentRating * totalRatings) + rating) / newTotalRatings
//            } else {
//                newTotalRatings = 1
//                newRating = rating.toDouble()
//            }
//
//            val data = hashMapOf(
//                "name" to menuItem.name,
//                "rating" to newRating,
//                "totalRatings" to newTotalRatings
//            )
//            transaction.set(ratingRef, data)
//            Pair(newRating, newTotalRatings)
//
//        }.addOnSuccessListener { (updatedRating, updatedTotal) ->
//            Toast.makeText(this, "Rating ($rating) submitted!", Toast.LENGTH_SHORT).show()
//            ratingsMap[docId] = Pair(updatedRating, updatedTotal)
//            menuAdapter.updateRatings(ratingsMap)
//            val index = menuAdapter.findItemPosition(menuItem.name)
//            if (index != -1) {
//                menuAdapter.notifyItemChanged(index)
//            }
//        }.addOnFailureListener { e ->
//            Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun setupClickListeners() {
//        binding.viewToggleButton.setOnClickListener {
//            toggleViewMode()
//        }
//
//        binding.categoryFilter.setOnCheckedChangeListener { _, checkedId ->
//            filterByCategory(checkedId)
//        }
//    }
//
//    private fun fetchRatingsAndLoadMenu() {
//        binding.menuRecyclerView.visibility = View.GONE
//        firestore.collection("canteen_ratings").get()
//            .addOnSuccessListener { snapshot ->
//                for (doc in snapshot.documents) {
//                    val rating = doc.getDouble("rating") ?: 0.0
//                    val total = doc.getLong("totalRatings")?.toInt() ?: 0
//                    ratingsMap[doc.id] = Pair(rating, total)
//                }
//                menuAdapter.updateRatings(ratingsMap)
//                loadMenuData()
//                binding.menuRecyclerView.visibility = View.VISIBLE
//            }
//            .addOnFailureListener {
//                loadMenuData()
//                binding.menuRecyclerView.visibility = View.VISIBLE
//            }
//    }
//
//    private fun loadMenuData() {
//        // *** FIX: Point to your PNG icon ***
//        val genericIcon = R.drawable.fast_food
//
//        val menuItems = listOf(
//            MenuItem("Samosa", "₹15", "Snacks", "Crispy fried pastry with spiced potato filling", genericIcon),
//            MenuItem("Vada Pav", "₹25", "Snacks", "Mumbai's favorite street food", genericIcon),
//            MenuItem("Pav Bhaji", "₹45", "Main Course", "Spiced vegetable curry with buttered bread", genericIcon),
//            // ... (the rest of your menu items stay the same, all using genericIcon)
//            MenuItem("Chole Bhature", "₹50", "Main Course", "Spiced chickpeas with fried bread", genericIcon),
//            MenuItem("Masala Dosa", "₹40", "Main Course", "Crispy crepe with spiced potato filling", genericIcon),
//            MenuItem("Idli Sambar", "₹35", "Main Course", "Steamed rice cakes with lentil curry", genericIcon),
//            MenuItem("Chicken Biryani", "₹80", "Main Course", "Fragrant basmati rice with spiced chicken", genericIcon),
//            MenuItem("Veg Biryani", "₹60", "Main Course", "Aromatic rice with mixed vegetables", genericIcon),
//            MenuItem("Dal Rice", "₹30", "Main Course", "Lentil curry with steamed rice", genericIcon),
//            MenuItem("Rajma Rice", "₹35", "Main Course", "Kidney beans curry with rice", genericIcon),
//            MenuItem("Chicken Curry", "₹70", "Main Course", "Spiced chicken in rich gravy", genericIcon),
//            MenuItem("Paneer Butter Masala", "₹55", "Main Course", "Cottage cheese in creamy tomato gravy", genericIcon),
//            MenuItem("Chai", "₹10", "Beverages", "Traditional Indian spiced tea", genericIcon),
//            MenuItem("Coffee", "₹15", "Beverages", "Freshly brewed coffee", genericIcon),
//            MenuItem("Lassi", "₹25", "Beverages", "Sweet yogurt drink", genericIcon),
//            MenuItem("Fresh Juice", "₹30", "Beverages", "Seasonal fruit juice", genericIcon),
//            MenuItem("Cold Coffee", "₹20", "Beverages", "Iced coffee with milk", genericIcon),
//            MenuItem("Lemonade", "₹15", "Beverages", "Refreshing lemon drink", genericIcon),
//            MenuItem("Sandwich", "₹35", "Snacks", "Fresh vegetables in bread", genericIcon),
//            MenuItem("Burger", "₹45", "Snacks", "Veg burger with fries", genericIcon),
//            MenuItem("Pizza Slice", "₹40", "Snacks", "Cheese pizza slice", genericIcon),
//            MenuItem("French Fries", "₹25", "Snacks", "Crispy golden fries", genericIcon),
//            MenuItem("Pakora", "₹20", "Snacks", "Deep-fried vegetable fritters", genericIcon),
//            MenuItem("Cutlet", "₹30", "Snacks", "Spiced potato cutlet", genericIcon),
//            MenuItem("Ice Cream", "₹25", "Desserts", "Vanilla ice cream", genericIcon),
//            MenuItem("Gulab Jamun", "₹20", "Desserts", "Sweet milk dumplings", genericIcon),
//            MenuItem("Rasgulla", "₹18", "Desserts", "Spongy cottage cheese balls", genericIcon),
//            MenuItem("Kheer", "₹30", "Desserts", "Rice pudding with nuts", genericIcon),
//            MenuItem("Jalebi", "₹25", "Desserts", "Crispy sweet spirals", genericIcon),
//            MenuItem("Fruit Salad", "₹35", "Healthy", "Fresh seasonal fruits", genericIcon)
//        )
//
//        menuAdapter.updateMenu(menuItems)
//    }
//
//    private fun toggleViewMode() {
//        isGridView = !isGridView
//
//        binding.menuRecyclerView.layoutManager = if (isGridView) {
//            GridLayoutManager(this, 2)
//        } else {
//            LinearLayoutManager(this)
//        }
//
//        // *** FIX: This code now works because you added the PNGs ***
//        binding.viewToggleButton.icon = ContextCompat.getDrawable(
//            this,
//            if (isGridView) R.drawable.ic_list_view else R.drawable.ic_grid_view
//        )
//
//        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
//        binding.menuRecyclerView.startAnimation(slideAnimation)
//    }
//
//    private fun filterByCategory(checkedId: Int) {
//        val category = when (checkedId) {
//            R.id.category_all -> "All"
//            R.id.category_snacks -> "Snacks"
//            R.id.category_main -> "Main Course"
//            R.id.category_beverages -> "Beverages"
//            R.id.category_desserts -> "Desserts"
//            R.id.category_healthy -> "Healthy"
//            else -> "All"
//        }
//
//        menuAdapter.filterByCategory(category)
//    }
//
//    private fun startAnimations() {
//        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
//        binding.toolbar.startAnimation(slideUp)
//
//        binding.categoryFilter.postDelayed({
//            binding.categoryFilter.startAnimation(slideUp)
//        }, 100)
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        onBackPressedDispatcher.onBackPressed()
//        return true
//    }
//}

package com.example.smartcampus

import android.os.Bundle
import android.util.Log
import android.view.View // <-- *** FIX: ADDED MISSING IMPORT ***
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcampus.databinding.ActivityCanteenMenuBinding
import com.google.firebase.firestore.FirebaseFirestore

class CanteenMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCanteenMenuBinding
    private lateinit var menuAdapter: MenuAdapter
    private var isGridView = false

    private val firestore = FirebaseFirestore.getInstance()
    private var ratingsMap = mutableMapOf<String, Pair<Double, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCanteenMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMenuRecyclerView()
        setupClickListeners()
        fetchRatingsAndLoadMenu()
        startAnimations()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Canteen Menu"
    }

    private fun setupMenuRecyclerView() {
        menuAdapter = MenuAdapter(
            onItemClick = { menuItem ->
                Toast.makeText(this, "${menuItem.name} - ${menuItem.price}", Toast.LENGTH_SHORT).show()
            },
            onRatingChanged = { menuItem, newRating ->
                submitRating(menuItem, newRating)
            }
        )

        binding.menuRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CanteenMenuActivity)
            adapter = menuAdapter
        }
    }

    private fun submitRating(menuItem: MenuItem, rating: Float) {
        val docId = menuItem.name.replace(" ", "_").lowercase()
        val ratingRef = firestore.collection("canteen_ratings").document(docId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ratingRef)

            val newRating: Double
            val newTotalRatings: Int

            if (snapshot.exists()) {
                val currentRating = snapshot.getDouble("rating") ?: 0.0
                val totalRatings = snapshot.getLong("totalRatings")?.toInt() ?: 0

                newTotalRatings = totalRatings + 1
                newRating = ((currentRating * totalRatings) + rating) / newTotalRatings
            } else {
                newTotalRatings = 1
                newRating = rating.toDouble()
            }

            val data = hashMapOf(
                "name" to menuItem.name,
                "rating" to newRating,
                "totalRatings" to newTotalRatings
            )
            transaction.set(ratingRef, data)
            Pair(newRating, newTotalRatings)

        }.addOnSuccessListener { (updatedRating, updatedTotal) ->
            Toast.makeText(this, "Rating ($rating) submitted!", Toast.LENGTH_SHORT).show()

            ratingsMap[docId] = Pair(updatedRating, updatedTotal)
            menuAdapter.updateRatings(ratingsMap)
            // This line will now work
            val index = menuAdapter.findItemPosition(menuItem.name)
            if (index != -1) {
                menuAdapter.notifyItemChanged(index)
            }

        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show()
            Log.e("CanteenMenu", "Failed to submit rating", e)
        }
    }

    private fun setupClickListeners() {
        binding.viewToggleButton.setOnClickListener {
            toggleViewMode()
        }

        binding.categoryFilter.setOnCheckedChangeListener { _, checkedId ->
            filterByCategory(checkedId)
        }
    }

    private fun fetchRatingsAndLoadMenu() {
        // These lines will now work
        binding.menuRecyclerView.visibility = View.GONE
        firestore.collection("canteen_ratings").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val rating = doc.getDouble("rating") ?: 0.0
                    val total = doc.getLong("totalRatings")?.toInt() ?: 0
                    ratingsMap[doc.id] = Pair(rating, total)
                }
                menuAdapter.updateRatings(ratingsMap)
                loadMenuData()
                binding.menuRecyclerView.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                loadMenuData()
                binding.menuRecyclerView.visibility = View.VISIBLE
            }
    }

    private fun loadMenuData() {
        val genericIcon = R.drawable.fast_food

        val menuItems = listOf(
            MenuItem("Samosa", "₹15", "Snacks", "Crispy fried pastry with spiced potato filling", genericIcon),
            MenuItem("Vada Pav", "₹25", "Snacks", "Mumbai's favorite street food", genericIcon),
            MenuItem("Pav Bhaji", "₹45", "Main Course", "Spiced vegetable curry with buttered bread", genericIcon),
            MenuItem("Chole Bhature", "₹50", "Main Course", "Spiced chickpeas with fried bread", genericIcon),
            MenuItem("Masala Dosa", "₹40", "Main Course", "Crispy crepe with spiced potato filling", genericIcon),
            MenuItem("Idli Sambar", "₹35", "Main Course", "Steamed rice cakes with lentil curry", genericIcon),
            MenuItem("Chicken Biryani", "₹80", "Main Course", "Fragrant basmati rice with spiced chicken", genericIcon),
            MenuItem("Veg Biryani", "₹60", "Main Course", "Aromatic rice with mixed vegetables", genericIcon),
            MenuItem("Dal Rice", "₹30", "Main Course", "Lentil curry with steamed rice", genericIcon),
            MenuItem("Rajma Rice", "₹35", "Main Course", "Kidney beans curry with rice", genericIcon),
            MenuItem("Chicken Curry", "₹70", "Main Course", "Spiced chicken in rich gravy", genericIcon),
            MenuItem("Paneer Butter Masala", "₹55", "Main Course", "Cottage cheese in creamy tomato gravy", genericIcon),
            MenuItem("Chai", "₹10", "Beverages", "Traditional Indian spiced tea", genericIcon),
            MenuItem("Coffee", "₹15", "Beverages", "Freshly brewed coffee", genericIcon),
            MenuItem("Lassi", "₹25", "Beverages", "Sweet yogurt drink", genericIcon),
            MenuItem("Fresh Juice", "₹30", "Beverages", "Seasonal fruit juice", genericIcon),
            MenuItem("Cold Coffee", "₹20", "Beverages", "Iced coffee with milk", genericIcon),
            MenuItem("Lemonade", "₹15", "Beverages", "Refreshing lemon drink", genericIcon),
            MenuItem("Sandwich", "₹35", "Snacks", "Fresh vegetables in bread", genericIcon),
            MenuItem("Burger", "₹45", "Snacks", "Veg burger with fries", genericIcon),
            MenuItem("Pizza Slice", "₹40", "Snacks", "Cheese pizza slice", genericIcon),
            MenuItem("French Fries", "₹25", "Snacks", "Crispy golden fries", genericIcon),
            MenuItem("Pakora", "₹20", "Snacks", "Deep-fried vegetable fritters", genericIcon),
            MenuItem("Cutlet", "₹30", "Snacks", "Spiced potato cutlet", genericIcon),
            MenuItem("Ice Cream", "₹25", "Desserts", "Vanilla ice cream", genericIcon),
            MenuItem("Gulab Jamun", "₹20", "Desserts", "Sweet milk dumplings", genericIcon),
            MenuItem("Rasgulla", "₹18", "Desserts", "Spongy cottage cheese balls", genericIcon),
            MenuItem("Kheer", "₹30", "Desserts", "Rice pudding with nuts", genericIcon),
            MenuItem("Jalebi", "₹25", "Desserts", "Crispy sweet spirals", genericIcon),
            MenuItem("Fruit Salad", "₹35", "Healthy", "Fresh seasonal fruits", genericIcon)
        )

        menuAdapter.updateMenu(menuItems)
    }

    private fun toggleViewMode() {
        isGridView = !isGridView

        binding.menuRecyclerView.layoutManager = if (isGridView) {
            GridLayoutManager(this, 2)
        } else {
            LinearLayoutManager(this)
        }

        binding.viewToggleButton.icon = ContextCompat.getDrawable(
            this,
            if (isGridView) R.drawable.ic_list_view else R.drawable.ic_grid_view
        )

        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        binding.menuRecyclerView.startAnimation(slideAnimation)
    }

    private fun filterByCategory(checkedId: Int) {
        val category = when (checkedId) {
            R.id.category_all -> "All"
            R.id.category_snacks -> "Snacks"
            R.id.category_main -> "Main Course"
            R.id.category_beverages -> "Beverages"
            R.id.category_desserts -> "Desserts"
            R.id.category_healthy -> "Healthy"
            else -> "All"
        }

        menuAdapter.filterByCategory(category)
    }

    private fun startAnimations() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.toolbar.startAnimation(slideUp)

        binding.categoryFilter.postDelayed({
            binding.categoryFilter.startAnimation(slideUp)
        }, 100)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}