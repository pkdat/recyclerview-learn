package com.example.learnandroid

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import kotlin.math.roundToInt


class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
    override fun calculateDtToFit(
        viewStart: Int,
        viewEnd: Int,
        boxStart: Int,
        boxEnd: Int,
        snapPreference: Int
    ): Int {
        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
    }

    override fun calculateTimeForScrolling(dx: Int): Int {
        // Slow down the scrolling by increasing the time required per pixel
        val baseTime = super.calculateTimeForScrolling(dx);
        val slowerTime = (baseTime * 5f).toInt();  // Increase this factor for slower scrolling

        return slowerTime;

    }
}



class MainActivity : AppCompatActivity() {
    private val TAG = "khac.dat"

    private lateinit var bigView: RecyclerView
    private lateinit var smallView: RecyclerView
    private lateinit var bigAdapter: BigAdapter
    private lateinit var smallAdapter: SmallAdapter

//    private val model: PositionViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Example data
        val items = List(10) { Item("Item ${it}") }

        // Big view
        bigView = findViewById(R.id.bigView)
        bigView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val bigSnapHelper = PagerSnapHelper()
        bigSnapHelper.attachToRecyclerView(bigView)

        bigAdapter = BigAdapter()
        bigView.adapter = bigAdapter
        bigAdapter.submitList(items)

        // Small view
        smallView = findViewById(R.id.smallView)
        smallView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val smoothScroller: SmoothScroller = CenterSmoothScroller(smallView.context)
        fun scroll(position: Int) {
            smoothScroller.targetPosition = position
            (smallView.layoutManager as LinearLayoutManager).startSmoothScroll(smoothScroller)
        }

        smallAdapter = SmallAdapter()
        smallView.adapter = smallAdapter
        smallAdapter.submitList(items)

        val smallSnapHelper = LinearSnapHelper()
        smallSnapHelper.attachToRecyclerView(smallView);

        fun scrollToCenter(percentage: Float) {
            val linearLayoutManager = smallView.layoutManager as LinearLayoutManager
            val position = Math.round(percentage)
            Log.d(TAG, "scrollToCenter: percentage $percentage, position $position")
            //Log.d(TAG, "position: $position")
            val view = linearLayoutManager.findViewByPosition(position);
            if (view != null) {
                val offset = ((percentage+0.5-position) * view.width).toInt()
                linearLayoutManager.scrollToPositionWithOffset(position, -offset)
                //Log.d(TAG, "scrollToCenter: ${(position -1 - percentage)}")
            }
        }
        fun computePosition(offset: Float = 0f):Float {
            return (bigView.computeHorizontalScrollOffset().toFloat()) / bigView.width
        }


        smallView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to avoid multiple calls
                smallView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val paddingHorizontal = smallView.width / 2
                smallView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
                scrollToCenter(0f)
            }
        })

        val gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {

                return true;
            }
        })




        var isBigScrolled = false
        var isSmallScrolled = false

        bigView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) isBigScrolled = false
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (isSmallScrolled) return;

                isBigScrolled = true;
                val x =  computePosition(dx.toFloat())
                scrollToCenter(x)
            }
        })
        smallView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) isSmallScrolled = false
            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (isBigScrolled) return

                isSmallScrolled = true;
                val snappedView = smallSnapHelper.findSnapView(recyclerView.layoutManager)
                snappedView?.let {
                    val position = recyclerView.layoutManager?.getPosition(snappedView)
                    if (position != null) bigView.scrollToPosition(position)
                }
            }
        })
    }
}

data class Item(val name: String)

object ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem
    }
}


class ItemSmallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val nameTextView: TextView = itemView.findViewById(R.id.quote)

    fun bind(item: Item) {
        nameTextView.text = item.name
    }
}

class SmallAdapter : ListAdapter<Item, ItemSmallViewHolder>(ItemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemSmallViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_small_view, parent, false)
        return ItemSmallViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemSmallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ItemBigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val nameTextView: TextView = itemView.findViewById(R.id.quote)

    fun bind(item: Item) {
        nameTextView.text = item.name
    }
}

class BigAdapter : ListAdapter<Item, ItemBigViewHolder>(ItemDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBigViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_big_view, parent, false)
        return ItemBigViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemBigViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}


