package io.github.xanderwang.performance;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.List;

import io.github.xanderwang.asu.aLog;
import io.github.xanderwang.hook.HookBridge;
import io.github.xanderwang.hook.core.MethodHook;
import io.github.xanderwang.hook.core.MethodParam;

/**
 * 监听图片的内存分配，大于某个值的时候 log 提示
 * <p/>
 * Glide
 * Glide 加载图片的时候，最终会调用 ImageView.setImageBitmap 或者 ImageView.setImageDrawable 方法给 ImageView 设置图片。
 * 所以处理方法如下：
 * 1. 先记录 into 方法的调用栈 A ，记下 ImageView 的 hashCode 和调用栈 A 之间的关联
 * 2. 调用 ImageView.setImageBitmap 或者 ImageView.setImageDrawable 方法的时候，
 * 判断 Bitmap 的大小是否超过阈值，超过的话需要展示异常，如果没有超过的话，忽略。
 * 3. 有异常的时候，可以根据ImageView.setImageBitmap 或者 ImageView.setImageDrawable 的方法调用链里面是否包含
 *    'EngineJob$CallResourceReady.run' 方法来判断是手动设置还是框架回调，如果是框架回调，就需要找到之前的 into
 *    方法调用栈 A，如果不是框架回调，直接显示方法调用链。
 * <p/>
 * Fresco
 * 待完成
 *
 * @author xander
 */
public class BitmapTool {

  private static final String TAG = "BitmapTool";

  private static HashMap<String, Issue> imageViewLoadMap = new HashMap<>(64);

  private static class BitmapIssue extends Issue {

    public BitmapIssue(Object data) {
      super(Issue.TYPE_BITMAP, "LARGE BITMAP", data);
    }
  }

  private static void linkLoadImageAndImageView(String imageViewKey,
      StackTraceElement[] loadTrace) {
    Issue issue = new BitmapIssue(StackTraceUtils.list(loadTrace));
    imageViewLoadMap.put(imageViewKey, issue);

  }

  private static void unlinkLoadImageAndImageView(String imageViewKey) {
    imageViewLoadMap.remove(imageViewKey);
  }

  static void start() {
    aLog.e(TAG, "start");
    hookBitmap();
    hookGlideInto();
  }

  private static void hookBitmap() {
    aLog.e(TAG, "hookBitmap start");
    HookBridge.findAndHookMethod(
        ImageView.class,
        "setImageBitmap",
        Bitmap.class,
        new SetImageBitmapHook()
    );
    HookBridge.findAndHookMethod(
        ImageView.class,
        "setImageDrawable",
        Drawable.class,
        new SetImageDrawableHook()
    );
    // HookBridge.findAllAndHookMethod(
    //     BitmapFactory.class,
    //     "decodeStream",
    //     new DecodeStreamHook()
    // );
    // HookBridge.findAndHookMethod(
    //     BitmapFactory.class,
    //     "setDensityFromOptions",
    //     Bitmap.class,
    //     BitmapFactory.Options.class,
    //     new SetDensityFromOptionsHook()
    // );
    aLog.e(TAG, "hookBitmap end");
  }

  private static void hookGlideInto() {
    aLog.e(TAG, "hookGlideInto start");
    try {
      Class<?> clazz = Class.forName("com.bumptech.glide.RequestBuilder");
      HookBridge.findAndHookMethod(
          clazz,
          "into",
          ImageView.class,
          new GlideIntoHook()
      );
    } catch (Exception e) {
      aLog.ee(TAG, "hookGlideInto", e);
    }
    aLog.e(TAG, "hookGlideInto end");
  }

