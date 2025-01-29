package com.example.projektprzejsciowy_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.projektprzejsciowy_v2.databinding.FragmentTitleBinding


class TitleFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentTitleBinding>(inflater,
            R.layout.fragment_title,container,false)

        binding.chooseimg.setOnClickListener { view : View ->
            view.findNavController().navigate(R.id.action_titleFragment_to_photoFragment)
        }
        binding.clfimg.setOnClickListener { view : View ->
            view.findNavController().navigate(R.id.action_titleFragment_to_cameraFragment)
        }
        return binding.root
    }
}