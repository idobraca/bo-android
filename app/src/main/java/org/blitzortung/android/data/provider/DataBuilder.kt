/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.data.provider

import org.blitzortung.android.data.beans.*
import org.blitzortung.android.util.TimeFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class DataBuilder {

    fun createDefaultStrike(referenceTimestamp: Long, jsonArray: JSONArray): Strike {
        try {
            return DefaultStrike(
                    timestamp = referenceTimestamp - 1000 * jsonArray.getInt(0),
                    longitude = jsonArray.getDouble(1).toFloat(),
                    latitude = jsonArray.getDouble(2).toFloat(),
                    lateralError = jsonArray.getDouble(3).toFloat(),
                    altitude = 0,
                    amplitude = jsonArray.getDouble(4).toFloat(),
                    stationCount = jsonArray.getInt(5).toShort())
        } catch (e: JSONException) {
            throw IllegalStateException("error with JSON format while parsing strike data", e)
        }

    }

    @Throws(JSONException::class)
    fun createRasterParameters(response: JSONObject, info: String): RasterParameters {
        return RasterParameters(
                longitudeStart = response.getDouble("x0").toFloat(),
                latitudeStart = response.getDouble("y1").toFloat(),
                longitudeDelta = response.getDouble("xd").toFloat(),
                latitudeDelta = response.getDouble("yd").toFloat(),
                longitudeBins = response.getInt("xc"),
                latitudeBins = response.getInt("yc"),
                info = info)
    }

    @Throws(JSONException::class)
    fun createRasterElement(rasterParameters: RasterParameters, referenceTimestamp: Long, jsonArray: JSONArray): RasterElement {
        return RasterElement(
                timestamp = referenceTimestamp + 1000 * jsonArray.getInt(3),
                longitude = rasterParameters.getCenterLongitude(jsonArray.getInt(0)),
                latitude = rasterParameters.getCenterLatitude(jsonArray.getInt(1)),
                multiplicity = jsonArray.getInt(2)
        )
    }

    fun createStation(jsonArray: JSONArray): Station {
        var name = "n/a"
        var longitude = 0f
        var latitude = 0f
        var offlineSince = Station.OFFLINE_SINCE_NOT_SET
        try {
            name = jsonArray.getString(1)
            longitude = jsonArray.getDouble(3).toFloat()
            latitude = jsonArray.getDouble(4).toFloat()
            if (jsonArray.length() >= 6) {

                val offlineSinceString = jsonArray.getString(5)
                if (offlineSinceString.length > 0) {
                    offlineSince = TimeFormat.parseTimeWithMilliseconds(offlineSinceString)
                }
            }
        } catch (e: JSONException) {
            throw IllegalStateException("error with JSON format while parsing participants data")
        }

        return Station(name = name, longitude = longitude, latitude = latitude, offlineSince = offlineSince)
    }
}