  private static class DecodeStreamHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.afterHookedMethod(param);
      Object arg = param.getArgs()[2];
      if (arg instanceof BitmapFactory.Options) {
        BitmapFactory.Options opts = (BitmapFactory.Options) arg;
        aLog.e(
            TAG,
            "DecodeStreamHook beforeHookedMethod inJustDecodeBounds:%s",
            opts.inJustDecodeBounds
        );
        aLog.e(TAG, "DecodeStreamHook beforeHookedMethod outWidth:%s", opts.outWidth);
        aLog.e(TAG, "DecodeStreamHook beforeHookedMethod outHeight:%s", opts.outHeight);
        // aLog.e(TAG, "DecodeStreamHook beforeHookedMethod %s", aUtil.object2JsonStr(arg));
      }
    }

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      super.afterHookedMethod(param);
      Object arg = param.getArgs()[2];
      if (arg instanceof BitmapFactory.Options) {
        BitmapFactory.Options opts = (BitmapFactory.Options) arg;
        aLog.e(
            TAG,
            "DecodeStreamHook afterHookedMethod inJustDecodeBounds:%s",
            opts.inJustDecodeBounds
        );
        aLog.e(TAG, "DecodeStreamHook afterHookedMethod outWidth:%s", opts.outWidth);
        aLog.e(TAG, "DecodeStreamHook afterHookedMethod outHeight:%s", opts.outHeight);
        // aLog.e(TAG, "DecodeStreamHook afterHookedMethod %s", aUtil.object2JsonStr(arg));
      }
    }
  }

  private static class SetDensityFromOptionsHook extends MethodHook {
    // BitmapFactory.setDensityFromOptions(Bitmap outputBitmap, Options opts)
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      Object arg = param.getArgs()[1];
      if (arg instanceof BitmapFactory.Options) {
        BitmapFactory.Options opts = (BitmapFactory.Options) arg;
        aLog.e(
            TAG,
            "SetDensityFromOptionsHook beforeHookedMethod inJustDecodeBounds:%s",
            opts.inJustDecodeBounds
        );
        aLog.e(TAG, "SetDensityFromOptionsHook beforeHookedMethod outWidth:%s", opts.outWidth);
        aLog.e(TAG, "SetDensityFromOptionsHook beforeHookedMethod outHeight:%s", opts.outHeight);
        // aLog.e(TAG, "SetDensityFromOptionsHook beforeHookedMethod %s", aUtil.object2JsonStr(arg));
      }
    }

    @Override
    public void afterHookedMethod(MethodParam param) throws Throwable {
      super.afterHookedMethod(param);
      Object arg = param.getArgs()[1];
      if (arg instanceof BitmapFactory.Options) {
        BitmapFactory.Options opts = (BitmapFactory.Options) arg;
        aLog.e(
            TAG,
            "SetDensityFromOptionsHook afterHookedMethod inJustDecodeBounds:%s",
            opts.inJustDecodeBounds
        );
        aLog.e(TAG, "SetDensityFromOptionsHook afterHookedMethod outWidth:%s", opts.outWidth);
        aLog.e(TAG, "SetDensityFromOptionsHook afterHookedMethod outHeight:%s", opts.outHeight);
        // aLog.e(TAG, "SetDensityFromOptionsHook afterHookedMethod %s", aUtil.object2JsonStr(arg));
        // aLog.ee(TAG, "SetDensityFromOptionsHook", new IllegalArgumentException());
      }
    }
  }

  private static class SetImageBitmapHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      // aLog.ee(TAG, "SetImageBitmapHook", new IllegalArgumentException());
      Object obj = param.getArgs()[0];
      if (obj instanceof Bitmap) {
        Bitmap bitmap = (Bitmap) obj;
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        ImageView imageView = (ImageView) param.getThisObject();
        int viewWidth = imageView.getMeasuredWidth();
        int viewHeight = imageView.getMeasuredHeight();
        checkBitmap(
            bitmapWidth,
            bitmapHeight,
            viewWidth,
            viewHeight,
            Integer.toHexString(imageView.hashCode()),
            new Throwable().getStackTrace()
        );
      }
    }
  }

  private static class SetImageDrawableHook extends MethodHook {
    public SetImageDrawableHook() {
    }

    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      // aLog.ee(TAG, "SetImageDrawableHook", new IllegalArgumentException());
      Object obj = param.getArgs()[0];
      if (obj instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) obj;
        int bitmapWidth = bitmapDrawable.getIntrinsicWidth();
        int bitmapHeight = bitmapDrawable.getIntrinsicHeight();
        ImageView imageView = (ImageView) param.getThisObject();
        int viewWidth = imageView.getMeasuredWidth();
        int viewHeight = imageView.getMeasuredHeight();
        checkBitmap(
            bitmapWidth,
            bitmapHeight,
            viewWidth,
            viewHeight,
            Integer.toHexString(imageView.hashCode()),
            new Throwable().getStackTrace()
        );
      }
    }
  }

  /**
   * 暂时认为如果加载的图片宽或高中的一个或者多个大于 view 的宽高的话
   * @param bitmapWidth
   * @param bitmapHeight
   * @param viewWidth
   * @param viewHeight
   * @param viewKey
   * @param trace
   */
  private static void checkBitmap(int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight,
      String viewKey, StackTraceElement[] trace) {
    if (bitmapWidth > viewWidth || bitmapHeight > viewHeight) {
      // 到这里就有问题了，然后需要看是通过框架库还是手动设置
      // 手动设置的话，需要提示，如果是框架库的话，需要找到框架库开始 load 的地方。
      List<String> traceList = StackTraceUtils.list(trace);
      boolean isInLibrary = false;
      for (String item : traceList) {
        if (inImageLibrary(item)) {
          isInLibrary = true;
          break;
        }
      }
      if (isInLibrary) {
        // 找到之前的 image library load image start 的地方
        Issue issue = imageViewLoadMap.remove(viewKey);
        if (null != issue) {
          issue.print();
        }
      } else {
        Issue issue = new BitmapIssue(traceList);
        issue.print();
      }
    } else {
      unlinkLoadImageAndImageView(viewKey);
    }
  }

  private static boolean inImageLibrary(String classAndMethod) {
    if (null == classAndMethod) {
      return false;
    }
    if (classAndMethod.startsWith("com.bumptech.glide.load.engine.EngineJob$CallResourceReady.run")) {
      // glide 库加载成功
      return true;
    }
    // if(classAndMethod.startsWith("com.bumptech.glide.load.engine.EngineJob$CallResourceReady.run")) {
    //
    // }
    return false;
  }

  private static class GlideIntoHook extends MethodHook {
    @Override
    public void beforeHookedMethod(MethodParam param) throws Throwable {
      super.beforeHookedMethod(param);
      // 记录下 image view 和调用栈之间的关联，后续会用到
      Object obj = param.getArgs()[0];
      if (obj instanceof ImageView) {
        linkLoadImageAndImageView(
            Integer.toHexString(obj.hashCode()),
            new Throwable().getStackTrace()
        );
      }
    }
  }

}
