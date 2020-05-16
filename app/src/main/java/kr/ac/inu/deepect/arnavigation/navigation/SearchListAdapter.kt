package kr.ac.inu.deepect.arnavigation.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kr.ac.inu.deepect.R

class SearchListAdapter : BaseAdapter() {
    private val arrayList = ArrayList<SearchItem>()


    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getItem(position: Int): Any {
        return arrayList.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context


        val inflater : LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val convertView = inflater.inflate(R.layout.listview_search_item, parent, false)


        val tvName: TextView = convertView.findViewById(R.id.tvName) as TextView
        val tvDesc: TextView = convertView.findViewById(R.id.tvDesc) as TextView

        val item = arrayList.get(position)

        tvName.setText(item.name)
        tvDesc.setText(item.desc)

        return convertView

    }

    fun addItem(name: String?, desc: String?) {
        val item = SearchItem()
        item.name = name!!
        item.desc = desc!!
        arrayList.add(item)
    }

    fun clear() {arrayList.clear()}
}