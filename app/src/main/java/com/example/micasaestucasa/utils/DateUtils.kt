package com.example.micasaestucasa.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    @SuppressLint("ConstantLocale")
    private val formatter = SimpleDateFormat(
        "dd/MM/yyyy", Locale.getDefault()
    )

    fun formatDate(time: Long ): String{
        return formatter.format(Date(time))

    }

    fun formatRange(
        start: Long,
        end: Long
    ): String{
        return "${formatDate(start)} → ${formatDate(end)}"
    }


    fun String.toLongDate(): Long {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.parse(this)?.time ?: 0L
    }
}




