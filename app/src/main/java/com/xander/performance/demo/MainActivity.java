package com.xander.performance.demo;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.xander.performance.tool.ThreadTool;
import com.xander.performance.tool.UiTool;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ThreadTool.init();
    UiTool.start();
  }

  public void testUiCheck(View v) {
    try {
      Thread.sleep(5000);
    } catch (Exception e) {

    }
  }
}
