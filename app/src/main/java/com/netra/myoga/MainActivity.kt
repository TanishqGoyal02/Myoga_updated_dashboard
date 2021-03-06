/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package com.netra.myoga

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.util.Size
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.netra.myoga.R
import org.tensorflow.lite.examples.poseestimation.ImageUtils
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils.drawBodyKeypoints
import org.tensorflow.lite.examples.poseestimation.ml.ModelType
import org.tensorflow.lite.examples.poseestimation.ml.MoveNet
import org.tensorflow.lite.examples.poseestimation.ml.PoseDetector
import org.tensorflow.lite.examples.poseestimation.ml.PoseNet
import org.tensorflow.lite.examples.poseestimation.data.Device
import android.app.Activity
import android.view.*
import androidx.core.graphics.withRotation

import android.graphics.Point
import android.net.Uri
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils.getTotalPoseError
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {
    companion object {
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480
        private const val FRAGMENT_DIALOG = "dialog"
        private const val TAG = "PoseEstimation"
    }





 /*   val display = windowManager.defaultDisplay

    // declare and initialize a point
    val size = Point()

    // store the points related details from the
    // display variable in the size variable
    display.getSize(size)

    // store the point information in integer
    // variables width and height
    // where .x extracts width pixels and
    // .y extracts height pixels
    val width = size.x
    val height = size.y*/

    var imageView: ImageView? = null
    var stopsign:ImageView? = null
    var videoView : VideoView? = null
    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    /** Abstract interface to someone holding a display surface.    */
    private lateinit var surfaceHolder: SurfaceHolder

    /** A [Handler] for running tasks in the background.    */
    private var backgroundHandler: Handler? = null

    /** The [android.util.Size] of camera preview.  */
    private var previewSize: Size? = null

    /** An additional thread for running tasks that shouldn't block the UI.   */
    private var backgroundThread: HandlerThread? = null

    /** ID of the current [CameraDevice].   */
    private var cameraId: String = ""

    /** The [android.util.Size.getWidth] of camera preview. */
    private var previewWidth = 0

    /** The [android.util.Size.getHeight] of camera preview.  */
    private var previewHeight = 0

    /** A reference to the opened [CameraDevice].    */
    private var cameraDevice: CameraDevice? = null

    /** A [CameraCaptureSession] for camera preview.   */
    private var captureSession: CameraCaptureSession? = null

    /** An object for the Posenet library.    */
    private var poseDetector: PoseDetector? = null

    /** Default device is GPU */
    private var device = Device.CPU

    /** Default 0 == Movenet Lightning model */
    private var modelPos = 2

    /** A shape for extracting frame data.   */
    private var imageReader: ImageReader? = null

    /** Threshold for confidence score. */
    private val minConfidence = .2f

    /** [CaptureRequest.Builder] for the camera preview   */
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    /** [CaptureRequest] generated by [.previewRequestBuilder   */
    private var previewRequest: CaptureRequest? = null

    private lateinit var tvScore: TextView
    private lateinit var tvTime: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnModel: Spinner

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            this@MainActivity.cameraDevice = camera

            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(camera)
        }
    }



    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }
    private var imageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(imageReader: ImageReader) {
            // We need wait until we have some size from onPreviewSizeChosen
            if (previewWidth == 0 || previewHeight == 0) {
                return
            }

            val image = imageReader.acquireLatestImage() ?: return
            val nv21Buffer =
                ImageUtils.yuv420ThreePlanesToNV21(image.planes, previewWidth, previewHeight)
            val imageBitmap = ImageUtils.getBitmap(nv21Buffer!!, previewWidth, previewHeight)

            // Create rotated version for portrait display
            val rotateMatrix = Matrix()
            rotateMatrix.postRotate(0.0f)

            val rotatedBitmap = Bitmap.createBitmap(
                imageBitmap!!, 0, 0, previewWidth, previewHeight,
                rotateMatrix, true
            )
            image.close()

            processImage(rotatedBitmap)
        }
    }

    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            changeModel(position)
        }
    }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        setContentView(R.layout.new_activity_main)

        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        createPoseEstimator()

        tvScore = findViewById(R.id.tvScore)
        tvTime = findViewById(R.id.tvTime)
        spnModel = findViewById(R.id.spnModel)
        spnDevice = findViewById(R.id.spnDevice)
        surfaceView = findViewById(R.id.surfaceView)
        imageView = findViewById<View>(R.id.imageView16) as ImageView?
