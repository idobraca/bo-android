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

package org.blitzortung.android.data.beans

import java.io.Serializable

data class RasterElement(
        override val timestamp: Long,
        override val longitude: Float,
        override val latitude: Float,
        override val multiplicity: Int) : Strike, Serializable {

    companion object {
        private val serialVersionUID = 6765788323616893614L
    }
}
