package com.xander.performance;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;

@Deprecated
class DumpTool {

  private static String TAG = PERF.TAG + "_DumpTool";

  private static LinkedHashSet<DumpSysListener> dumpListeners = new LinkedHashSet<>();

  public static void addDumpListener(DumpSysListener dumpListener) {
    dumpListeners.add(dumpListener);
  }

  public static void removeDumpListener(DumpSysListener dumpListener) {
    dumpListeners.remove(dumpListener);
  }

  static void resetTag(String tag) {
    TAG = tag + "_DumpTool";
  }

  static void init(String serviceName) {
    xLog.e(TAG, "init");
    try {
      addService(serviceName);
    } catch (Exception e) {
      xLog.w(TAG, "DumpTool init error:", e);
    }
  }

  private static void addService(String serviceName)
      throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
    Class<?> smClass = Class.forName("android.os.ServiceManager");
    Method addServiceMethod = smClass.getDeclaredMethod("addService", String.class, IBinder.class);
    addServiceMethod.setAccessible(true);
    addServiceMethod.invoke(null, serviceName, new DumpSysBinder());
  }


  static class DumpSysBinder extends Binder implements IInterface {

    @Override
    public void dump(FileDescriptor fd, String[] args) {
      super.dump(fd, args);
      xLog.e(TAG, "dump");
      dump(args);
    }

    @Override
    public void dumpAsync(FileDescriptor fd, String[] args) {
      super.dumpAsync(fd, args);
      xLog.e(TAG, "dumpAsync");
      dump(args);
    }

    private void dump(String[] args) {
      // 需要注意
      xLog.e(TAG, Arrays.toString(args));
      for (DumpSysListener listener : DumpTool.dumpListeners) {
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

  public interface DumpSysListener {

    boolean dump(String[] args);
  }

}