//        stopsign = findViewById<View>(R.id.stopsign) as ImageView?

        surfaceHolder = surfaceView.holder
        initSpinner()
        requestPermission()

        videoView = findViewById<View>(R.id.videoView2) as VideoView?
        videoView!!.setVideoURI(Uri.parse("android.resource://"+packageName+"/"+R.raw.perfectplank))
        videoView!!.requestFocus()
       videoView!!.start()
    }


    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        poseDetector?.close()
    }

    // change model when app is running
    private fun changeModel(position: Int) {
        modelPos = position
        createPoseEstimator()
    }

    // change device type when app is running
    private fun changeDevice(position: Int) {
        device = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        createPoseEstimator()
    }

    private fun createPoseEstimator() {
        closeCamera()
        stopBackgroundThread()
        poseDetector?.close()
        poseDetector = null
        poseDetector = when (modelPos) {
            0 -> MoveNet.create(this, device)
            1 -> MoveNet.create(this, device, ModelType.Thunder)
            else -> PoseNet.create(this, device)
        }
        openCamera()
        startBackgroundThread()
    }

    // Init spinner that user can choose model and device they want.
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spnModel.adapter = adapter
            spnModel.onItemSelectedListener = changeModelListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adaper ->
            adaper.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adaper
            spnDevice.onItemSelectedListener = changeDeviceListener
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun openCamera() {
        // check if permission is granted or not.
        if (checkPermission(
                Manifest.permission.CAMERA,
                Process.myPid(),
                Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setUpCameraOutputs()
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }

    /**
     * Sets up member variables related to camera.
     */
    private fun setUpCameraOutputs() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId);
                var orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                orientation = 180;


                // We don't use a front facing camera in this sample.
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (cameraDirection != null &&
                    cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    //   continue
                    //}

                    previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)

                    imageReader = ImageReader.newInstance(
                        PREVIEW_WIDTH, PREVIEW_HEIGHT,
                        ImageFormat.YUV_420_888, /*maxImages*/ 2
                    )

                    previewHeight = previewSize!!.height
                    previewWidth = previewSize!!.width

                    this.cameraId = cameraId
                    return
                }
                // We've found a viable camera and finished setting up member variables,
                // so we don't need to iterate through other available cameras.
                //return
            }
        } catch (e: CameraAccessException) {
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("imageAvailableListener").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // do nothing
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        try {
            // We capture images from preview in YUV format.
            imageReader = ImageReader.newInstance(
                previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2
            )
            imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

            // This is the surface we need to record images for processing.
            val recordingSurface = imageReader!!.surface

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder!!.addTarget(recordingSurface)

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice!!.createCaptureSession(
                listOf(recordingSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (cameraDevice == null) return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder!!.build()
                            captureSession!!.setRepeatingRequest(
                                previewRequest!!,
                                null, null
                            )
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, e.toString())
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(this@MainActivity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error creating camera preview session.", e)
        }
    }

    /** Process image using Movenet library.
     *
     */
    private fun processImage(bitmap: Bitmap) {
        var score = 0f
        var outputBitmap = bitmap

        // run detect pose
        // draw points and lines on original image
        poseDetector?.estimateSinglePose(bitmap)?.let { person ->
            score = person.score
            if (score > minConfidence) {
                outputBitmap = drawBodyKeypoints(bitmap, person)

//                if(score > 0.4) {
//                   stopsign!!.setImageDrawable(null)
//                }
                var poseError = getTotalPoseError()
                if (poseError < 10) {
                    imageView!!.setBackgroundColor(Color.rgb(0, 100, 0))
                } else {

                    imageView!!.setBackgroundColor(Color.rgb(100, 0, 0))
                }

            }
        }

        // Draw `bitmap` and `person`
        val canvas: Canvas = surfaceHolder.lockCanvas()

        val screenWidth: Int
        val screenHeight: Int
        val left: Int
        val top: Int

        if (canvas.height > canvas.width) {
            val ratio = outputBitmap.height.toFloat() / outputBitmap.width
            screenWidth = canvas.width
            left = 0
            screenHeight = canvas.height
//            screenHeight = (canvas.width * ratio).toInt()
//            top = (canvas.height - screenHeight) / 2
            top = 0
        } else {
            val ratio = outputBitmap.width.toFloat() / outputBitmap.height
            screenHeight = canvas.height
            top = 0
            screenWidth = (canvas.height * ratio).toInt()
            left = (canvas.width - screenWidth) / 2
        }
        val right: Int = left + screenWidth
        val bottom: Int = top + screenHeight
        canvas.drawBitmap(
            outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
            Rect(left, top, right, bottom), Paint()
        )
        surfaceHolder.unlockCanvasAndPost(canvas)
        tvScore.text = getString(R.string.tfe_pe_tv_score).format(score)
        poseDetector?.lastInferenceTimeNanos()?.let {
            tvTime.text =
                getString(R.string.tfe_pe_tv_time).format(it * 1.0f / 1_000_000)
        }
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this@MainActivity, newDashboard_tani::class.java)
        startActivity(intent)
    }


}
