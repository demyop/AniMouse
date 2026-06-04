package com.example.animouse.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animouse.data.model.Anime
import com.example.animouse.ui.activity.MainViewModel

class SchedulePagerAdapter(
    private val daysData: List<List<Anime>>,

    private val customBadges: Map<Int, List<MainViewModel.CustomFolderPreview>> = emptyMap() // <-- ДОБАВИЛИ
) : RecyclerView.Adapter<SchedulePagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val recyclerView = RecyclerView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutManager = LinearLayoutManager(parent.context)
        }
        return PageViewHolder(recyclerView)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // Передаем и список тайтлов, и кастомные теги дальше в адаптер!
        holder.recyclerView.adapter = ScheduleAnimeAdapter(daysData[position], customBadges)
    }

    override fun getItemCount(): Int = daysData.size
}