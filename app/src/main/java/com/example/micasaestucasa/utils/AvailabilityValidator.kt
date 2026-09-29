package com.example.micasaestucasa.utils

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints

class AvailabilityValidator(private val availableRanges: List<Pair<Long, Long>>) : CalendarConstraints.DateValidator {

    override fun isValid(date: Long): Boolean {
        return availableRanges.any { (start, end) ->
            date in start..end
        }
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeList(availableRanges)
    }

    companion object CREATOR : Parcelable.Creator<AvailabilityValidator> {
        override fun createFromParcel(parcel: Parcel): AvailabilityValidator {
            val list = mutableListOf<Pair<Long, Long>>()
            parcel.readList(list, Pair::class.java.classLoader)
            return AvailabilityValidator(list)
        }
        override fun newArray(size: Int): Array<AvailabilityValidator?> = arrayOfNulls(size)
    }
}

