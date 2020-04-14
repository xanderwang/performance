package com.xander.performance.demo;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void testUiCheck(View v) {
    try {
      Thread.sleep(15000);
    } catch (Exception e) {

    }
  }
}
