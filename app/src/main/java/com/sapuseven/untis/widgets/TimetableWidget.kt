package com.sapuseven.untis.widgets

import android.appwidget.AppWidgetManager
import android.util.Log
import com.sapuseven.untis.R
import com.sapuseven.untis.data.databases.UserDatabase
import com.sapuseven.untis.data.timetable.TimegridItem
import com.sapuseven.untis.helpers.TimetableListSorter.formatItems
import com.sapuseven.untis.helpers.timetable.TimetableDatabaseInterface
import com.sapuseven.untis.helpers.timetable.TimetableLoader
import com.sapuseven.untis.interfaces.TimetableDisplay
import com.sapuseven.untis.models.untis.UntisDate
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.lang.ref.WeakReference

class TimetableWidget : BaseWidget() {

    private lateinit var timetableDatabaseInterface: TimetableDatabaseInterface
    private lateinit var timetableLoader: TimetableLoader

    override fun updateAppWidget(appWidgetId: Int) {
        super.updateAppWidget(appWidgetId)
        //appWidgetManager.updateAppWidget(appWidgetId, views)

        timetableDatabaseInterface = TimetableDatabaseInterface(userDatabase, user?.id ?: return)
        timetableLoader = TimetableLoader(WeakReference(context), TimetableWidgetTimetableDisplay(appWidgetManager, appWidgetId, user), user ?: return, timetableDatabaseInterface)
        loadTimetable()
    }

    private fun loadTimetable(force: Boolean = false) {
        val today = UntisDate.fromLocalDate(LocalDate.now())
        val flags = if (force) TimetableLoader.FLAG_LOAD_SERVER else TimetableLoader.FLAG_LOAD_CACHE
        timetableLoader.load(TimetableLoader.TimetableLoaderTarget(today, today, user?.userData?.elemId ?: return, user?.userData?.elemType ?: ""), flags)
    }

    inner class TimetableWidgetTimetableDisplay(
            private val appWidgetManager: AppWidgetManager,
            private val appWidgetId: Int,
            private val user: UserDatabase.User?
    ) : TimetableDisplay {

        override fun addTimetableItems(items: List<TimegridItem>, startDate: UntisDate, endDate: UntisDate, timestamp: Long) {
            val newViews = loadBaseLayout(user)
            val formattedItems = formatItems(items)
            val fmt: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm")
            var time: String
            var text = ""
            formattedItems.forEach {
                time = it.startDateTime.toString(fmt) + " - " + it.endDateTime.toString(fmt)
                text += "$time\n${it.title}"
                if (it.top != "") text += ", ${it.top}"
                if (it.bottom != "") text += ", ${it.bottom}"
                text += "\n\n"
            }
            newViews.setTextViewText(R.id.textview_base_widget_content, text)
            appWidgetManager.updateAppWidget(appWidgetId, newViews)
        }

        override fun onTimetableLoadingError(requestId: Int, code: Int?, message: String?) {
            Log.e(TimetableWidget::class.java.simpleName, message ?: "")
            val newViews = loadBaseLayout(user)
            newViews.setTextViewText(R.id.textview_base_widget_content, context.resources.getString(R.string.all_error))
            appWidgetManager.updateAppWidget(appWidgetId, newViews)
        }
    }
}