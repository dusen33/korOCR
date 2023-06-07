package com.example.koreanocr;

import static android.net.wifi.p2p.WifiP2pManager.ERROR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

public class MyAnalyzer implements ImageAnalysis.Analyzer {
    static final int readingCount = 3;
    private TextRecognizer recognizer;
    private Button captureButton;
    private Context context;
    private TextView textView;
    private TextToSpeech tts;
    public MyAnalyzer(Context context) {
        this.context = context;
        // When using Korean script library
        this.recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        this.captureButton = MainActivity.captureButton;
        this.textView = MainActivity.textView;
        this.tts = tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
    }
    @Override
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void analyze(ImageProxy imageProxy) {
        recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // process image to recognizer
            Task<Text> result = recognizer.process(image)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text visionText) {
                            PriorityQueue<Text.TextBlock> pq = new PriorityQueue<>(new Comparator<Text.TextBlock>() {
                                @Override
                                public int compare(Text.TextBlock b1, Text.TextBlock b2) {
                                    int size1 = symbolSize(b1);
                                    int size2 = symbolSize(b2);
                                    if (size1 < size2) return 1;
                                    if (size1 > size2) return -1;
                                    return 0;
                                }
                            });

                            for(Text.TextBlock block : visionText.getTextBlocks())
                                pq.add(block);

                            StringBuffer sb = new StringBuffer();
                            StringBuffer tts_sb = new StringBuffer();
                            for(int i=0; i<readingCount && !pq.isEmpty(); i++) {
                                Text.TextBlock block = pq.poll();
                                try {
                                    sb.append(block.getText() + " : " + symbolSize(block) + "\n");
                                    tts_sb.append((block.getText())+"\n");
                                } catch (Exception e) {
                                    sb.append("인식 실패\n");
                                }
                            }


//                            Toast toast = Toast.makeText(context, String.valueOf(sb), Toast.LENGTH_SHORT);
                            // 버튼 조작을 통해 출력
                            captureButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    Log.d("success", String.valueOf(sb)+"-----------------------------------------------\n");
//                                    toast.show();

                                    textView.setText(String.valueOf(sb));
                                    // editText에 있는 문장을 읽는다.
                                    tts.speak(String.valueOf(tts_sb), TextToSpeech.QUEUE_FLUSH, null);
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

    int symbolSize(Text.TextBlock t){
        Text.Symbol s = t.getLines().get(0).getElements().get(0).getSymbols().get(0);
        return s.getBoundingBox().height() * s.getBoundingBox().width();
    }
}
