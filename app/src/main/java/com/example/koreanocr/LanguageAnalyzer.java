package com.example.koreanocr;

import static android.net.wifi.p2p.WifiP2pManager.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import android.content.Context;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

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
        // When using Korean script library
        this.recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        this.screen = WordDetectFragment.previewView;
        this.textView = WordDetectFragment.textView;

        this.tts = new TextToSpeech( context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(2);
                }
            }
        });
    }

    @Override
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // process image to recognizer
            Task<Text> result = recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            PriorityQueue<Text.Line> pq = new PriorityQueue<>(new Comparator<Text.Line>() {
                                @Override
                                public int compare(Text.Line b1, Text.Line b2) {
                                    int size1 = wordSize(b1);
                                    int size2 = wordSize(b2);
                                    if (size1 < size2) return 1;
                                    if (size1 > size2) return -1;
                                    return 0;
                                }
                            });

                            for(Text.TextBlock block : visionText.getTextBlocks())
                                for(Text.Line line : block.getLines())
//                                    if(line.getConfidence() > 0.7)
                                        pq.add(line);

                            StringBuffer sb = new StringBuffer();
                            for(int i=0; i<readingCount && !pq.isEmpty(); i++) {
                                Text.Line line = pq.poll();
                                try {
                                    sb.append(line.getText() + "\n");
                                } catch (Exception e) {
                                    sb.append("인식 실패\n");
                                }
                            }
                            screen.setOnTouchListener(new View.OnTouchListener() {
                                @Override
                                public boolean onTouch(View v, MotionEvent event) {
                                    switch(event.getAction()) {
                                        case MotionEvent.ACTION_DOWN:
                                            // tts 실행
                                            tts.speak(sb.toString(), QUEUE_FLUSH, null, null);
                                            textView.setText(String.valueOf(sb));
                                            return true;
                                        case MotionEvent.ACTION_UP:
                                            // RELEASED
                                            tts.stop();
                                            return true;
                                    }
                                    return false;
                                }
                            });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("recognizer", "Failure:" + e.getMessage());
                        }
                    })
                    .addOnCompleteListener(proxy -> imageProxy.close());
        }
    }

    int wordSize(Text.Line t){
        Text.Element s = t.getElements().get(0);
        return s.getBoundingBox().height() * s.getBoundingBox().width();
    }
}
