package com.v2reading.reader.utils

import com.v2reading.reader.R
import com.v2reading.reader.help.glide.ImageLoader
import com.v2reading.reader.model.BannerBean
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import splitties.init.appCtx

/**
 * Created by shaowen.jiang@wellcare.cn on 2022/9/16.
 */
class SimpleAdapter : BaseBannerAdapter<BannerBean>() {

    override fun bindData(
        holder: BaseViewHolder<BannerBean>,
        data: BannerBean?,
        position: Int,
        pageSize: Int
    ) {
        kotlin.runCatching {
            val bitmap = ImageLoader.loadBitmap(appCtx, data!!.url).submit()
            holder.setImageBitmap(R.id.banner_image, bitmap.get())
        }.getOrElse {
            if (data?.action == "update") {
                holder.setImageResource(R.id.banner_image, R.drawable.banner)
            } else
                holder.setImageResource(R.id.banner_image, R.drawable.banner_default)
        }

    }


    override fun getLayoutId(viewType: Int): Int {
        return R.layout.banner_item
    }
}
