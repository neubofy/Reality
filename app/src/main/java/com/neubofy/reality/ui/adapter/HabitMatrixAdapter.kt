package com.neubofy.reality.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.reality.R
import com.neubofy.reality.data.db.HabitEntity
import com.neubofy.reality.data.db.HabitEntryEntity
import com.neubofy.reality.data.repository.HabitRepository

class HabitMatrixAdapter(
    private var items: List<HabitMatrixItem> = emptyList()
) : RecyclerView.Adapter<HabitMatrixAdapter.MatrixViewHolder>() {

    data class HabitMatrixItem(
        val habitStatus: HabitRepository.HabitWithStatus,
        val historyEntries: List<HabitEntryEntity?> // Past 7 or 30 days
    )

    fun updateItems(newItems: List<HabitMatrixItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatrixViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit_matrix, parent, false)
        return MatrixViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatrixViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MatrixViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_matrix_habit_name)
        private val tvStreak: TextView = itemView.findViewById(R.id.tv_matrix_streak)
        private val dotsContainer: LinearLayout = itemView.findViewById(R.id.layout_history_dots)

        fun bind(item: HabitMatrixItem) {
            tvName.text = item.habitStatus.habit.name
            tvStreak.text = "🔥 ${item.habitStatus.currentStreak}"

            dotsContainer.removeAllViews()
            val ctx = itemView.context
            val dotMargin = (3 * ctx.resources.displayMetrics.density).toInt()
            val dotSize = if (item.historyEntries.size > 14) {
                (8 * ctx.resources.displayMetrics.density).toInt()
            } else {
                (14 * ctx.resources.displayMetrics.density).toInt()
            }

            for (entry in item.historyEntries) {
                val dot = View(ctx)
                val params = LinearLayout.LayoutParams(dotSize, dotSize)
                params.setMargins(dotMargin, 0, dotMargin, 0)
                dot.layoutParams = params

                val entryVal = entry?.value ?: HabitEntryEntity.VALUE_NO
                val isCompleted = if (item.habitStatus.habit.type == HabitEntity.TYPE_MEASURABLE) {
                    val valReal = entryVal / 1000.0
                    val target = item.habitStatus.habit.targetValue
                    if (item.habitStatus.habit.targetType == HabitEntity.TARGET_AT_LEAST) {
                        valReal >= target && target > 0
                    } else {
                        entryVal != HabitEntryEntity.VALUE_NO && valReal <= target
                    }
                } else {
                    entryVal == HabitEntryEntity.VALUE_YES_MANUAL || entryVal == HabitEntryEntity.VALUE_YES_AUTO
                }
                val isSkip = entryVal == HabitEntryEntity.VALUE_SKIP

                val bgDrawable = when {
                    isCompleted -> R.drawable.circle_primary
                    isSkip -> R.drawable.circle_background
                    else -> R.drawable.circle_background_dark
                }
                dot.setBackgroundResource(bgDrawable)

                val colorRes = when {
                    isCompleted -> R.color.green_500
                    isSkip -> R.color.orange_500
                    else -> R.color.md_theme_surfaceContainerHighest
                }
                dot.backgroundTintList = ContextCompat.getColorStateList(ctx, colorRes)

                dotsContainer.addView(dot)
            }
        }
    }
}
