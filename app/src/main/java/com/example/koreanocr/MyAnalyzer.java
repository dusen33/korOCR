package com.example.koreanocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class MyAnalyzer implements ImageAnalysis.Analyzer {
    private TextRecognizer recognizer;
    private Button captureButton;
    private Context context;
    public MyAnalyzer(Context context) {
        this.context = context;
        // When using Korean script library
        this.recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        this.captureButton = MainActivity.captureButton;
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
                            String text = visionText.getText();

                            // 바로 바로 출력
                            // Log.d("recognizer", "Success:" + text);

                            CharSequence sequence = text;
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(context, sequence, duration);
                            // 버튼 조작을 통해 출력
                            captureButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    toast.show();
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
}
