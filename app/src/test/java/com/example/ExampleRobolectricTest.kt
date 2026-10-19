package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CalibrationConfig
import com.example.data.model.OperatingMode
import com.example.data.model.WeedClass
import com.example.vision.YoloVisionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OnionBot", appName)
    }

    @Test
    fun testGroundCoordinateProjection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = YoloVisionEngine(context)
        val calib = CalibrationConfig(
            cameraHeightCm = 25f,
            cameraTiltAngleDeg = 45f,
            cameraFocalLengthPx = 380f
        )

        // Optical center pixel (320, 240) in 640x480 frame
        val (gx, gy) = engine.projectPixelToGround(
            pxX = 320f,
            pxY = 240f,
            imgWidth = 640,
            imgHeight = 480,
            calibration = calib
        )

        // At optical center with 45 degree tilt, ground distance forward should equal height (approx 25 cm)
        assertEquals(0f, gx, 0.5f)
        assertEquals(25f, gy, 1.0f)
    }

    @Test
    fun testSafeWorkspaceBounds() {
        val calib = CalibrationConfig()
        // Outside X limit
        val xOutside = 20.0f
        val isInside = xOutside >= calib.workspace.xMinCm && xOutside <= calib.workspace.xMaxCm
        assertFalse("X=20cm should be outside safe workspace (-15..+15)", isInside)

        // Inside X and Y limits
        val targetX = 5.0f
        val targetY = 22.0f
        val valid = targetX >= calib.workspace.xMinCm && targetX <= calib.workspace.xMaxCm &&
                targetY >= calib.workspace.yMinCm && targetY <= calib.workspace.yMaxCm
        assertTrue("Target (5, 22) should be inside safe workspace", valid)
    }
}
