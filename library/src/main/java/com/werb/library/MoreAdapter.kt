package com.werb.library

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import com.werb.library.action.DataAction
import com.werb.library.action.MoreClickListener
import com.werb.library.extension.AlphaAnimation
import com.werb.library.extension.AnimExtension
import com.werb.library.extension.MoreAnimation
import com.werb.library.link.MoreLink
import com.werb.library.link.MoreLinkManager
import com.werb.library.link.MultiLink
import com.werb.library.link.RegisterItem
import kotlin.reflect.KClass


/**
 * [MoreAdapter] build viewHolder with data
 * Created by wanbo on 2017/7/2.
 */
class MoreAdapter : Adapter<MoreViewHolder<Any>>(), MoreLink, AnimExtension, DataAction {

    val list: MutableList<Any> = mutableListOf()
    private val linkManager: MoreLink by lazy { MoreLinkManager() }
    private var animation: MoreAnimation? = null
    private var animDuration = 250L
    private var startAnimPosition = 0
    private var firstShow = false
    private var lastAnimPosition = -1
    private var linearInterpolator = LinearInterpolator()

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MoreViewHolder<Any> {
        val viewHolderClass = createViewHolder(viewType)
        val con = viewHolderClass.getConstructor(View::class.java)
        val view = LayoutInflater.from(parent?.context).inflate(viewType, parent, false)
        return con.newInstance(view) as MoreViewHolder<Any>
    }

    override fun onBindViewHolder(holder: MoreViewHolder<Any>, position: Int) {
        val any = list[position]
        holder.clickListener = bindClickListener(holder)
        holder.bindData(any)
    }

    override fun onBindViewHolder(holder: MoreViewHolder<Any>, position: Int, payloads: MutableList<Any>?) {
        payloads?.let {
            val any = list[position]
            holder.clickListener = bindClickListener(holder)
            holder.bindData(any, payloads)
        } ?: onBindViewHolder(holder, position)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewRecycled(holder: MoreViewHolder<Any>) {
        holder.unBindData()
    }

    fun attachTo(view: RecyclerView): MoreLink {
        view.adapter = this
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun loadData(data: Any) {
        if (data is List<*>) {
            var position = 0
            if (list.size > 0) {
                position = list.size
            }
            list.addAll(position, data as Collection<Any>)
            notifyItemRangeInserted(position, data.size)
        } else {
            list.add(data)
            notifyItemInserted(itemCount - 1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun loadData(index: Int, data: Any) {
        if (data is List<*>) {
            list.addAll(index, data as Collection<Any>)
            notifyItemRangeInserted(index, data.size)
        } else {
            list.add(index, data)
            notifyItemInserted(index)
        }
    }

    override fun getData(position: Int): Any = list[position]

    override fun removeAllData() {
        if (list.isNotEmpty()) {
            list.clear()
            notifyDataSetChanged()
        }
    }

    override fun removeAllNotRefresh() {
        list.clear()
    }

    override fun removeData(data: Any) {
        val contains = list.contains(data)
        if (contains) {
            val index = list.indexOf(data)
            removeData(index)
        }
    }

    override fun removeData(position: Int) {
        if (list.size == 0) {
            return
        }
        if (position >= 0 && position <= list.size - 1) {
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun replaceData(position: Int, data: Any) {
        list.removeAt(position)
        list.add(position, data)
        notifyItemChanged(position)
    }

    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int {
        val any = list[position]
        return attachViewTypeLayout(any)
    }

    override fun onViewAttachedToWindow(holder: MoreViewHolder<Any>) {
        super.onViewAttachedToWindow(holder)
        addAnimation(holder)
    }

    /** [renderWithAnimation] user default animation AlphaAnimation */
    override fun renderWithAnimation(): MoreAdapter {
        this.animation = AlphaAnimation()
        return this
    }

    /** [renderWithAnimation] user custom animation */
    override fun renderWithAnimation(animation: MoreAnimation): MoreAdapter {
        this.animation = animation
        return this
    }

    /** [addAnimation] addAnimation when view attached to windows */
    override fun addAnimation(holder: MoreViewHolder<Any>) {
        this.animation?.let {
            if (holder.layoutPosition < startAnimPosition) {
                return
            }
            if (!firstShow || holder.layoutPosition > lastAnimPosition) {
                val animators = it.getItemAnimators(holder.itemView)
                for (anim in animators) {
                    anim.setDuration(animDuration).start()
                    anim.interpolator = linearInterpolator
                }
                lastAnimPosition = holder.layoutPosition
            }
        }
    }

    /** [duration] set animation duration */
    override fun duration(duration: Long): MoreAdapter {
        this.animDuration = duration
        return this
    }

    /** [firstShowAnim] set isShow animation when first display  */
    override fun firstShowAnim(firstShow: Boolean): MoreAdapter {
        this.firstShow = firstShow
        return this
    }

    /** [startAnimPosition] set animation start position */
    override fun startAnimPosition(position: Int): MoreAdapter {
        this.startAnimPosition = position
        return this
    }

    /** [register] register viewType which single link with model  */
    override fun register(registerItem: RegisterItem) {
        linkManager.register(registerItem)
    }

    /** [multiRegister] multiRegister viewType like one2more case , user MultiLink to choose which one is we need */
    override fun multiRegister(link: MultiLink<*>) {
        linkManager.multiRegister(link)
    }

    /** [attachViewTypeLayout]  find viewType layout by item of list */
    override fun attachViewTypeLayout(any: Any): Int = linkManager.attachViewTypeLayout(any)

    /** [createViewHolder]  find viewType by layout */
    override fun createViewHolder(type: Int): Class<out MoreViewHolder<*>> = linkManager.createViewHolder(type)

    override fun bindClickListener(holder: MoreViewHolder<*>): MoreClickListener? = linkManager.bindClickListener(holder)

    /** [userSoleRegister] register sole global viewType */
    override fun userSoleRegister() = linkManager.userSoleRegister()
}