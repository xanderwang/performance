package com.xander.performance;

import android.annotation.SuppressLint;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.IInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class DumpTool {

  private static String TAG = pTool.TAG + "_DumpTool";

  private static LinkedHashSet<DumpListener> linkedHashSet = new LinkedHashSet<>();

  public static void addDumpListener(DumpListener dumpListener) {
    linkedHashSet.add(dumpListener);
  }

  public static void removeDumpListener(DumpListener dumpListener) {
    linkedHashSet.remove(dumpListener);
  }

  static void resetTag(String tag) {
    TAG = tag + "_DumpTool";
  }

  public static void init(String serviceName) {
    xLog.e(TAG, "init");
    try {
      if (VERSION.SDK_INT < 28) {
        addService(serviceName);
      }
    } catch (Exception e) {
      xLog.e(TAG, "=====================================================================");
      xLog.w(TAG, "DumpTool init error:", e);
    }
  }

  private static void addService(String serviceName)
      throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
    @SuppressLint("PrivateApi") Class<?> smClass = Class.forName("android.os.ServiceManager");
    Method addServiceMethod = smClass.getMethod("addService", String.class, IBinder.class);
    addServiceMethod.setAccessible(true);
    addServiceMethod.invoke((Object) null, serviceName, (new DumpBinder()));
  }


  public static class DumpBinder extends Binder implements IInterface {

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) {
      super.dump(fd, args);
      xLog.e(TAG, "dump");
      dump(args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) {
      super.dumpAsync(fd, args);
      xLog.e(TAG, "dumpAsync");
      dump(args);
    }

    private void dump(String[] args) {
      // 需要注意
      xLog.e(TAG, Arrays.toString(args));
      for (DumpListener listener : DumpTool.linkedHashSet) {
        if (listener.dump(args)) {
          break;
        }
      }
    }

    @Override
    public IBinder asBinder() {
      return this;
    }
  }

  public interface DumpListener {

    boolean dump(String[] args);
  }

}
