package com.icloud.android.webview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.icloud.android.webview.R
import com.icloud.android.webview.databinding.ItemDownloadBinding
import com.icloud.android.webview.model.DownloadItem

class DownloadAdapter(
    private val onCancelClicked: (DownloadItem) -> Unit,
    private val onOpenClicked: (DownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder>() {

    private val downloads = mutableListOf<DownloadItem>()

    fun setDownloads(newDownloads: List<DownloadItem>) {
        downloads.clear()
        downloads.addAll(newDownloads)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(downloads[position])
    }

    override fun getItemCount(): Int = downloads.size

    inner class DownloadViewHolder(private val binding: ItemDownloadBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadItem) {
            binding.apply {
                fileNameText.text = item.fileName
                progressBar.progress = item.progress
                statusText.text = DownloadItem.getStatusText(item.status)
                sizeText.text = item.getFormattedSize()

                // 设置按钮状态和文本
                when (item.status) {
                    DownloadItem.STATUS_DOWNLOADING, DownloadItem.STATUS_PENDING -> {
                        actionButton.text = "取消"
                        actionButton.setTextColor(actionButton.context.getColor(android.R.color.holo_red_dark))
                        actionButton.visibility = View.VISIBLE
                        openButton.visibility = View.GONE
                    }
                    DownloadItem.STATUS_COMPLETED -> {
                        actionButton.text = "删除"
                        actionButton.setTextColor(actionButton.context.getColor(android.R.color.holo_red_dark))
                        actionButton.visibility = View.VISIBLE
                        openButton.visibility = View.VISIBLE
                    }
                    DownloadItem.STATUS_FAILED, DownloadItem.STATUS_CANCELLED -> {
                        actionButton.text = "重试"
                        actionButton.setTextColor(actionButton.context.getColor(android.R.color.holo_blue_dark))
                        actionButton.visibility = View.VISIBLE
                        openButton.visibility = View.GONE
                    }
                }

                // 设置按钮点击事件
                actionButton.setOnClickListener {
                    onCancelClicked(item)
                }

                openButton.setOnClickListener {
                    onOpenClicked(item)
                }
            }
        }
    }
} 