package it.palsoftware.pastiera.inputmethod.suggestions.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.palsoftware.pastiera.inputmethod.japanese.MozcBridge

/** Japanese-only candidate renderer. English suggestions stay in FullSuggestionsBar. */
class MozcScrollableCandidateView(
    context: android.content.Context,
    private val onCandidateClick: (position: Int, item: MozcBridge.CandidateItem) -> Unit
) : RecyclerView(context) {
    private val candidateLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    private val candidateAdapter = CandidateAdapter(context)
    private val baseHorizontalPaddingPx = dp(4)

    init {
        layoutManager = candidateLayoutManager
        adapter = candidateAdapter
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setPadding(baseHorizontalPaddingPx, 0, baseHorizontalPaddingPx, 0)
        addItemDecoration(object : ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                if (parent.getChildAdapterPosition(view) > 0) outRect.left = dp(2)
            }
        })
    }

    fun update(items: List<MozcBridge.CandidateItem>, focusedIndex: Int) {
        candidateAdapter.replace(items, focusedIndex)
        visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        post { applyContentAlignmentAndFocus() }
    }

    fun clear() {
        candidateAdapter.replace(emptyList(), -1)
        setHorizontalContentPadding(baseHorizontalPaddingPx)
        candidateLayoutManager.scrollToPosition(0)
        visibility = View.GONE
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width != oldWidth) {
            post { applyContentAlignmentAndFocus() }
        }
    }

    /**
     * Centers the complete candidate group only when its measured content fits in this view.
     * The view width already reflects FullSuggestionsBar's mode-specific insets.
     */
    private fun applyContentAlignmentAndFocus() {
        if (width <= 0 || candidateAdapter.itemCount == 0) return

        val availableWidth = (width - baseHorizontalPaddingPx * 2).coerceAtLeast(0)
        val contentWidth = candidateAdapter.requiredContentWidth()
        val fits = contentWidth <= availableWidth
        val sidePadding = if (fits) {
            baseHorizontalPaddingPx + ((availableWidth - contentWidth) / 2)
        } else {
            baseHorizontalPaddingPx
        }
        setHorizontalContentPadding(sidePadding)

        val focusedIndex = candidateAdapter.focusedCandidateIndex()
        if (fits) {
            // Reset any previous overflow scroll offset without inferring a focused candidate.
            candidateLayoutManager.scrollToPosition(0)
        } else if (focusedIndex in 0 until candidateAdapter.itemCount) {
            candidateLayoutManager.smoothScrollToPosition(this, null, focusedIndex)
        }
    }

    private fun setHorizontalContentPadding(sidePadding: Int) {
        if (paddingLeft == sidePadding && paddingRight == sidePadding) return
        setPadding(sidePadding, paddingTop, sidePadding, paddingBottom)
    }

    private inner class CandidateAdapter(
        private val context: android.content.Context
    ) : RecyclerView.Adapter<CandidateViewHolder>() {
        private var items: List<MozcBridge.CandidateItem> = emptyList()
        private var focusedIndex: Int = -1

        fun replace(next: List<MozcBridge.CandidateItem>, focused: Int) {
            items = next.toList()
            focusedIndex = focused
            notifyDataSetChanged()
        }

        fun focusedCandidateIndex(): Int = focusedIndex

        fun requiredContentWidth(): Int {
            val itemWidth = items.sumOf { item ->
                measureCandidateText(item.value)
            }
            val itemSpacing = dp(2) * (items.size - 1).coerceAtLeast(0)
            return itemWidth + itemSpacing
        }

        private fun measureCandidateText(value: String): Int {
            val text = createCandidateTextView().apply { this.text = value }
            val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            text.measure(unspecified, unspecified)
            return text.measuredWidth
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
            return CandidateViewHolder(createCandidateTextView())
        }

        private fun createCandidateTextView(): TextView = TextView(context).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(4), dp(14), dp(4))
            minHeight = dp(36)
            maxLines = 1
            isClickable = true
            isFocusable = false
        }

        override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
            val item = items[position]
            val selected = position == focusedIndex
            holder.text.text = item.value
            holder.text.setTextColor(Color.WHITE)
            holder.text.background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(if (selected) Color.rgb(65, 100, 160) else Color.rgb(45, 45, 50))
            }
            holder.text.setOnClickListener {
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION && current < items.size) {
                    onCandidateClick(current, items[current])
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private class CandidateViewHolder(val text: TextView) : RecyclerView.ViewHolder(text)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
