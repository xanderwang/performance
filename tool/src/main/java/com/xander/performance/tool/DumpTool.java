package com.xander.performance.tool;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.FileDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class DumpTool {

  private static final String TAG = pTool.TAG + "_DumpTool";

  private static LinkedHashSet<DumpListener> linkedHashSet = new LinkedHashSet<>();

  public static void addDumpListener(DumpListener dumpListener) {
    linkedHashSet.add(dumpListener);
  }

  public static void removeDumpListener(DumpListener dumpListener) {
    linkedHashSet.remove(dumpListener);
  }

  public static void init(String serviceName) {
    try {
      Class<?> smClass = Class.forName("android.os.ServiceManager");
      Method addServiceMethod = smClass.getMethod("addService", new Class[]{String.class, IBinder.class});
      if (addServiceMethod != null) {
        addServiceMethod.setAccessible(true);
        addServiceMethod.invoke((Object) null, serviceName, (new DumpBinder()));
      }
    } catch (Exception e) {
      xLog.e(TAG, "DumpTool init error:", e);
    }
  }

  public static class DumpBinder extends Binder implements IInterface {

    //@Override
    //protected void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter fout,
    //    @Nullable String[] args) {
    //  super.dump(fd, fout, args);
    //  xLog.e(TAG, "dump 3");
    //}

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
