package com.dji.videostreamdecodingsample.FlightControl;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.List;


public class Targeting {
    private Context appContext;
    private String modelWeights;
    private String modelConfiguration;
    private Net net;
    private final int INPUT_WIDTH = 416;
    private final int INPUT_HEIGHT = 416;
    private final double IMG_SCALING = 1/255.0;

    private int numDetections = 0;

    private static final int EXCLUSION_LEFT_BOUND = 400;
    private static final int EXCLUSION_RIGHT_BOUND = 1520;
    private static final float THRESHOLD = 0.4f; // Insert thresholding beyond which the model will detect objects//
    private StringBuilder stringBuilder;

    private Size sz = new Size(INPUT_WIDTH, INPUT_HEIGHT);
    private Mat detections = new Mat();
    private List<Mat> detectedObjects = new ArrayList<>();
    private List<Integer> clsIds = new ArrayList<>();
    private List<Float> confs = new ArrayList<>();
    private List<Rect2d> rects = new ArrayList<>();
    private List<String> outBlobNames;

    public Targeting(Context context) {
        this.appContext = context;
        initNet();
    }

    public String getPath(String fileName){

        AssetManager assetManager = appContext.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(fileName));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(appContext.getFilesDir(), fileName);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void initNet(){
        try {
            modelWeights = getPath("yolov3tiny.weights");
            modelConfiguration = getPath("yolov3tiny.cfg");
            net = Dnn.readNetFromDarknet(modelConfiguration, modelWeights);
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    public Mat analyzeVideo(Mat image) {
        Log.d("OPENCV", "Analyze Video");
        return analyzeFrame(image, net);
    }




    private List<String> getOutputNames(Net net) { //not mine
        List<String> names = new ArrayList<>();
        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();

        for(Integer i : outLayers){
            names.add(layersNames.get(i-1));
        }
        return names;
    }


    private Mat analyzeFrame(Mat frame, Net net) {
        int centerX;
        int centerY;
        int width;
        int height;
        int right;
        int left;
        int top;
        int bottom;
        int classId;
        Mat row;
        Mat scores;
        Mat level;
        Core.MinMaxLocResult mm;
        float confidence;
        Point classIdPoint;
        Log.d("OPENCV", "analyzeFrame" + frame.size());

        Mat blob = Dnn.blobFromImage(frame, IMG_SCALING, sz, new Scalar(0), true, true);  //edit this maybe, scalar is empty rn, so no mean subtraction i think
        net.setInput(blob);
        net.forward(detectedObjects); //detections is a 4d tensor, images, height, width, color channels

        //detections = detections.reshape(1, (int)detections.total() / 7);

        for (int i = 0; i < detectedObjects.size(); ++i) {
            // each row is a candidate detection, the 1st 4 numbers are
            // [center_x, center_y, width, height], followed by (N-4) class probabilities

            level = detectedObjects.get(i); //gets i output blob image from network, now a 3d tensor, height, width, color channels

            for (int j = 0; j < level.rows(); ++j) {
                row = level.row(j); // gets the data for the image,
                scores = row.colRange(5, level.cols()); //scores are class probabilities listed after the first 4 values
                mm = Core.minMaxLoc(scores); //finds maximum score in class probabilities
                confidence = (float) mm.maxVal; //confidence = maxVal
                classIdPoint = mm.maxLoc; //Id index is the index of maxVal

                if (confidence > THRESHOLD) {
                    centerX = (int) (row.get(0, 0)[0] * frame.cols()); //gets centerX from output blob and scales it for the input image
                    centerY = (int) (row.get(0, 1)[0] * frame.rows()); //same but with centerY
                    width = (int) (row.get(0, 2)[0] * frame.cols()); //same but with width
                    height = (int) (row.get(0, 3)[0] * frame.rows() + 100);//same but with height
                    left = centerX - width / 2;
                    top = centerY - height / 2;

                    clsIds.add((int) classIdPoint.x);
                    confs.add((float) confidence);
                    rects.add(new Rect2d(left, top, width, height));
                }
            }
            /*double confidence = detections.get(i, 2)[0];
            if (confidence > THRESHOLD) {
                classId = (int)detections.get(i, 1)[0];
                left   = (int)(detections.get(i, 3)[0] * cols);
                top    = (int)(detections.get(i, 4)[0] * rows);
                right  = (int)(detections.get(i, 5)[0] * cols);
                bottom = (int)(detections.get(i, 6)[0] * rows);
                // Draw rectangle around detected object.
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom),
                        new Scalar(0, 255, 0));
                String label = "Human" + ": " + confidence;
                int[] baseLine = new int[1];
                Size labelSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
                // Draw background for label.
                Imgproc.rectangle(frame, new Point(left, top - labelSize.height),
                        new Point(left + labelSize.width, top + baseLine[0]),
                        new Scalar(255, 255, 255), Imgproc.FILLED);
                // Write class name and confidence.
                Imgproc.putText(frame, label, new Point(left, top),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
            }*/
        }

        float nmsThresh = 0.25f;
        MatOfFloat confidences;
        Log.d("OPENCV", "" + confs.size());
        if (!confs.isEmpty()) {
            confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));
            Rect2d[] boxesArray = rects.toArray(new Rect2d[rects.size()]);
            MatOfRect2d boxes = new MatOfRect2d();
            boxes.fromArray(boxesArray);
            MatOfInt indices = new MatOfInt();
            Dnn.NMSBoxes(boxes, confidences, THRESHOLD, nmsThresh, indices); //eliminates weaker classifications of same objects

            //System.out.println(clsIds.get(0));
            int[] ind = indices.toArray();
            for (int idx : ind) {
                Rect2d box = boxesArray[idx];
                Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 255, 0), 2);
            }
            //drawLargestBox(boxesArray, ind, frame); //only biggest object drawn
            numDetections = ind.length;
        }


        Log.d("TARGETING", "FOUND " + confs.size() + " HUMAN?");
        return frame;
    }

    private void drawLargestBox(Rect2d[] boxes, int[] indices, Mat frame) {
        double largestArea = 0;
        Rect2d largestBox = null;
        Rect2d curBox;
        for (int index : indices) {
            curBox = boxes[index];
            if (curBox.x >= EXCLUSION_LEFT_BOUND && curBox.x <= EXCLUSION_RIGHT_BOUND) {
                if (curBox.area() >= largestArea) {
                    largestBox = curBox;
                    largestArea = curBox.area();
                }
            }
        }
        assert largestBox != null;
        Imgproc.rectangle(frame, largestBox.tl(), largestBox.br(), new Scalar(255, 200, 0), 2); //scalar is color (B, G, R)

    }


    public Mat getOutFrame(){
        return this.detections;
    }

    private Mat byteVidToMat(byte[] vidFrame){
        MatOfByte mob = new MatOfByte();
        mob.fromArray(vidFrame);
        Log.d("OPENCV", "byteVidToMat " + vidFrame.length);
        Log.d("OPENCV", "byteVidToMat Return " + mob.size());

        if(vidFrame != null) {
            return mob;
        }

        return null;
    }

    public String detections(){
        String val =  "Found: " + numDetections + " humans.";
        numDetections = 0;
        detectedObjects.clear();
        clsIds.clear();
        confs.clear();
        rects.clear();
        return val;
    }
}



