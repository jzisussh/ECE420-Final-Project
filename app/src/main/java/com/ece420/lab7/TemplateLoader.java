package com.ece420.lab7;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Pair;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TemplateLoader {
    public static List<Pair<Integer, Mat>> loadTemplates(Context context) {
        List<Pair<Integer, Mat>> screenList = new ArrayList<>();

        AssetManager assetManager = context.getAssets();
        int num = 2;
        while (num <= 2048) {
            String imagePath = String.format("template_%d.png", num);
            try {
                InputStream inputStream = assetManager.open(imagePath);
                byte[] imageBytes = new byte[inputStream.available()];
                inputStream.read(imageBytes);
                Mat grayImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_GRAYSCALE);
                if (!grayImage.empty()) {
                    screenList.add(new Pair<>(num, grayImage));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (num == 0) {
                num += 1;
            }
            num *= 2;
        }

        return screenList;
    }
}
