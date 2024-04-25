package dev.brahmkshatriya.echo.extension

import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.EchoMediaItem

class EchoMediaItemComparator : DiffUtil.ItemCallback<EchoMediaItem>() {
    override fun areItemsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EchoMediaItem, newItem: EchoMediaItem): Boolean {
        return oldItem == newItem
    }

}
