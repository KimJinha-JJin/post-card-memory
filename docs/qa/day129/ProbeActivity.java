package com.postcardmemory.exportprobe.day129;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.*;

/** No permissions. Only this fresh QA package's assets/files are read or written. */
public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView label = new TextView(this);
        label.setPadding(32, 64, 32, 32);
        label.setText("129일차 스티커 출력 비교 중\n검증 전용 앱 · 사용자 데이터 접근 없음");
        setContentView(label);
        new Thread(() -> {
            File dir = new File(getFilesDir(), "comparison-2");
            // Refuse repeat launches rather than overwrite a previous result.
            if (!dir.mkdir()) {
                runOnUiThread(() -> label.setText("기존 비교 결과가 있어 재실행을 멈췄어."));
                return;
            }
            try (PrintStream log = new PrintStream(new File(dir, "probe.log"), "UTF-8")) {
                System.setOut(log);
                try {
                    for (String name : new String[]{"before.apk", "after.apk"}) {
                        File target = new File(dir, name);
                        try (InputStream in = getAssets().open(name); OutputStream out = new FileOutputStream(target)) {
                            byte[] buffer = new byte[65536]; int n;
                            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                        }
                        if (!target.setReadOnly()) throw new IOException("Cannot protect APK copy");
                    }
                    StickerExportProbe.run(dir, getFilesDir());
                    runOnUiThread(() -> label.setText("출력 비교 완료\n결과는 검증 전용 앱 안에만 있어."));
                } catch (Throwable t) {
                    t.printStackTrace(log);
                    log.println("PROBE_FAILED");
                    runOnUiThread(() -> label.setText("비교 중 오류가 있어. 로그 확인이 필요해."));
                }
            } catch (Throwable t) {
                runOnUiThread(() -> label.setText("검증 로그를 만들지 못했어."));
            }
        }, "day129-render-probe").start();
    }
}
