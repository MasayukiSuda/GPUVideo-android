package com.daasuu.gpuv.camerarecorder

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range

class CameraThreadExtension  {
    companion object {

        @JvmStatic
        fun getRange(cManager: CameraManager, cameraID: String): Range<Int>? {
            var chars: CameraCharacteristics? = null
            try {
                chars = cManager.getCameraCharacteristics(cameraID)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

            val ranges: Array<Range<Int>>? = chars!![CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
            var result: Range<Int>? = null
            for (range in ranges!!) {
                val upper: Int = range?.upper!!

                if (upper >= 20) {
                    if (result == null || upper < result.upper) {
                        result = range
                    }
                }
            }
            return result
        }
    }
}