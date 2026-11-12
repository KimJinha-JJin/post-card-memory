package com.postcardmemory.exportprobe.day130;

// Renderer probe hosted by a separate, data-isolated QA package. No production Application/DB/MediaStore.
import android.content.Context;
import android.content.ContextWrapper;
import android.content.ContentResolver;
import android.graphics.*;
import android.net.Uri;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public final class StickerExportProbe {
    static File root;
    static Context context;

    static final class Renderer {
        final ClassLoader loader;
        final Object exporter;
        final Class<?> overlay, style;
        final Method draw, decode;
        Renderer(String apk) throws Exception {
            loader = new DexClassLoader(apk, null, null, ClassLoader.getSystemClassLoader());
            Class<?> type = loader.loadClass("com.postcardmemory.utils.PostcardImageExporter");
            exporter = type.getField("INSTANCE").get(null);
            overlay = loader.loadClass(type.getName() + "$StickerOverlay");
            style = loader.loadClass("com.postcardmemory.ui.detail.PhotoStickerEdgeStyle");
            draw = type.getDeclaredMethod("drawStickerOverlay", Context.class, Canvas.class, overlay);
            draw.setAccessible(true);
            decode = Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.getName().equals("decodeStickerBitmap")).findFirst().get();
            decode.setAccessible(true);
        }
        Object overlay(File file, float ratio, boolean fit, float angle, boolean h, boolean v,
                       String edge, File original) throws Exception {
            Object edgeValue = style.getField(edge).get(null);
            return overlay.getDeclaredConstructor(Uri.class, float.class, float.class, float.class,
                    Uri.class, boolean.class, float.class, boolean.class, boolean.class, style, long.class)
                .newInstance(Uri.fromFile(file), .25f, .25f, ratio,
                    original == null ? null : Uri.fromFile(original), fit, angle, h, v, edgeValue, 129L);
        }
        Bitmap render(File file, float ratio, boolean fit, float angle, boolean h, boolean v,
                      String edge, File original) throws Exception {
            Bitmap out = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888);
            try {
                draw.invoke(exporter, context, new Canvas(out), overlay(file, ratio, fit, angle, h, v, edge, original));
                return out;
            } catch (Throwable t) { out.recycle(); throw t; }
        }
        Bitmap decode(File file, float ratio, boolean fit) throws Exception {
            return (Bitmap) (decode.getParameterCount() == 2
                ? decode.invoke(exporter, context, Uri.fromFile(file))
                : decode.invoke(exporter, context, Uri.fromFile(file), Math.max(1f, Math.min(2048f, ratio * 2048)), fit));
        }
    }

    public static void run(File comparisonDir, File appFilesDir) throws Exception {
        System.out.println("PROBE_START");
        root = comparisonDir.getCanonicalFile();
        File owner = appFilesDir.getCanonicalFile();
        if (!owner.getName().equals("files") ||
            !owner.getParentFile().getName().equals("com.postcardmemory.exportprobe.day130") ||
            !root.getParentFile().equals(owner) || !root.getName().equals("comparison-130") || !root.isDirectory())
            throw new IllegalArgumentException("Use the isolated QA package directory");
        // All fixtures are file://; the exporter reads but does not use this resolver.
        // Do not initialize ActivityThread/systemMain or any Application.
        context = new ContextWrapper(null) {
            @Override public ContentResolver getContentResolver() { return null; }
        };
        Renderer before = new Renderer(new File(root, "before.apk").getPath());
        Renderer after = new Renderer(new File(root, "after.apk").getPath());
        System.out.println("RENDERERS_READY");
        File square = fixture("square", 2992, 2992, false, false);
        File wide = fixture("wide", 4000, 1000, false, false);
        File alpha = fixture("alpha", 2993, 1999, true, false);
        File small = fixture("small", 120, 80, true, false);
        File jpeg = fixture("jpeg", 2992, 1998, false, true);
        System.out.println("case,beforeWidth,beforeHeight,afterWidth,afterHeight,beforeAllocationBytes,afterAllocationBytes,meanCompositeChannelError,maxCompositeChannelError,alphaBoundsDelta");
        compare(before, after, "square", square, .25f, false, 0, false, false, "DEFAULT", null);
        compare(before, after, "crop-wide", wide, .25f, false, 0, false, false, "DEFAULT", null);
        compare(before, after, "cutout-alpha", alpha, .25f, true, 0, false, false, "DEFAULT", null);
        compare(before, after, "small", small, .25f, true, 0, false, false, "DEFAULT", null);
        compare(before, after, "large", square, .75f, false, 0, false, false, "DEFAULT", null);
        compare(before, after, "tiny", square, .10f, false, 0, false, false, "DEFAULT", null);
        compare(before, after, "rotate-flip", jpeg, .20f, false, 37, true, true, "DEFAULT", null);
        compare(before, after, "alpha-rotate", alpha, .25f, true, 37, true, false, "DEFAULT", null);
        for (String edge : new String[]{"POLAROID", "SCISSOR", "TORN", "MAGAZINE"})
            compare(before, after, edge, jpeg, .20f, false, -21, true, false, edge, null);
        compare(before, after, "cutout-fallback", new File(root, "missing.png"), .25f, true, 37, false, true, "DEFAULT", wide);
        Class<?> exif = after.loader.loadClass("androidx.exifinterface.media.ExifInterface");
        for (int orientation = 1; orientation <= 8; orientation++) {
            File f = new File(root, "exif-" + orientation + ".jpg");
            try (InputStream in = new FileInputStream(jpeg); OutputStream out = new FileOutputStream(f)) {
                byte[] buf = new byte[16384]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            Object metadata = exif.getConstructor(String.class).newInstance(f.getPath());
            exif.getMethod("setAttribute", String.class, String.class).invoke(metadata, "Orientation", "" + orientation);
            exif.getMethod("saveAttributes").invoke(metadata);
            compare(before, after, "exif-" + orientation, f, .20f, false, 13, true, false, "DEFAULT", null);
        }
        for (String fileName : new String[]{"portrait.jpg", "landscape.jpg", "fur.png"}) {
            File photo = new File(root, fileName);
            for (float ratio : new float[]{.25f, .10f, .75f})
                compare(before, after, fileName + "-" + ratio, photo, ratio, false, 0, false, false, "DEFAULT", null);
            compare(before, after, fileName + "-rotate", photo, .10f, false, 37, true, true, "DEFAULT", null);
            benchmark(before, after, fileName, photo, .25f);
            benchmark(before, after, fileName, photo, .10f);
        }
        compare(before, after, "fur-native-tiny", new File(root,"fur.png"), .03f, false, 0, false, false, "DEFAULT", null);
        benchmark(before, after, "fur-tiny", new File(root,"fur.png"), .03f);
        System.out.println("PROBE_COMPLETED");
    }

    // Same QA process, two warmups + five measured pairs; AB/BA alternates.
    // PSS snapshots are NOT an app-wide peak, and recycle is not proof of OS page release.
    static void benchmark(Renderer a, Renderer b, String name, File file, float ratio) throws Exception {
        for (int round = -2; round < 5; round++) {
            for (int turn = 0; turn < 2; turn++) {
                boolean useAfter = ((round + 2 + turn) & 1) != 0;
                Renderer renderer = useAfter ? b : a;
                android.os.Debug.MemoryInfo idle = new android.os.Debug.MemoryInfo();
                android.os.Debug.getMemoryInfo(idle);
                long start = System.nanoTime();
                Bitmap bitmap = renderer.decode(file, ratio, false);
                long nanos = System.nanoTime() - start;
                int bytes = bitmap.getAllocationByteCount();
                android.os.Debug.MemoryInfo live = new android.os.Debug.MemoryInfo();
                android.os.Debug.getMemoryInfo(live);
                bitmap.recycle();
                android.os.Debug.MemoryInfo recycled = new android.os.Debug.MemoryInfo();
                android.os.Debug.getMemoryInfo(recycled);
                System.out.println("BENCH,"+name+","+ratio+","+round+","+(useAfter?"after":"before")+","+
                    nanos+","+bytes+","+idle.getTotalPss()+","+live.getTotalPss()+","+recycled.getTotalPss());
            }
        }
    }
    static File fixture(String name, int w, int h, boolean alpha, boolean jpeg) throws Exception {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        try {
            Canvas c = new Canvas(b);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            p.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.rgb(22, 95, 72), Color.rgb(244, 211, 119), Color.rgb(132, 45, 86)}, null, Shader.TileMode.CLAMP));
            if (alpha) { p.setAlpha(190); c.drawOval(w*.07f, h*.07f, w*.93f, h*.93f, p); }
            else c.drawRect(0, 0, w, h, p);
            p.setShader(null); p.setAlpha(255);
            p.setColor(Color.WHITE); p.setTextSize(w / 20f);
            c.drawText("TOP / 129", w*.23f, h*.29f, p);
            p.setColor(Color.BLACK); p.setStrokeWidth(3);
            for (int x = w/4; x < w*3/4; x += 13) c.drawLine(x, h*.35f, x + w*.06f, h*.65f, p);
            p.setColor(Color.CYAN); c.drawCircle(w*.30f, h*.76f, w*.035f, p);
            p.setColor(Color.RED); c.drawCircle(w*.72f, h*.25f, w*.035f, p);
            File f = new File(root, name + (jpeg ? ".jpg" : ".png"));
            try (OutputStream out = new FileOutputStream(f)) {
                if (!b.compress(jpeg ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, 95, out))
                    throw new IOException("fixture encoding failed");
            }
            return f;
        } finally { b.recycle(); }
    }

    static void compare(Renderer a, Renderer b, String name, File file, float ratio, boolean fit,
                        float angle, boolean h, boolean v, String edge, File fallback) throws Exception {
        File decodedFile = fallback == null ? file : fallback;
        Bitmap da = a.decode(decodedFile, ratio, fallback == null && fit);
        Bitmap db = b.decode(decodedFile, ratio, fallback == null && fit);
        String allocation = da.getWidth()+","+da.getHeight()+","+db.getWidth()+","+db.getHeight()+","+
            da.getAllocationByteCount()+","+db.getAllocationByteCount();
        da.recycle(); db.recycle();
        Bitmap before = a.render(file, ratio, fit, angle, h, v, edge, fallback);
        Bitmap after = b.render(file, ratio, fit, angle, h, v, edge, fallback);
        try {
            long error = 0, count = 0; int max = 0;
            Rect ba = new Rect(2048,2048,0,0), bb = new Rect(2048,2048,0,0);
            int[] rowA = new int[2048], rowB = new int[2048];
            for (int y=0; y<2048; y++) {
                before.getPixels(rowA,0,2048,0,y,2048,1); after.getPixels(rowB,0,2048,0,y,2048,1);
                for (int x=0; x<2048; x++) {
                    int ca=rowA[x], cb=rowB[x], aa=Color.alpha(ca), ab=Color.alpha(cb);
                    if (aa>8) { ba.left=Math.min(ba.left,x);ba.top=Math.min(ba.top,y);ba.right=Math.max(ba.right,x+1);ba.bottom=Math.max(ba.bottom,y+1); }
                    if (ab>8) { bb.left=Math.min(bb.left,x);bb.top=Math.min(bb.top,y);bb.right=Math.max(bb.right,x+1);bb.bottom=Math.max(bb.bottom,y+1); }
                    if (aa==0 && ab==0) continue;
                    for (int shift : new int[]{0,8,16}) {
                        int va=(((ca>>shift)&255)*aa+255*(255-aa)+127)/255;
                        int vb=(((cb>>shift)&255)*ab+255*(255-ab)+127)/255;
                        int d=Math.abs(va-vb);error+=d;max=Math.max(max,d);count++;
                    }
                }
            }
            int boundsDelta=Math.max(Math.max(Math.abs(ba.left-bb.left),Math.abs(ba.top-bb.top)),
                Math.max(Math.abs(ba.right-bb.right),Math.abs(ba.bottom-bb.bottom)));
            System.out.println(name+","+allocation+","+(count==0?0:error/(double)count)+","+max+","+boundsDelta);
            // Same native Canvas render path, both before/after outputs retained in scratch only.
            save(before, name+"-before.png");save(after, name+"-after.png");
            if (ba.isEmpty() || bb.isEmpty()) throw new AssertionError(name+": empty sticker render");
            if (boundsDelta>2) throw new AssertionError(name+": shifted alpha bounds "+boundsDelta);
            if ((name.equals("small") || name.equals("large") || name.equals("crop-wide")) && !before.sameAs(after))
                throw new AssertionError(name+": unsampled rendering changed");
        } finally { before.recycle(); after.recycle(); }
    }
    static void save(Bitmap b, String name) throws Exception {
        try (OutputStream out=new FileOutputStream(new File(root,name))) {
            if (!b.compress(Bitmap.CompressFormat.PNG,100,out)) throw new IOException("output encoding failed");
        }
    }
}
