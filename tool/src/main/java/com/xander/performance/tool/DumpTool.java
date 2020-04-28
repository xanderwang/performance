package com.xander.performance.tool;

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Iterator;
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
      Method addServiceMethod = smClass
          .getMethod("addService", new Class[]{String.class, IBinder.class});
      if (addServiceMethod != null) {
        addServiceMethod.invoke((Object) null, serviceName, (new DumpBinder()));
      }
    } catch (Exception e) {
      Log.e(TAG, "DumpTool init error:", e);
    }
  }


  public static class DumpBinder extends Binder {

    @Override
    protected void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter fout,
        @Nullable String[] args) {
      super.dump(fd, fout, args);
      dump(args);
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) {
      super.dump(fd, args);
      dump(args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) {
      super.dumpAsync(fd, args);
      dump(args);
    }

    private void dump(String[] args) {
      // 需要注意
      Iterator<DumpListener> iterator = DumpTool.linkedHashSet.iterator();
      while (iterator.hasNext()) {
        DumpListener listener = iterator.next();
        if (listener.dump(args)) {
          break;
        }
      }
    }
  }

  public static interface DumpListener {

    boolean dump(String[] args);
  }

}
