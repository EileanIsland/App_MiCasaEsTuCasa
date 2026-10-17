package com.example.micasaestucasa.utils

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker

class AvailabilityValidator(private val availableRanges: List<Pair<Long, Long>>) : CalendarConstraints.DateValidator {

    override fun isValid(date: Long): Boolean {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        if (date < today) {
            return false
        }

        return availableRanges.any { (start, end) ->
            date in start..end
        }
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(availableRanges.size)
        for (range in availableRanges) {
            dest.writeLong(range.first)
            dest.writeLong(range.second)
        }
    }

    companion object CREATOR : Parcelable.Creator<AvailabilityValidator> {
        override fun createFromParcel(parcel: Parcel): AvailabilityValidator {
            val size = parcel.readInt()
            val list = List(size) { Pair(parcel.readLong(), parcel.readLong()) }
            return AvailabilityValidator(list)
        }
        override fun newArray(size: Int): Array<AvailabilityValidator?> = arrayOfNulls(size)
    }
}

