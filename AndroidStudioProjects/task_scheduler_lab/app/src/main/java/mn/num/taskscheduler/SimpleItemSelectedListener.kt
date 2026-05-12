package mn.num.taskscheduler

import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(
    private val onItemSelectedAction: (position: Int) -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        onItemSelectedAction(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // хийх зүйлгүй
    }
}