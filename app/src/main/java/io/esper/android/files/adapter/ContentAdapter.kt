@file:Suppress("DEPRECATION")

package io.esper.android.files.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.esper.android.files.R
import io.esper.android.files.model.AllContent
import io.esper.android.files.ui.AutoGoneTextView
import io.esper.android.files.ui.DisabledAlphaImageView
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.UploadDownloadUtils
import me.zhanghai.android.foregroundcompat.ForegroundLinearLayout
import java.text.DecimalFormat
import java.util.Locale

class ContentAdapter : RecyclerView.Adapter<ContentAdapter.MyViewHolder>(), Filterable {

    private lateinit var mContext: Context
    private var mItemContentList: MutableList<AllContent> = mutableListOf()
    var mItemContentListOriginal: MutableList<AllContent> = mutableListOf()
    private lateinit var sharedPrefManaged: SharedPreferences

    // Extension to drawable resource map
    private val extensionToDrawableMap = mapOf(
        "apk" to R.drawable.apk,
        "zip" to R.drawable.zip,
        "rar" to R.drawable.zip,
        "pdf" to R.drawable.pdf,
        "xls" to R.drawable.xls,
        "xlsx" to R.drawable.xls,
        "ppt" to R.drawable.ppt,
        "pptx" to R.drawable.ppt,
        "doc" to R.drawable.doc,
        "docx" to R.drawable.doc,
        "csv" to R.drawable.csv,
        "vcf" to R.drawable.vcf,
        "json" to R.drawable.json,
        "txt" to R.drawable.txt,
        "html" to R.drawable.html,
        "mp3" to R.drawable.mp3,
        "xml" to R.drawable.xml,
        "pem" to R.drawable.cert,
        "crt" to R.drawable.cert
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        mContext = parent.context
        sharedPrefManaged = mContext.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.content_item, parent, false)
        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = mItemContentList[position]
        showContentElement(currentItem, holder)
    }

    private fun showContentElement(currentItem: AllContent, holder: MyViewHolder) {
        val fileName = currentItem.name?.lowercase(Locale.getDefault()) ?: ""

        val drawableResource = extensionToDrawableMap.entries.find {
            fileName.endsWith(it.key)
        }?.value

        if (drawableResource != null) {
            holder.imgThumbnail.setImageResource(drawableResource)
        } else {
            if (currentItem.kind?.contains("image", true) == true) {
                val isGif = currentItem.kind!!.contains(".gif", true)
                loadImage(mContext, currentItem.download_url.toString(), isGif, holder.imgThumbnail)
            } else {
                holder.imgThumbnail.setImageResource(R.drawable.file)
            }
        }

        holder.txtTitle.text = currentItem.name
        holder.txtInfo.text = formatFileSize(currentItem.size!!.toLong())

        // Set download button and image based on conditions (simplified)
        holder.downloadBtn.setOnClickListener {
            if (GeneralUtils.hasActiveInternetConnection(mContext)) {
                holder.downloadBtn.isEnabled = false
                holder.downloadImg.setImageResource(R.drawable.ic_complete)
                UploadDownloadUtils.startDownload(mContext, currentItem)
            } else {
                GeneralUtils.showNoInternetDialog(mContext)
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val precision = DecimalFormat("0.00")
        return when {
            size > 1073741823 -> precision.format(size.toFloat() / 1073741824) + " GB"
            size > 1048575 -> precision.format(size.toFloat() / 1048576) + " MB"
            size > 1023 -> precision.format(size.toFloat() / 1024) + " KB"
            else -> "$size Bytes"
        }
    }

    override fun getItemCount(): Int = mItemContentList.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtTitle: TextView = itemView.findViewById(R.id.txt_item_name)
        var txtInfo: AutoGoneTextView = itemView.findViewById(R.id.txt_item_info)
        var imgThumbnail: DisabledAlphaImageView = itemView.findViewById(R.id.img_item_thumbnail)
        var downloadBtn: ForegroundLinearLayout = itemView.findViewById(R.id.downloadBtn)
        var downloadImg: ImageView = itemView.findViewById(R.id.downloadImg)

        init {
            txtTitle.apply {
                ellipsize = TextUtils.TruncateAt.MARQUEE
                isSingleLine = true
                isSelected = true
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                mItemContentList = if (charString.isEmpty()) {
                    mItemContentListOriginal
                } else {
                    val filteredList = mItemContentListOriginal.filter {
                        it.name!!.toLowerCase(Locale.getDefault()).contains(
                            charString.toLowerCase(Locale.getDefault())
                        )
                    }.toMutableList()
                    filteredList
                }

                return FilterResults().apply { values = mItemContentList }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(
                charSequence: CharSequence?, filterResults: FilterResults?
            ) {
                mItemContentList = filterResults?.values as MutableList<AllContent>
                notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setContentItems(context: Context, contentItems: MutableList<AllContent>?) {
        this.mItemContentList = contentItems ?: mutableListOf()
        this.mItemContentListOriginal = ArrayList(this.mItemContentList)
        sharedPrefManaged = context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        sharedPrefManaged.getBoolean(Constants.SORT_ASCENDING, true).let {
            sortItems(it)
        }
    }

    fun sortItems(ascending: Boolean) {
        if (mItemContentList.isNotEmpty()) {
            mItemContentList.sortWith(if (ascending) sortNames() else sortNamesDesc())
            notifyDataSetChanged()
        }
    }

    private fun sortNames(): Comparator<AllContent> =
        Comparator { o1, o2 -> o1.name!!.compareTo(o2.name!!, true) }

    private fun sortNamesDesc(): Comparator<AllContent> =
        Comparator { o1, o2 -> o2.name!!.compareTo(o1.name!!, true) }

    private fun loadImage(context: Context, url: String, isGif: Boolean, imageView: ImageView) {
        val requestOptions =
            RequestOptions().timeout(15000).diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.file).centerCrop().priority(Priority.HIGH)

        val glideRequest = Glide.with(context).setDefaultRequestOptions(requestOptions)
        if (isGif) {
            glideRequest.asGif().load(url).into(imageView)
        } else {
            glideRequest.load(url).into(imageView)
        }
    }
}
