package com.neubofy.reality.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.reality.R
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.db.HabitEntryEntity
import com.neubofy.reality.data.repository.HabitRepository

class HabitAdapter(
    private val onToggleClick: (HabitRepository.HabitWithStatus) -> Unit,
    private val onItemClick: (HabitRepository.HabitWithStatus) -> Unit,
    private val onOptionsClick: (HabitRepository.HabitWithStatus, View) -> Unit
) : ListAdapter<HabitRepository.HabitWithStatus, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_habit_name)
        private val tvSubtext: TextView = itemView.findViewById(R.id.tv_habit_subtext)
        private val tvStreak: TextView = itemView.findViewById(R.id.tv_streak_badge)
        private val tvScore: TextView = itemView.findViewById(R.id.tv_score_percentage)
        private val ivStatus: ImageView = itemView.findViewById(R.id.iv_status_icon)
        private val btnToggle: FrameLayout = itemView.findViewById(R.id.btn_toggle_habit)
        private val ibOptions: ImageButton = itemView.findViewById(R.id.ib_habit_options)

        fun bind(item: HabitRepository.HabitWithStatus) {
            tvName.text = item.habit.name
            
            val entryVal = item.entry?.value ?: HabitEntryEntity.VALUE_NO
            val isSkip = entryVal == HabitEntryEntity.VALUE_SKIP

            val subtext = if (item.habit.type == HabitEntity.TYPE_MEASURABLE) {
                val currentVal = (entryVal / 1000.0).coerceAtLeast(0.0)
                "Progress: ${String.format("%.1f", currentVal)} / ${item.habit.targetValue} ${item.habit.unit}"
            } else if (item.habit.question.isNotEmpty()) {
                item.habit.question
            } else if (item.habit.autoSourceType != HabitEntity.SOURCE_NONE) {
                "Auto: ${item.habit.autoSourceType}"
            } else {
                "${item.habit.freqNumerator}/${item.habit.freqDenominator} days"
            }
            tvSubtext.text = subtext

            tvStreak.text = "🔥 ${item.currentStreak}"
            val scorePercent = (item.currentScore * 100).toInt()
            tvScore.text = "$scorePercent%"

            when {
                item.isCompleted -> {
                    ivStatus.setImageResource(R.drawable.baseline_check_circle_24)
                    ivStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.green_500))
                }
                isSkip -> {
                    ivStatus.setImageResource(R.drawable.baseline_remove_24)
                    ivStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.orange_500))
                }
                else -> {
                    ivStatus.setImageResource(R.drawable.baseline_radio_button_unchecked_24)
                    ivStatus.setColorFilter(ContextCompat.getColor(itemView.context, R.color.gray_light))
                }
            }

            btnToggle.setOnClickListener { onToggleClick(item) }
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onOptionsClick(item, it)
                true
            }
            ibOptions.setOnClickListener {
                onOptionsClick(item, it)
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitRepository.HabitWithStatus>() {
        override fun areItemsTheSame(oldItem: HabitRepository.HabitWithStatus, newItem: HabitRepository.HabitWithStatus): Boolean {
            return oldItem.habit.id == newItem.habit.id
        }

        override fun areContentsTheSame(oldItem: HabitRepository.HabitWithStatus, newItem: HabitRepository.HabitWithStatus): Boolean {
            return oldItem == newItem
        }
    }
}
