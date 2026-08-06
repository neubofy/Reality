package com.neubofy.reality.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.neubofy.reality.R
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.repository.HabitRepository
import com.neubofy.reality.ui.adapter.HabitAdapter
import com.neubofy.reality.ui.base.BaseActivity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitTrackerActivity : BaseActivity() {

    private lateinit var repo: HabitRepository
    private lateinit var adapter: HabitAdapter
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedCategory: String = HabitEntity.CATEGORY_ALL
    private var allHabits: List<HabitRepository.HabitWithStatus> = emptyList()

    private lateinit var tvCurrentDate: TextView
    private lateinit var tvDateSubtext: TextView
    private lateinit var btnPrevDay: ImageButton
    private lateinit var btnNextDay: ImageButton
    private lateinit var btnOpenAnalytics: MaterialButton
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var rvHabits: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_tracker)

        repo = HabitRepository(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Habit Tracker"
        toolbar.setNavigationOnClickListener { finish() }

        tvCurrentDate = findViewById(R.id.tv_current_date)
        tvDateSubtext = findViewById(R.id.tv_date_subtext)
        btnPrevDay = findViewById(R.id.btn_prev_day)
        btnNextDay = findViewById(R.id.btn_next_day)
        btnOpenAnalytics = findViewById(R.id.btn_open_analytics)
        chipGroupCategories = findViewById(R.id.chip_group_categories)
        rvHabits = findViewById(R.id.rv_habits)
        layoutEmpty = findViewById(R.id.layout_empty)
        fabAdd = findViewById(R.id.fab_add_habit)

        setupRecyclerView()
        setupDateNavigation()
        setupCategoryChips()

        btnOpenAnalytics.setOnClickListener {
            startActivity(Intent(this, ReflectionDetailActivity::class.java))
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, CreateHabitActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadHabitsForSelectedDate()
    }

    private fun setupRecyclerView() {
        adapter = HabitAdapter(
            onToggleClick = { habitStatus ->
                window.decorView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                lifecycleScope.launch {
                    repo.toggleHabit(habitStatus.habit.id, selectedDate)
                    loadHabitsForSelectedDate()
                }
            },
            onItemClick = { habitStatus ->
                val intent = Intent(this, CreateHabitActivity::class.java).apply {
                    putExtra("habit_id", habitStatus.habit.id)
                }
                startActivity(intent)
            }
        )
        rvHabits.layoutManager = LinearLayoutManager(this)
        rvHabits.adapter = adapter
    }

    private fun setupDateNavigation() {
        btnPrevDay.setOnClickListener {
            selectedDate = selectedDate.minusDays(1)
            loadHabitsForSelectedDate()
        }

        btnNextDay.setOnClickListener {
            selectedDate = selectedDate.plusDays(1)
            loadHabitsForSelectedDate()
        }
    }

    private fun setupCategoryChips() {
        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            selectedCategory = when (checkedIds.first()) {
                R.id.chip_cat_health -> HabitEntity.CATEGORY_HEALTH
                R.id.chip_cat_focus -> HabitEntity.CATEGORY_FOCUS
                R.id.chip_cat_mind -> HabitEntity.CATEGORY_MIND
                R.id.chip_cat_body -> HabitEntity.CATEGORY_BODY
                else -> HabitEntity.CATEGORY_ALL
            }
            filterAndDisplayHabits()
        }
    }

    private fun loadHabitsForSelectedDate() {
        lifecycleScope.launch {
            allHabits = repo.getHabitsWithStatusForDate(selectedDate)

            // Update Header Text
            val dateText = if (selectedDate == LocalDate.now()) {
                "Today"
            } else if (selectedDate == LocalDate.now().minusDays(1)) {
                "Yesterday"
            } else {
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            }
            tvCurrentDate.text = dateText

            val completedCount = allHabits.count { it.isCompleted }
            tvDateSubtext.text = "$completedCount of ${allHabits.size} habits completed"

            filterAndDisplayHabits()
        }
    }

    private fun filterAndDisplayHabits() {
        val filtered = if (selectedCategory == HabitEntity.CATEGORY_ALL) {
            allHabits
        } else {
            allHabits.filter { it.habit.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            rvHabits.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvHabits.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            adapter.submitList(filtered)
        }
    }
}
