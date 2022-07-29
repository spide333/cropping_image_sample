package com.example.croptestapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.croptestapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMainBinding?>(this, R.layout.activity_main)
            .apply {
                startButton.setOnClickListener { start() }
                firstButton.setOnClickListener { createBox() }
                secondButton.setOnClickListener { crop() }
                lifecycleOwner = this@MainActivity
            }
    }

    private fun start() {
        val intent = Intent()
            .apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
            }

        startActivityForResult(intent, 1234)
    }

    private fun createBox() {
        binding.imageView.createBox()
    }

    private fun crop() {
        binding.imageView.crop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1234) {
            onReceivedUri(data?.data)
        }
    }

    private fun onReceivedUri(uri: Uri?) {
        uri ?: return

        try {
            bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            binding.imageView.sourceBitmap = bitmap
        } catch (e: Exception) {

        }
    }
}