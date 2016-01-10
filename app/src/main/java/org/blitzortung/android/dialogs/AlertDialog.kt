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

package org.blitzortung.android.dialogs

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent

import org.blitzortung.android.app.AppService
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.AlertView
import org.blitzortung.android.map.overlay.color.ColorHandler

class AlertDialog(context: Context, private val service: AppService?, private val colorHandler: ColorHandler) : android.app.AlertDialog(context) {
    internal var intervalDuration: Int = 0
    private lateinit var alertView: AlertView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.alarm_dialog)
    }

    fun setIntervalDuration(intervalDuration: Int) {
        this.intervalDuration = intervalDuration
    }

    override fun onStart() {
        super.onStart()

        alertView = findViewById(R.id.alarm_diagram) as AlertView

        setTitle(context.getString(R.string.alarms))

        if (service != null) {
            alertView.setColorHandler(colorHandler, service.dataHandler().intervalDuration)
            alertView.alertEventConsumer.invoke(service.alertEvent())
            service.addAlertConsumer(alertView!!.alertEventConsumer)
        }
        colorHandler.updateTarget()
    }

    override fun onStop() {
        super.onStop()

        service?.removeAlertListener(alertView!!.alertEventConsumer)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
