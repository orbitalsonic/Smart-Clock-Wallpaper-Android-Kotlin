@file:Suppress("MayBeConstant")

package com.orbitalsonic.smartclockwallpaper

import android.annotation.SuppressLint
import android.app.WallpaperColors
import android.content.SharedPreferences
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.*

class AnalogWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AnalogWallpaperEngine()
    }

    private inner class AnalogWallpaperEngine : Engine(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        //Internal Values
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { draw() }

        var startDegSec = 0.0F
        var startDegMint = 0.0F
        var startDegHr = 0.0F

        var paintTime: Paint? = null
        var paintDate: Paint? = null
        var paintDay: Paint? = null

        var mBackColor: Int = Color.parseColor("#222222")

        private lateinit var mBitmapSec: Bitmap
        private lateinit var mBitmapMint: Bitmap
        private lateinit var mBitmapHr: Bitmap
        var canvas: Canvas? = null

        var drawOk: Boolean = false

        //Called if Engine Created
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)

            //Disables not used notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setOffsetNotificationsEnabled(false)
            }
            setTouchEventsEnabled(false)

            updateClockFace()

            //Start Drawing
            handler.post(drawRunner)
        }

        //Called if Engine Destroyed
        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunner)
        }

        //Called if Surface Destroyed
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)

            //Sets Visibility
            setVisibility(false)
        }

        //Called if Visibility Changed
        override fun onVisibilityChanged(vis: Boolean) {
            super.onVisibilityChanged(vis)

            //Set Visibility
            setVisibility(vis)
        }

        //Called if Surface Changed
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            handler.removeCallbacks(drawRunner)
            if (isVisible) {
                handler.post(drawRunner)
            }
        }

        //Called if Wallpaper Colors Wanted
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors {
            return WallpaperColors(
                Color.valueOf(mBackColor),
                Color.valueOf(mBackColor),
                Color.valueOf(mBackColor)
            )
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)

        }

        //Sets Visibility and Callback
        private fun setVisibility(vis: Boolean) {
            handler.removeCallbacks(drawRunner)
            if (vis) {
                getInitialAllAngle()
                drawOk = true
                handler.post(drawRunner)
            } else {
                drawOk = false
            }
        }

        //update clock
        private fun updateClockFace() {

            getInitialAllAngle()
            getPainClock()
        }

        //Draws the Wallpaper
        private fun draw() {
            val holder = surfaceHolder

            if (drawOk) {
                try {
                    canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        holder.lockHardwareCanvas()
                    } else {
                        holder.lockCanvas()
                    }
                } catch (e: Exception) {
                }


                if (canvas != null) {

                    try {
                        canvas?.drawColor(mBackColor, PorterDuff.Mode.SRC)
                        val mBitmap = BitmapFactory.decodeResource(
                            resources,
                            R.drawable.clock_bg
                        )
                        val bitmap = Bitmap.createScaledBitmap(
                            mBitmap,
                            getPercentageWidth(90, canvas?.width!!),
                            getPercentageWidth(90, canvas?.width!!),
                            true
                        )

                        mBitmapSec = BitmapFactory.decodeResource(
                            resources,
                            R.drawable.clock_sec
                        )
                        secNeedleControl()

                        mBitmapMint =
                            BitmapFactory.decodeResource(
                                resources,
                                R.drawable.clock_min
                            )
                        mintNeedleControl()

                        mBitmapHr = BitmapFactory.decodeResource(
                            resources,
                            R.drawable.clock_hour
                        )
                        hrNeedleControl()

                        canvas?.drawBitmap(
                            bitmap,
                            ((canvas?.width!! - bitmap.width) / 2).toFloat(),
                            ((canvas?.height!! - bitmap.height) / 2).toFloat(),
                            null
                        )

                        getTextClock()
                    }catch (e:Exception){

                    }

                }
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                        repeatMethod()

                    } catch (e: Exception) {
                    }
                }
            }


        }

        private fun getPercentageWidth(percentage: Int, mWidth: Int): Int {
            return ((percentage * mWidth) / 100)

        }

        private fun repeatMethod() {
//            Log.i("Information", "repeatMethod 1")

            handler.removeCallbacks(drawRunner)
            handler.postDelayed(
                drawRunner,
                (1000)
            )

//            Handler(Looper.getMainLooper()).postDelayed({
//                Log.i("Information", "repeatMethod")
//                    draw()
//
//            }, 1000)

        }

        private fun secNeedleControl() {

            val width: Int = mBitmapSec.width
            val height: Int = mBitmapSec.height
            val newWidth = getPercentageWidth(90, canvas?.width!!)
            val newHeight = getPercentageWidth(90, canvas?.width!!)

            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height

            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            matrix.postRotate(startDegSec)
            mBitmapSec = Bitmap.createBitmap(
                mBitmapSec, 0, 0,
                width, height, matrix, true
            )

            if (startDegSec >= 360) {
                startDegSec = 6.0F
                if (startDegMint >= 360) {
                    startDegMint = 6.0F
                    if (startDegHr >= 360) {
                        startDegHr = 0.5F
                    } else {
                        startDegHr += 0.5F
                    }
                } else {
                    startDegMint += 6.0F
                    if (startDegMint % 6 == 0.0F) {
                        if (startDegHr >= 360) {
                            startDegHr = 0.5F
                        } else {
                            startDegHr += 0.5F
                        }
                    }
                }
            } else {
                startDegSec += 6.0F
            }

            canvas?.drawBitmap(
                mBitmapSec,
                ((canvas?.width!! - mBitmapSec.width) / 2).toFloat(),
                ((canvas?.height!! - mBitmapSec.height) / 2).toFloat(),
                null
            )



        }

        private fun mintNeedleControl() {

            val width: Int = mBitmapMint.width
            val height: Int = mBitmapMint.height
            val newWidth = getPercentageWidth(90, canvas?.width!!)
            val newHeight = getPercentageWidth(90, canvas?.width!!)

            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height

            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            matrix.postRotate(startDegMint)
            mBitmapMint = Bitmap.createBitmap(
                mBitmapMint, 0, 0,
                width, height, matrix, true
            )

            canvas?.drawBitmap(
                mBitmapMint,
                ((canvas?.width!! - mBitmapMint.width) / 2).toFloat(),
                ((canvas?.height!! - mBitmapMint.height) / 2).toFloat(),
                null
            )

        }

        private fun hrNeedleControl() {

            val width: Int = mBitmapHr.width
            val height: Int = mBitmapHr.height
            val newWidth = getPercentageWidth(90, canvas?.width!!)
            val newHeight = getPercentageWidth(90, canvas?.width!!)

            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height

            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            matrix.postRotate(startDegHr)
            mBitmapHr = Bitmap.createBitmap(
                mBitmapHr, 0, 0,
                width, height, matrix, true
            )

            canvas?.drawBitmap(
                mBitmapHr,
                ((canvas?.width!! - mBitmapHr.width) / 2).toFloat(),
                ((canvas?.height!! - mBitmapHr.height) / 2).toFloat(),
                null
            )

        }

        private fun initialSecondAngle(sec: Int): Float {
            return sec * 6F
        }

        private fun initialMinuteAngle(mint: Int): Float {
            return mint * 6F
        }

        private fun initialHourAngle(hour: Int, mint: Int): Float {
            return ((hour * 30) + (mint * 0.5)).toFloat()
        }

        private fun getInitialAllAngle() {
            val calender: Calendar = Calendar.getInstance()
            val mSecond: Int = calender.get(Calendar.SECOND)
            val mMinute: Int = calender.get(Calendar.MINUTE)
            val mHour: Int = calender.get(Calendar.HOUR)

            startDegSec = initialSecondAngle(mSecond)
            startDegMint = initialMinuteAngle(mMinute)
            startDegHr = initialHourAngle(mHour, mMinute)
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
        }


        @SuppressLint("SimpleDateFormat")
        fun getCurrentDay(): String {
            // Thursday
            val dateFormat = SimpleDateFormat("EEE")
            val today: Date = Calendar.getInstance().time
            return dateFormat.format(today)

//            return "Wednesday"
        }

        @SuppressLint("SimpleDateFormat")
        fun getCurrentDate(): String {
            // 03.Dec
            val dateFormat = SimpleDateFormat("dd MMM")
            val today: Date = Calendar.getInstance().time
            return dateFormat.format(today)
        }


        @SuppressLint("SimpleDateFormat")
        fun getCurrentTime(): String {
            val dateFormat = SimpleDateFormat("hh:mm a")
            val today: Date = Calendar.getInstance().time
            return dateFormat.format(today)
        }

        fun getTextClock() {
            val mDate = getCurrentDate()
            val mTime = getCurrentTime()
            val mDay = getCurrentDay()

            val rDate = Rect()
            paintDate?.getTextBounds(mDate, 0, mDate.length, rDate)
            val rTime = Rect()
            paintTime?.getTextBounds(mTime, 0, mTime.length, rTime)
            val rDay = Rect()
            paintDay?.getTextBounds(mDay, 0, mDay.length, rDay)

            val yPosTime = canvas?.height!! / 2 + abs(rTime.height())/2
            val xPosTime = canvas?.width!! / 2 - canvas?.width!! / 4 - abs(rTime.height())

            val yPosDate =canvas?.height!! / 2 - canvas?.width!! / 7 - abs(rDate.height())/2
            val xPosDate = canvas?.width!! / 2  - canvas?.width!! / 4 + abs(rDate.height())/2

            val yPosDay = canvas?.height!! / 2 + canvas?.width!! / 4 - abs(rDay.height())
            val xPosDay = canvas?.width!! / 2 - canvas?.width!! / 6

            canvas!!.drawText(mDate,xPosDate.toFloat(),yPosDate.toFloat(),paintDate!!)
            canvas!!.drawText(mTime, xPosTime.toFloat(), yPosTime.toFloat(), paintTime!!)
            canvas!!.drawText(mDay, xPosDay.toFloat(), yPosDay.toFloat(), paintDay!!)

        }

        fun getPainClock() {

            val clockFont = Typeface.createFromAsset(assets, "fonts/nexa_light.otf")
            val clockTime = Typeface.createFromAsset(assets, "fonts/nexa_bold.otf")

            paintTime = Paint()
            paintTime?.typeface = Typeface.create(clockTime, Typeface.BOLD)
            paintTime?.textSize = resources.getDimensionPixelSize(R.dimen._18ssp).toFloat()
            paintTime?.textAlign = Paint.Align.LEFT
            paintTime?.color = Color.parseColor("#ffffff")

            paintDate = Paint()
            paintDate?.typeface = Typeface.create(clockFont, Typeface.BOLD)
            paintDate?.textSize = resources.getDimensionPixelSize(R.dimen._15ssp).toFloat()
            paintDate?.textAlign = Paint.Align.LEFT
            paintDate?.color =  Color.parseColor("#ffffff")

            paintDay = Paint()
            paintDay?.typeface = Typeface.create(clockFont, Typeface.BOLD)
            paintDay?.textSize = resources.getDimensionPixelSize(R.dimen._15ssp).toFloat()
            paintDay?.textAlign = Paint.Align.LEFT
            paintDay?.color =  Color.parseColor("#ffffff")

        }

    }


}