package io.github.xanderwang.performance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import io.github.xanderwang.asu.ALog.e
import io.github.xanderwang.hook.HookBridge
import io.github.xanderwang.hook.core.MethodHook
import io.github.xanderwang.hook.core.MethodParam

/**
 * 监听图片的内存分配，大于某个值的时候 log 提示
 *
 *
 * Glide
 * Glide 加载图片的时候，最终会调用 ImageView.setImageBitmap 或者 ImageView.setImageDrawable 方法给 ImageView 设置图片。
 * 所以处理方法如下：
 * 1. 先记录 into 方法的调用栈 A ，记下 ImageView 的 hashCode 和调用栈 A 之间的关联
 * 2. 调用 ImageView.setImageBitmap 或者 ImageView.setImageDrawable 方法的时候，
 * 判断 Bitmap 的大小是否超过阈值，超过的话需要展示异常，如果没有超过的话，忽略。
 * 3. 有异常的时候，可以根据ImageView.setImageBitmap 或者 ImageView.setImageDrawable 的方法调用链里面是否包含
 * 'EngineJob$CallResourceReady.run' 方法来判断是手动设置还是框架回调，如果是框架回调，就需要找到之前的 into
 * 方法调用栈 A，如果不是框架回调，直接显示方法调用链。
 *
 *
 * Fresco
 * 待完成
 *
 * @author xander
 */
object BitmapTool {
    /**
     * log tag
     */
    private const val TAG = "BitmapTool"

    /**
     * 大图加载堆栈
     */
    private val imageViewLoadMap = HashMap<String, BitmapIssue>(64)
    private fun linkLoadImageAndImageView(imageViewKey: String, loadTrace: Array<StackTraceElement>) {
        val issue = BitmapIssue(Utils.list(loadTrace))
        imageViewLoadMap[imageViewKey] = issue
    }

    private fun unlinkLoadImageAndImageView(imageViewKey: String) {
        imageViewLoadMap.remove(imageViewKey)
    }

    @JvmStatic
    fun start() {
        e(TAG, "start")
        hookBitmap()
        hookGlideInto()
    }

    private fun hookBitmap() {
        e(TAG, "hookBitmap start")
        HookBridge.findAndHookMethod(ImageView::class.java, "setImageBitmap", Bitmap::class.java, SetImageBitmapHook())
        HookBridge.findAndHookMethod(ImageView::class.java, "setImageDrawable", Drawable::class.java,
                SetImageDrawableHook())
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
        e(TAG, "hookBitmap end")
    }

    private fun hookGlideInto() {
        e(TAG, "hookGlideInto start")
        try {
            val clazz = Class.forName("com.bumptech.glide.RequestBuilder")
            HookBridge.findAndHookMethod(clazz, "into", ImageView::class.java, GlideIntoHook())
        } catch (e: Exception) {
            e(TAG, "hookGlideInto", e)
        }
        e(TAG, "hookGlideInto end")
    }

    /**
     * 暂时认为如果加载的图片宽或高中的一个或者多个大于 view 的宽高的话
     *
     * @param bitmapWidth
     * @param bitmapHeight
     * @param viewWidth
     * @param viewHeight
     * @param viewKey
     * @param trace
     */
    private fun checkBitmap(bitmapWidth: Int, bitmapHeight: Int, viewWidth: Int, viewHeight: Int, viewKey: String,
        trace: Array<StackTraceElement>) {
        e(TAG, "checkBitmap bitmapWidth:%s ,bitmapHeight:%s ,viewWidth:%s ,viewHeight:%s", bitmapWidth, bitmapHeight,
                viewWidth, viewHeight)
        if ((bitmapWidth > viewWidth || bitmapHeight > viewHeight) && viewWidth > 0 && viewHeight > 0) {
            // 到这里就有问题了，然后需要看是通过框架库还是手动设置
            // 手动设置的话，需要提示，如果是框架库的话，需要找到框架库开始 load 的地方。
            val traceList = Utils.list(trace)
            var isInLibrary = false
            for (item in traceList) {
                if (inImageLibrary(item)) {
                    isInLibrary = true
                    break
                }
            }
            var issue: BitmapIssue? = null
            issue = if (isInLibrary) {
                // 找到之前的 image library load image start 的地方
                imageViewLoadMap.remove(viewKey)
            } else {
                BitmapIssue(traceList)
            }
            if (null != issue) {
                issue.imageInfo(viewWidth, viewHeight, bitmapWidth, bitmapHeight)
                issue.print()
            }
        } else {
            unlinkLoadImageAndImageView(viewKey)
        }
    }

    private fun inImageLibrary(classAndMethod: String?): Boolean {
        if (null == classAndMethod) {
            return false
        }
        return if (classAndMethod.startsWith("com.bumptech.glide.load.engine.EngineJob\$CallResourceReady.run")) {
            // glide 库加载成功
            true
        } else false
        // if(classAndMethod.startsWith("com.bumptech.glide.load.engine.EngineJob$CallResourceReady.run")) {
        //
        // }
    }

    /**
     * 图片加载堆栈
     */
    private class BitmapIssue(data: Any?) : Issue(TYPE_BITMAP, "LARGE BITMAP", data) {
        /**
         * 视图宽
         */
        var viewWidth = 0

        /**
         * 视图高
         */
        var viewHeight = 0

        /**
         * 图片宽
         */
        var imageWidth = 0

        /**
         * 图片高
         */
        var imageHeight = 0

