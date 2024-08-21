package io.esper.android.network

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.esper.android.files.R
import io.esper.android.network.model.UrlAndPortItem

class NetworkResultAdapter(private val urlAndPortItems: List<UrlAndPortItem>) :
    RecyclerView.Adapter<NetworkResultAdapter.ResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.network_result_item, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val resultItem = urlAndPortItems[position]
        holder.bind(resultItem)
    }

    override fun getItemCount(): Int = urlAndPortItems.size

    fun scrollToLastPosition(recyclerView: RecyclerView) {
        recyclerView.scrollToPosition(itemCount - 1)
    }

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewUrl: TextView = itemView.findViewById(R.id.textViewUrl)
        private val imageViewStatus: ImageView = itemView.findViewById(R.id.imageViewStatus)

        fun bind(urlAndPortItem: UrlAndPortItem) {
            textViewUrl.text = "${urlAndPortItem.url}:${urlAndPortItem.port}"
            textViewUrl.isSelected = true

            if (urlAndPortItem.isAccessible) {
                imageViewStatus.setImageResource(R.drawable.success_icon)
                imageViewStatus.visibility = View.VISIBLE
            } else {
                imageViewStatus.setImageResource(R.drawable.failure_icon)
                imageViewStatus.visibility = View.VISIBLE
            }
        }
    }
}