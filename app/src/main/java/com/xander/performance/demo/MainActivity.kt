package com.xander.performance.demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

  }

  fun testUiCheck(v: View?) {
    try {
      Thread.sleep(15000)
    } catch (e: Exception) {
    }
  }


}