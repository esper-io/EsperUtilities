@file:Suppress("DEPRECATION")

package io.esper.android.files.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
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
import com.downloadservice.filedownloadservice.manager.FileDownloadManager
import io.esper.android.files.R
import io.esper.android.files.compat.isSingleLineCompat
import io.esper.android.files.model.AllContent
import io.esper.android.files.model.Item
import io.esper.android.files.ui.AutoGoneTextView
import io.esper.android.files.ui.DisabledAlphaImageView
import io.esper.android.files.util.Constants
import io.esper.android.files.util.FileUtils
import io.esper.android.files.util.GeneralUtils
import me.zhanghai.android.foregroundcompat.ForegroundLinearLayout
import java.text.DecimalFormat
import java.util.Locale


class ContentAdapter : RecyclerView.Adapter<ContentAdapter.MyViewHolder>(), Filterable {

    private var prevCharLength: Int = 0
    private var mContext: Context? = null
    private var mItemContentList: MutableList<AllContent>? = ArrayList()
    private var inflater: LayoutInflater? = null
    private var mItemContentListFiltered: MutableList<AllContent>? = ArrayList()
    private var mItemContentListOriginal: MutableList<AllContent>? = ArrayList()
    private var mItemContentPrevList: MutableList<AllContent>? = ArrayList()
    private var mItemContentReadyForPrev: MutableList<AllContent>? = ArrayList()
    private var sharedPrefManaged: SharedPreferences? = null

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): MyViewHolder {
        mContext = parent.context
        inflater = LayoutInflater.from(mContext)
        val view: View = inflater!!.inflate(R.layout.content_item, parent, false)
        mItemContentListOriginal = mItemContentList
        sharedPrefManaged = mContext!!.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: MyViewHolder, position: Int
    ) {
        val currentItem = mItemContentList!![position]
        if (sharedPrefManaged == null) {
            sharedPrefManaged = mContext!!.getSharedPreferences(
                Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
            )
        }
        showContentElement(currentItem, holder)
    }

    private fun showContentElement(currentItem: AllContent, holder: MyViewHolder) {
        val newItemsList = GeneralUtils.getInternalStoragePath(sharedPrefManaged!!)
            ?.let { FileUtils.populateItemList(it) }
        when {
            currentItem.name!!.endsWith(
                ".apk", ignoreCase = true
            ) -> {
                holder.imgThumbnail.setImageResource(R.drawable.apk)
            }

            currentItem.name!!.endsWith(".zip", ignoreCase = true) || currentItem.name!!.endsWith(
                ".rar", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.zip)

            currentItem.name!!.endsWith(
                ".pdf", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.pdf)

            currentItem.name!!.endsWith(".xls", ignoreCase = true) || currentItem.name!!.endsWith(
                ".xlsx", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.xls)

            currentItem.name!!.endsWith(".ppt", ignoreCase = true) || currentItem.name!!.endsWith(
                ".pptx", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.ppt)

            currentItem.name!!.endsWith(".doc", ignoreCase = true) || currentItem.name!!.endsWith(
                ".docx", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.doc)

            currentItem.name!!.endsWith(
                ".csv", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.csv)

            currentItem.name!!.endsWith(
                ".vcf", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.vcf)

            currentItem.name!!.endsWith(
                ".json", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.json)

            currentItem.name!!.endsWith(
                ".txt", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.txt)

            currentItem.name!!.endsWith(
                ".html", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.html)

            currentItem.name!!.endsWith(
                ".mp3", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.mp3)

            currentItem.name!!.endsWith(
                ".xml", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.xml)

            currentItem.name!!.endsWith(".pem", ignoreCase = true) || currentItem.name!!.endsWith(
                ".crt", ignoreCase = true
            ) -> holder.imgThumbnail.setImageResource(R.drawable.cert)

            else -> {
                if (currentItem.kind?.contains("image", true) == true) {
                    val requestOptions =
                        RequestOptions().timeout(15000).diskCacheStrategy(DiskCacheStrategy.ALL)
                    if (currentItem.kind!!.contains(".gif", true)) {
                        mContext?.let {
                            Glide.with(it).setDefaultRequestOptions(requestOptions).asGif()
                                .load(currentItem.download_url.toString())
                                .placeholder(R.drawable.file).centerCrop().priority(Priority.HIGH)
                                .into(holder.imgThumbnail)
                        }
                    } else {
                        mContext?.let {
                            Glide.with(it).setDefaultRequestOptions(requestOptions)
                                .load(currentItem.download_url.toString())
                                .placeholder(R.drawable.file).centerCrop().priority(Priority.HIGH)
                                .into(holder.imgThumbnail)
                        }
                    }
                } else {
                    holder.imgThumbnail.setImageResource(R.drawable.file)
                }
            }
        }
        holder.txtInfo.apply {
            if (isSingleLineCompat) {
                val nameEllipsize = nameEllipsize
                ellipsize = nameEllipsize
                isSelected = nameEllipsize == TextUtils.TruncateAt.MARQUEE
            }
        }
        holder.txtTitle.text = currentItem.name
        val precision = DecimalFormat("0.00")
        holder.txtInfo.text = when {
            currentItem.size!!.toFloat() > 1073741823 -> precision.format(currentItem.size!!.toFloat() / 1073741824.toFloat()) + " GB"
            currentItem.size!!.toFloat() > 1048575 -> precision.format(currentItem.size!!.toFloat() / 1048576.toFloat()) + " MB"
            currentItem.size!!.toFloat() > 1023 -> precision.format(currentItem.size!!.toFloat() / 1024.toFloat()) + " KB"
            else -> currentItem.size!! + " Bytes"
        } // x Bytes

        holder.downloadBtn.setOnClickListener {}
        if (newItemsList?.let {
                containsItemWithSameNameAndSize(
                    it,
                    currentItem.name!!,
                    currentItem.size!!
                )
            } == true) {
            holder.downloadBtn.isEnabled = false
            holder.downloadImg.setImageResource(R.drawable.ic_complete)
        } else {
            holder.downloadBtn.isEnabled = true
            holder.downloadImg.setImageResource(R.drawable.ic_cloud_download)
        }
        holder.downloadBtn.setOnClickListener {
            // Todo: Uncomment this block to show to user that the file is already downloaded
//            if (newItemsList?.let { it1 -> containsName(it1, currentItem.name) } == true) {
//                Log.d("ContentAdapter", "Already downloaded")
//                mContext?.showToast(
//                    "${mContext!!.getString(R.string.already_downloaded)} ${
//                        FileUtils.getFilePath(
//                            mContext!!, currentItem.name.toString()
//                        )
//                    }", Toast.LENGTH_LONG
//                )
//                return@setOnClickListener
//            }
            holder.downloadBtn.isEnabled = false
            holder.downloadImg.setImageResource(R.drawable.ic_complete)
            GeneralUtils.getInternalStoragePath(sharedPrefManaged!!)?.let { it1 ->
                FileDownloadManager.initDownload(
                    mContext!!,
                    currentItem.download_url.toString(),
                    it1,
                    currentItem.name.toString()
                )
            }
        }
        GeneralUtils.setFadeAnimation(holder.itemView)
    }

    // Function to check if an item with the same name and size exists in the list
    private fun containsItemWithSameNameAndSize(
        itemList: List<Item>, currentItemName: String, currentItemSize: String
    ): Boolean {
        val matchingItems = itemList.filter { it.name == currentItemName }
        for (item in matchingItems) {
            Log.d("ContentAdapter", "Matching item size: ${item.size}, Current item size: $currentItemSize")
            if (item.size == currentItemSize) {
                return true
            }
        }
        return false
    }


    override fun getItemCount(): Int {
        return mItemContentList!!.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var txtTitle: TextView = itemView.findViewById<View>(R.id.txt_item_name) as TextView
        var txtInfo: AutoGoneTextView =
            itemView.findViewById<View>(R.id.txt_item_info) as AutoGoneTextView
        var imgThumbnail: DisabledAlphaImageView =
            itemView.findViewById<View>(R.id.img_item_thumbnail) as DisabledAlphaImageView
        var downloadBtn: ForegroundLinearLayout =
            itemView.findViewById<View>(R.id.downloadBtn) as ForegroundLinearLayout
        var downloadImg: ImageView = itemView.findViewById<View>(R.id.downloadImg) as ImageView

        init {
            txtTitle.ellipsize = TextUtils.TruncateAt.MARQUEE
            txtTitle.isSingleLine = true
            txtTitle.isSelected = true
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                if (charSequence.toString().isEmpty() || charSequence.toString() == "") {
                    mItemContentList = mItemContentListOriginal!!
                } else if (charSequence.toString().length < prevCharLength) mItemContentList =
                    mItemContentPrevList!!
                val filteredList: MutableList<AllContent> = ArrayList()
                for (row in mItemContentList!!) {
                    if (row.name!!.toLowerCase(Locale.getDefault()).contains(
                            charSequence.toString().toLowerCase(Locale.getDefault())
                        )
                    ) filteredList.add(row)
                }
                mItemContentListFiltered = filteredList

                prevCharLength = charSequence.toString().length
                val filterResults = FilterResults()
                filterResults.values = mItemContentListFiltered
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                charSequence: CharSequence?, filterResults: FilterResults
            ) {
                mItemContentPrevList =
                    if (mItemContentListFiltered!!.size <= mItemContentPrevList!!.size) mItemContentReadyForPrev
                    else mItemContentListFiltered

                mItemContentReadyForPrev = mItemContentPrevList
                mItemContentListFiltered = filterResults.values as MutableList<AllContent>?
                mItemContentList = mItemContentListFiltered!!

                notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setContentItems(context: Context, contentItems: MutableList<AllContent>?) {
        this.mItemContentList = contentItems
        if (sharedPrefManaged == null) {
            sharedPrefManaged = context.getSharedPreferences(
                Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
            )
        }
        sharedPrefManaged?.getBoolean(Constants.SORT_ASCENDING, true)?.let {
            sortItems(it)
        } ?: run { notifyDataSetChanged() }
    }

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    private var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    //Sorting names ascending
    private fun sortNames(): Comparator<AllContent> =
        Comparator { o1, o2 -> o1!!.name!!.compareTo(o2!!.name!!, true) }

    //Sorting names descending
    private fun sortNamesDesc(): Comparator<AllContent> =
        Comparator { o1, o2 -> o2!!.name!!.compareTo(o1!!.name!!, true) }

    fun sortItems(ascending: Boolean) {
        if (mItemContentList != null && mItemContentList!!.isNotEmpty()) {
            val editor = sharedPrefManaged?.edit()
            editor?.putBoolean(Constants.SORT_ASCENDING, ascending)?.apply()
            if (ascending) {
                mItemContentList!!.sortWith(sortNames())
            } else {
                mItemContentList!!.sortWith(sortNamesDesc())
            }
            notifyDataSetChanged()
        }
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()
    }
}