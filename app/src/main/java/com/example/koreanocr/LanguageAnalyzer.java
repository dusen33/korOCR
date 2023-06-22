package com.example.koreanocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Locale;
import java.util.PriorityQueue;

public class LanguageAnalyzer implements ImageAnalysis.Analyzer {
    static final int readingCount = 5;
    private TextRecognizer recognizer;
    private PreviewView screen;
    private Context context;
    private TextView textView;
    private TextToSpeech tts;

    public LanguageAnalyzer(Context context) {
        this.context = context;
        this.recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        this.screen = WordDetectFragment.previewView;
        this.textView = WordDetectFragment.textView;

        this.tts = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.KOREAN);
                tts.setSpeechRate(2);
            }
        });
    }

    @Override
    @ExperimentalGetImage
    public void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = inputImageFromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        PriorityQueue<Text.Line> pq = new PriorityQueue<>((b1, b2) -> {
                            int size1 = wordSize(b1);
                            int size2 = wordSize(b2);
                            return Integer.compare(size2, size1);
                        });

                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                pq.add(line);
                            }
                        }

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < readingCount && !pq.isEmpty(); i++) {
                            Text.Line line = pq.poll();
                            try {
                                sb.append(line.getText()).append("\n");
                            } catch (Exception e) {
                                sb.append("인식 실패\n");
                            }
                        }
                        screen.setOnTouchListener((View v, MotionEvent event) -> {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    tts.speak(sb.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                                    textView.setText(sb.toString());
                                    return true;
                                case MotionEvent.ACTION_UP:
                                    tts.stop();
                                    return true;
                            }
                            return false;
                        });
                    })
                    .addOnFailureListener(e -> Log.d("recognizer", "Failure:" + e.getMessage()))
                    .addOnCompleteListener(proxy -> imageProxy.close());
        }
    }

    private InputImage inputImageFromMediaImage(Image mediaImage, int rotationDegrees) {
        ByteBuffer buffer = mediaImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.YUV_420_888, mediaImage.getWidth(), mediaImage.getHeight(), null);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
        int imageQuality = 100;
        yuvImage.compressToJpeg(rect, imageQuality, byteArrayOutputStream);
        byte[] jpegImageBytes = byteArrayOutputStream.toByteArray();

        Bitmap bmp = BitmapFactory.decodeByteArray(jpegImageBytes, 0, jpegImageBytes.length);
        Bitmap preprocessedBmp = preprocessImage(bmp);
        return InputImage.fromBitmap(preprocessedBmp, rotationDegrees);
    }

    private Bitmap preprocessImage(Bitmap image) {
        // Perform image preprocessing here.
        // For instance, resize, denoise, or increase contrast.
        return image;
    }

    int wordSize(Text.Line t) {
        Text.Element s = t.getElements().get(0);
        return s.getBoundingBox().height() * s.getBoundingBox().width();
    }
}
