package com.example.learnandroid

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import androidx.recyclerview.widget.RecyclerView.SmoothScroller


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


        smallView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to avoid multiple calls
                smallView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val paddingHorizontal = smallView.width / 2
                smallView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
                smallView.scrollToPosition(0)
                scroll(0)
            }
        })



//        bigView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    val snappedView = bigSnapHelper.findSnapView(recyclerView.layoutManager)
//                    snappedView?.let {
//                        val position = recyclerView.layoutManager?.getPosition(snappedView)
//                        if (position != null) {
//                            scroll(position)
//                            Log.d(TAG, "onScrollStateChanged: $position")
//                        }
//                    }
//                }
//            }
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val snappedView = bigSnapHelper.findSnapView(recyclerView.layoutManager)
//                snappedView?.let {
//                    val position = recyclerView.layoutManager?.getPosition(snappedView)
//                    if (position != null) scroll(position)
//                }
//            }
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val snappedView = bigSnapHelper.findSnapView(recyclerView.layoutManager)
//                snappedView?.let {
//                    val position = recyclerView.layoutManager?.getPosition(snappedView)
//                    if (position != null) {
//                        val view = smallView.layoutManager?.findViewByPosition(position)
//                        if (view != null) {
//                            Log.d(TAG, "position: ${snappedView.width}, view: ${view.width}")
//                            smallView.scrollBy(((dx.toDouble()/snappedView.width)*view.width).toInt(), dy)
//                        }
//                    }
//                }
//
//            }
//        })
        smallView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
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