        /**
         * 图片信息
         *
         * @param viewWidth   视图宽
         * @param viewHeight  视图高
         * @param imageWidth  图片宽
         * @param imageHeight 图片高
         */
        fun imageInfo(viewWidth: Int, viewHeight: Int, imageWidth: Int, imageHeight: Int) {
            this.viewWidth = viewWidth
            this.viewHeight = viewHeight
            this.imageWidth = imageWidth
            this.imageHeight = imageHeight
        }

        /**
         * 构建额外的信息
         * @param sb 外部传入的 stringbuilder ，用于插入额外信息
         */
        override fun formatExtraInfo(sb: StringBuilder?) {
            if (viewWidth > 0) {
                sb?.append("image info:")?.append("viewWidth:")?.append(viewWidth)?.append(",viewHeight:")?.append(viewHeight)
                    ?.append(",imageWidth:")?.append(imageWidth)?.append(",imageHeight:")?.append(imageHeight)?.append('\n')
            }
        }
    }

    private class DecodeStreamHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            super.afterHookedMethod(param)
            val arg = param?.args?.get(2)
            if (arg is BitmapFactory.Options) {
                e(TAG, "DecodeStreamHook beforeHookedMethod inJustDecodeBounds:%s", arg.inJustDecodeBounds)
                e(TAG, "DecodeStreamHook beforeHookedMethod outWidth:%s", arg.outWidth)
                e(TAG, "DecodeStreamHook beforeHookedMethod outHeight:%s", arg.outHeight)
                // aLog.e(TAG, "DecodeStreamHook beforeHookedMethod %s", aUtil.object2JsonStr(arg));
            }
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            super.afterHookedMethod(param)
            val arg = param?.args?.get(2)
            if (arg is BitmapFactory.Options) {
                e(TAG, "DecodeStreamHook afterHookedMethod inJustDecodeBounds:%s", arg.inJustDecodeBounds)
                e(TAG, "DecodeStreamHook afterHookedMethod outWidth:%s", arg.outWidth)
                e(TAG, "DecodeStreamHook afterHookedMethod outHeight:%s", arg.outHeight)
                // aLog.e(TAG, "DecodeStreamHook afterHookedMethod %s", aUtil.object2JsonStr(arg));
            }
        }
    }

    private class SetDensityFromOptionsHook : MethodHook() {
        // BitmapFactory.setDensityFromOptions(Bitmap outputBitmap, Options opts)
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            super.beforeHookedMethod(param)
            val arg = param?.args?.get(1)
            if (arg is BitmapFactory.Options) {
                e(TAG, "SetDensityFromOptionsHook beforeHookedMethod inJustDecodeBounds:%s", arg.inJustDecodeBounds)
                e(TAG, "SetDensityFromOptionsHook beforeHookedMethod outWidth:%s", arg.outWidth)
                e(TAG, "SetDensityFromOptionsHook beforeHookedMethod outHeight:%s", arg.outHeight)
                // aLog.e(TAG, "SetDensityFromOptionsHook beforeHookedMethod %s", aUtil.object2JsonStr(arg));
            }
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodParam?) {
            super.afterHookedMethod(param)
            val arg = param?.args?.get(1)
            if (arg is BitmapFactory.Options) {
                e(TAG, "SetDensityFromOptionsHook afterHookedMethod inJustDecodeBounds:%s", arg.inJustDecodeBounds)
                e(TAG, "SetDensityFromOptionsHook afterHookedMethod outWidth:%s", arg.outWidth)
                e(TAG, "SetDensityFromOptionsHook afterHookedMethod outHeight:%s", arg.outHeight)
                // aLog.e(TAG, "SetDensityFromOptionsHook afterHookedMethod %s", aUtil.object2JsonStr(arg));
                // aLog.ee(TAG, "SetDensityFromOptionsHook", new IllegalArgumentException());
            }
        }
    }

    private class SetImageBitmapHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            super.beforeHookedMethod(param)
            // aLog.ee(TAG, "SetImageBitmapHook", new IllegalArgumentException());
            val obj = param?.args?.get(0)
            if (obj is Bitmap) {
                val bitmapWidth = obj.width
                val bitmapHeight = obj.height
                val imageView = param.thisObject as ImageView
                val viewWidth = imageView.measuredWidth
                val viewHeight = imageView.measuredHeight
                checkBitmap(bitmapWidth, bitmapHeight, viewWidth, viewHeight, Integer.toHexString(imageView.hashCode()),
                        Throwable().stackTrace)
            }
        }
    }

    private class SetImageDrawableHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            super.beforeHookedMethod(param)
            // aLog.ee(TAG, "SetImageDrawableHook", new IllegalArgumentException());
            val obj = param?.args?.get(0)
            if (obj is BitmapDrawable) {
                val bitmapWidth = obj.intrinsicWidth
                val bitmapHeight = obj.intrinsicHeight
                val imageView = param.thisObject as ImageView
                val viewWidth = imageView.measuredWidth
                val viewHeight = imageView.measuredHeight
                checkBitmap(bitmapWidth, bitmapHeight, viewWidth, viewHeight, Integer.toHexString(imageView.hashCode()),
                        Throwable().stackTrace)
            }
        }
    }

    private class GlideIntoHook : MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodParam?) {
            super.beforeHookedMethod(param)
            // 记录下 image view 和调用栈之间的关联，后续会用到
            val obj = param?.args?.get(0)
            if (obj is ImageView) {
                linkLoadImageAndImageView(Integer.toHexString(obj.hashCode()), Throwable().stackTrace)
            }
        }
    }
}