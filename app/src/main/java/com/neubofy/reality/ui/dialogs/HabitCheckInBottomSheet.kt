package com.neubofy.reality.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.neubofy.reality.R
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.db.HabitEntryEntity
import com.neubofy.reality.data.repository.HabitRepository

class HabitCheckInBottomSheet(
    private val habitStatus: HabitRepository.HabitWithStatus,
    private val onSaved: (value: Int, measurableVal: Double?, notes: String) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedValue: Int = habitStatus.entry?.value ?: HabitEntryEntity.VALUE_NO
    private var currentMeasurableVal: Double = (habitStatus.entry?.value ?: 0) / 1000.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_habit_check_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.dialog_tv_habit_name)
        val tvQuestion = view.findViewById<TextView>(R.id.dialog_tv_question)
        val layoutBoolean = view.findViewById<LinearLayout>(R.id.layout_boolean_actions)
        val btnYes = view.findViewById<MaterialButton>(R.id.btn_action_yes)
        val btnSkip = view.findViewById<MaterialButton>(R.id.btn_action_skip)
        val btnNo = view.findViewById<MaterialButton>(R.id.btn_action_no)

        val layoutMeasurable = view.findViewById<LinearLayout>(R.id.layout_measurable_actions)
        val tvMeasurableProgress = view.findViewById<TextView>(R.id.tv_measurable_progress)
        val btnMinus5 = view.findViewById<MaterialButton>(R.id.btn_step_minus_5)
        val btnMinus1 = view.findViewById<MaterialButton>(R.id.btn_step_minus_1)
        val btnPlus1 = view.findViewById<MaterialButton>(R.id.btn_step_plus_1)
        val btnPlus5 = view.findViewById<MaterialButton>(R.id.btn_step_plus_5)
        val etCustom = view.findViewById<TextInputEditText>(R.id.et_measurable_custom)

        val etNotes = view.findViewById<TextInputEditText>(R.id.dialog_et_notes)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_dialog_save)

        tvName.text = habitStatus.habit.name
        tvQuestion.text = habitStatus.habit.question.ifEmpty { "Did you complete this habit today?" }
        etNotes.setText(habitStatus.entry?.notes ?: "")

        if (habitStatus.habit.type == HabitEntity.TYPE_MEASURABLE) {
            layoutBoolean.visibility = View.GONE
            layoutMeasurable.visibility = View.VISIBLE

            fun updateMeasurableText() {
                tvMeasurableProgress.text = "${String.format("%.1f", currentMeasurableVal)} / ${habitStatus.habit.targetValue} ${habitStatus.habit.unit}"
                etCustom.setText(String.format("%.1f", currentMeasurableVal))
            }
            updateMeasurableText()

            btnMinus5.setOnClickListener {
                currentMeasurableVal = (currentMeasurableVal - 5.0).coerceAtLeast(0.0)
                updateMeasurableText()
            }
            btnMinus1.setOnClickListener {
                currentMeasurableVal = (currentMeasurableVal - 1.0).coerceAtLeast(0.0)
                updateMeasurableText()
            }
            btnPlus1.setOnClickListener {
                currentMeasurableVal += 1.0
                updateMeasurableText()
            }
            btnPlus5.setOnClickListener {
                currentMeasurableVal += 5.0
                updateMeasurableText()
            }
        } else {
            layoutBoolean.visibility = View.VISIBLE
            layoutMeasurable.visibility = View.GONE

            fun updateButtonStyles() {
                when (selectedValue) {
                    HabitEntryEntity.VALUE_YES_MANUAL, HabitEntryEntity.VALUE_YES_AUTO -> {
                        btnYes.alpha = 1.0f
                        btnSkip.alpha = 0.5f
                        btnNo.alpha = 0.5f
                    }
                    HabitEntryEntity.VALUE_SKIP -> {
                        btnYes.alpha = 0.5f
                        btnSkip.alpha = 1.0f
                        btnNo.alpha = 0.5f
                    }
                    else -> {
                        btnYes.alpha = 0.5f
                        btnSkip.alpha = 0.5f
                        btnNo.alpha = 1.0f
                    }
                }
            }
            updateButtonStyles()

            btnYes.setOnClickListener {
                selectedValue = HabitEntryEntity.VALUE_YES_MANUAL
                updateButtonStyles()
            }
            btnSkip.setOnClickListener {
                selectedValue = HabitEntryEntity.VALUE_SKIP
                updateButtonStyles()
            }
            btnNo.setOnClickListener {
                selectedValue = HabitEntryEntity.VALUE_NO
                updateButtonStyles()
            }
        }

        btnSave.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            if (habitStatus.habit.type == HabitEntity.TYPE_MEASURABLE) {
                val customVal = etCustom.text.toString().toDoubleOrNull() ?: currentMeasurableVal
                onSaved(HabitEntryEntity.VALUE_YES_MANUAL, customVal, notes)
            } else {
                onSaved(selectedValue, null, notes)
            }
            dismiss()
        }
    }
}
