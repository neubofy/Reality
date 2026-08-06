package com.neubofy.reality.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.neubofy.reality.R
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.repository.HabitRepository
import com.neubofy.reality.ui.base.BaseActivity
import kotlinx.coroutines.launch

class CreateHabitActivity : BaseActivity() {

    private lateinit var repo: HabitRepository
    private var editingHabitId: Long = 0L

    private lateinit var etName: TextInputEditText
    private lateinit var etQuestion: TextInputEditText
    private lateinit var toggleType: MaterialButtonToggleGroup
    private lateinit var btnBoolean: MaterialButton
    private lateinit var btnMeasurable: MaterialButton
    private lateinit var layoutMeasurable: LinearLayout
    private lateinit var etTargetValue: TextInputEditText
    private lateinit var etUnit: TextInputEditText
    private lateinit var spinnerAutoSource: Spinner
    private lateinit var btnSave: MaterialButton

    private val autoSourceOptions = listOf(
        "None (Manual Check-in)" to HabitEntity.SOURCE_NONE,
        "Health Connect: Steps" to HabitEntity.SOURCE_HEALTH_STEPS,
        "Health Connect: Sleep Duration" to HabitEntity.SOURCE_HEALTH_SLEEP,
        "Usage Stats: Screen Time" to HabitEntity.SOURCE_USAGE_SCREEN_TIME,
        "Tapasya: Focus Duration" to HabitEntity.SOURCE_TAPASYA_FOCUS,
        "Tasks: Daily Tasks Completed" to HabitEntity.SOURCE_TASK_COMPLETION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)

        repo = HabitRepository(this)
        editingHabitId = intent.getLongExtra("habit_id", 0L)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = if (editingHabitId > 0) "Edit Habit" else "New Habit"
        toolbar.setNavigationOnClickListener { finish() }

        etName = findViewById(R.id.et_habit_name)
        etQuestion = findViewById(R.id.et_habit_question)
        toggleType = findViewById(R.id.toggle_type)
        btnBoolean = findViewById(R.id.btn_type_boolean)
        btnMeasurable = findViewById(R.id.btn_type_measurable)
        layoutMeasurable = findViewById(R.id.layout_measurable_fields)
        etTargetValue = findViewById(R.id.et_target_value)
        etUnit = findViewById(R.id.et_unit)
        spinnerAutoSource = findViewById(R.id.spinner_auto_source)
        btnSave = findViewById(R.id.btn_save_habit)

        setupAutoSourceSpinner()
        setupTypeToggle()

        if (editingHabitId > 0) {
            loadHabitForEdit()
        } else {
            toggleType.check(R.id.btn_type_boolean)
        }

        btnSave.setOnClickListener { saveHabit() }
    }

    private fun setupAutoSourceSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            autoSourceOptions.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAutoSource.adapter = adapter
    }

    private fun setupTypeToggle() {
        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btn_type_measurable) {
                    layoutMeasurable.visibility = View.VISIBLE
                } else {
                    layoutMeasurable.visibility = View.GONE
                }
            }
        }
    }

    private fun loadHabitForEdit() {
        lifecycleScope.launch {
            val habits = repo.getActiveHabits()
            val habit = habits.find { it.id == editingHabitId } ?: return@launch

            etName.setText(habit.name)
            etQuestion.setText(habit.question)
            if (habit.type == HabitEntity.TYPE_MEASURABLE) {
                toggleType.check(R.id.btn_type_measurable)
                etTargetValue.setText(habit.targetValue.toString())
                etUnit.setText(habit.unit)
            } else {
                toggleType.check(R.id.btn_type_boolean)
            }

            val sourceIdx = autoSourceOptions.indexOfFirst { it.second == habit.autoSourceType }
            if (sourceIdx >= 0) {
                spinnerAutoSource.setSelection(sourceIdx)
            }
        }
    }

    private fun saveHabit() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
            return
        }

        val question = etQuestion.text.toString().trim()
        val isMeasurable = toggleType.checkedButtonId == R.id.btn_type_measurable
        val targetVal = etTargetValue.text.toString().toDoubleOrNull() ?: 0.0
        val unit = etUnit.text.toString().trim()

        val selectedSourcePos = spinnerAutoSource.selectedItemPosition
        val autoSourceType = if (selectedSourcePos >= 0) autoSourceOptions[selectedSourcePos].second else HabitEntity.SOURCE_NONE

        lifecycleScope.launch {
            val existing = if (editingHabitId > 0) {
                repo.getActiveHabits().find { it.id == editingHabitId }
            } else null

            val habit = HabitEntity(
                id = editingHabitId,
                uuid = existing?.uuid ?: java.util.UUID.randomUUID().toString(),
                name = name,
                question = if (question.isEmpty()) "Did you complete $name today?" else question,
                type = if (isMeasurable) HabitEntity.TYPE_MEASURABLE else HabitEntity.TYPE_BOOLEAN,
                targetValue = targetVal,
                unit = unit,
                autoSourceType = autoSourceType,
                autoSourceTarget = targetVal,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )

            repo.saveHabit(habit)
            Toast.makeText(this@CreateHabitActivity, "Habit saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
