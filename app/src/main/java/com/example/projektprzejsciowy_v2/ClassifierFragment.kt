package com.example.projektprzejsciowy_v2

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.projektprzejsciowy_v2.databinding.FragmentClassifierBinding
import com.example.projektprzejsciowy_v2.ml.JointAttentionMobilenetv2
import com.example.projektprzejsciowy_v2.ml.OpticNet
import com.example.projektprzejsciowy_v2.ml.VGG16
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class ClassifierFragment : Fragment() {
    private lateinit var myuri: Uri
    private var PrevFragment = 0
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingMessage: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentClassifierBinding>(inflater,
            R.layout.fragment_classifier,container,false)

        progressBar = binding.progressBar
        loadingMessage = binding.loadingMessage

        val args = ClassifierFragmentArgs.fromBundle(requireArguments())
        myuri = args.sharedUri.toUri()

        PrevFragment = args.prevFragment

        val inputStream = requireActivity().contentResolver.openInputStream(myuri)
        val bitmap  = BitmapFactory.decodeStream(inputStream)
        binding.imageToLabel.setImageBitmap(bitmap)

        binding.btnTest.setOnClickListener {
            binding.btnTest.visibility = View.GONE
            showLoadingIndicator()

            Handler(Looper.getMainLooper()).postDelayed({
            val model = JointAttentionMobilenetv2.newInstance(requireActivity().application)
            val model1 = VGG16.newInstance(requireActivity().application)
            val model2 = OpticNet.newInstance(requireActivity().application)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val inputFeature0_1 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val inputFeature0_2 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap!!, 224, 224, true)
            val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)

            inputFeature0.loadBuffer(byteBuffer)
            inputFeature0_1.loadBuffer(byteBuffer)
            inputFeature0_2.loadBuffer(byteBuffer)

            val startTime = SystemClock.elapsedRealtime()
            val outputs = model.process(inputFeature0)
            val outputFeature1 = outputs.outputFeature1AsTensorBuffer
            val resultTimeModel = SystemClock.elapsedRealtime() - startTime


            val startTime1 = SystemClock.elapsedRealtime()
            val outputs1 = model1.process(inputFeature0_1)
            val outputFeature0_1 = outputs1.outputFeature0AsTensorBuffer
            val resultTimeModel1 = SystemClock.elapsedRealtime() - startTime1


            val startTime2 = SystemClock.elapsedRealtime()
            val outputs2 = model2.process(inputFeature0_2)
            val outputFeature0_2 = outputs2.outputFeature0AsTensorBuffer
            val resultTimeModel2 = SystemClock.elapsedRealtime() - startTime2

            val labelList = listOf("CNV", "DME", "DRUSEN", "NORMAL", "VMT")

            val maxIndex0 = outputFeature1.floatArray.indices.maxByOrNull { outputFeature1.floatArray[it] } ?: -1
            val result0 = "Joint Attention Network + MobileNetV2: \nMeasurement of image classification time: $resultTimeModel ms \n" +
                    highlightMaxClass(labelList, outputFeature1.floatArray, maxIndex0)

            val maxIndex1 = outputFeature0_1.floatArray.indices.maxByOrNull { outputFeature0_1.floatArray[it] } ?: -1
            val result1 = "VGG16: \nMeasurement of image classification time: $resultTimeModel1 ms \n" +
                    highlightMaxClass(labelList, outputFeature0_1.floatArray, maxIndex1)

            val maxIndex2 = outputFeature0_2.floatArray.indices.maxByOrNull { outputFeature0_2.floatArray[it] } ?: -1
            val result2 = "OpticNet-71: \nMeasurement of image classification time: $resultTimeModel2 ms \n" +
                    highlightMaxClass(labelList, outputFeature0_2.floatArray, maxIndex2)

            val formattedText = "$result0\n\n$result1\n\n$result2".replace("\n", "<br>")
            binding.textResult.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
            saveBitmapToGallery(scaledBitmap)

            model.close()
            model1.close()
            model2.close()

            hideLoadingIndicator()
            binding.btnTest.visibility = View.VISIBLE
        }, 0)
        }

        return binding.root
    }

    private fun showLoadingIndicator() {
        progressBar.visibility = View.VISIBLE
        loadingMessage.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        progressBar.visibility = View.GONE
        loadingMessage.visibility = View.GONE
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val imageFileName = "classified_image_${System.currentTimeMillis()}.jpg"

        val resolver = requireActivity().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                Toast.makeText(requireContext(), "Bitmap saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}

private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
    byteBuffer.order(ByteOrder.nativeOrder())
    val pixels = IntArray(224 * 224)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    var pixel = 0
    for (i in 0 until 224) {
        for (j in 0 until 224) {
            val pixelVal = pixels[pixel++]
            byteBuffer.putFloat(((pixelVal shr 16) and 0xFF)/ 255.0f)
            byteBuffer.putFloat(((pixelVal shr 8) and 0xFF)/ 255.0f)
            byteBuffer.putFloat((pixelVal and 0xFF)/ 255.0f)
        }
    }
    return byteBuffer
}
private fun highlightMaxClass(labelList: List<String>, probabilities: FloatArray, maxIndex: Int): String {
    val resultStringBuilder = StringBuilder()
    for (i in probabilities.indices) {
        val label = labelList[i]
        val probability = probabilities[i]
        val p = "%.4f".format(probability)
        val formattedResult = if (i == maxIndex) {
            "<b>$label : $p</b><br>"
        } else {
            "$label : $p<br>"
        }
        resultStringBuilder.append(formattedResult)
    }
    return resultStringBuilder.toString()
}
