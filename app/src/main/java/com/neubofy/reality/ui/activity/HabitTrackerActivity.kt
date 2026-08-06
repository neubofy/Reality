package com.neubofy.reality.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.neubofy.reality.R
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

    private lateinit var tvCurrentDate: TextView
    private lateinit var tvDateSubtext: TextView
    private lateinit var btnPrevDay: ImageButton
    private lateinit var btnNextDay: ImageButton
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
        rvHabits = findViewById(R.id.rv_habits)
        layoutEmpty = findViewById(R.id.layout_empty)
        fabAdd = findViewById(R.id.fab_add_habit)

        setupRecyclerView()
        setupDateNavigation()

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

    private fun loadHabitsForSelectedDate() {
        lifecycleScope.launch {
            val habits = repo.getHabitsWithStatusForDate(selectedDate)

            // Update Header Text
            val dateText = if (selectedDate == LocalDate.now()) {
                "Today"
            } else if (selectedDate == LocalDate.now().minusDays(1)) {
                "Yesterday"
            } else {
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            }
            tvCurrentDate.text = dateText

            val completedCount = habits.count { it.isCompleted }
            tvDateSubtext.text = "$completedCount of ${habits.size} habits completed"

            if (habits.isEmpty()) {
                rvHabits.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
            } else {
                rvHabits.visibility = View.VISIBLE
                layoutEmpty.visibility = View.GONE
                adapter.submitList(habits)
            }
        }
    }
}
