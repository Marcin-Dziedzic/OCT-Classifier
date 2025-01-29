package com.example.projektprzejsciowy_v2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.projektprzejsciowy_v2.databinding.FragmentPhotoBinding


class PhotoFragment : Fragment() {
    private lateinit var selectedImageUri: Uri
    private lateinit var selectedBitmap: Bitmap
    private lateinit var scaledBitmap: Bitmap
    private lateinit var img: ImageView
    private lateinit var binding: FragmentPhotoBinding

    private val getImageContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = uri
                val inputStream = requireActivity().contentResolver.openInputStream(uri)
                selectedBitmap = BitmapFactory.decodeStream(inputStream)
                scaledBitmap = Bitmap.createScaledBitmap(selectedBitmap, 224, 224, true)
                img.setImageBitmap(selectedBitmap)
                binding.chooseclf.visibility = View.VISIBLE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_photo,container,false)
        binding.chooseclf.visibility = View.INVISIBLE
        binding.choose.setOnClickListener {
            if (VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
                val readPermission = checkSelfPermission(
                    requireActivity().application,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                val writePermission = checkSelfPermission(
                    requireActivity().application,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                if (readPermission == PackageManager.PERMISSION_DENIED ||
                    writePermission == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        PERMISSION_CODE
                    )
                } else {
                    getImageContent.launch("image/*")
                }
            }
            else if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU){
                if(checkSelfPermission(requireActivity().application,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        PERMISSION_CODE) }
                else{
                    getImageContent.launch("image/*")
                } }
            img = binding.chosenPhoto
        }
        binding.chooseclf.setOnClickListener { view : View ->
            view.findNavController().navigate(PhotoFragmentDirections
                .actionPhotoFragmentToClassifierFragment(selectedImageUri.toString(), 0))
        }
        return binding.root
    }

    companion object {
        private const val PERMISSION_CODE = 1001

    }
}