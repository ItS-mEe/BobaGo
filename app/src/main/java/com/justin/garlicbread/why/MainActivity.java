/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.justin.garlicbread.why;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends Activity {
    private static final String API_KEY = BuildConfig.API_KEY;
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private TextView bigNumber;
    private double currentTotal;
    private File thisMonthsFile, ratingFile;
    private GlobalRating gr;
    private String name = null;
    private boolean shouldLaunchPopup = false;



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder
                    .setMessage(R.string.dialog_select_prompt)
                    .setPositiveButton(R.string.dialog_select_gallery, (dialog, which) -> startGalleryChooser())
                    .setNegativeButton(R.string.dialog_select_camera, (dialog, which) -> startCamera());
            builder.create().show();
        });

        FloatingActionButton fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            LinearLayout ll = new LinearLayout(this);
            ll.setGravity(Gravity.CENTER_HORIZONTAL);
            ll.setOrientation(LinearLayout.VERTICAL);
            HashMap<String, int[]> locations = gr.getRatings();
            for(String loc : locations.keySet()){
                TextView value = new TextView(this);
                value.setGravity(Gravity.CENTER_HORIZONTAL);
                value.setText(String.format(Locale.US, loc + " rated: %.2f", (double)locations.get(loc)[1]/locations.get(loc)[2]));
                ll.addView(value);
            }
            builder
                    .setView(ll)
                    .setMessage("Here are the average ratings for boba stores: ")
                    .setPositiveButton("OK", (dialog, which) -> {});
            builder.create().show();
        });

        FloatingActionButton fab3 = findViewById(R.id.fab3);
        fab3.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            LinearLayout v = new LinearLayout(this);
            v.setGravity(Gravity.CENTER_HORIZONTAL);
            GraphView graph = new GraphView(this);
            DataPoint[] dps = new DataPoint[31];
            double[] hist = Storage.getHistory(thisMonthsFile);
            for(int i = 0; i<dps.length; i++){
                dps[i] = new DataPoint(i, hist[i]);
            }
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dps);
            graph.addSeries(series);
            graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
            graph.setLayoutParams(new LinearLayout.LayoutParams(800,800));
            graph.setPadding(20,20,20,20);
            graph.getGridLabelRenderer().setNumHorizontalLabels(6);
            graph.getGridLabelRenderer().setPadding(20);
            graph.getViewport().setMinX(1);
            graph.getViewport().setMaxX(31);
            graph.getViewport().setXAxisBoundsManual(true);

            v.addView(graph);

            builder
                    .setView(v)
                    .setMessage("Here are your expenditures over time: ")
                    .setPositiveButton("OK", (dialog, which) -> {});
            Dialog dialog = builder.create();
            dialog.show();
        });

        File topFile = this.getFilesDir();

        Date today = Date.from(Instant.now());
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);

        String thisMonthFileName = cal.get(Calendar.MONTH)+"_"+cal.get(Calendar.YEAR);
        thisMonthsFile = new File(topFile, thisMonthFileName);
        //Storage.reset(thisMonthsFile);

        currentTotal = Storage.read(thisMonthsFile);
        ratingFile = new File(topFile, "RATINGS");
        gr = new GlobalRating(ratingFile);

        bigNumber = findViewById(R.id.bigNumber);
        bigNumber.setText(String.format(Locale.US, "$%.2f", currentTotal));
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION);

                callCloudVision(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("DOCUMENT_TEXT_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private static class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return mActivityWeakReference.get().convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String result) {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                if(result != "") {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder
                            .setMessage("The amount detected was $" + result)
                            .setPositiveButton("That's right!", (dialog, which) -> activity.submitCharge(result))
                            .setNegativeButton("No, needs correction.", (dialog, which) -> activity.getEnterCorrectAmountDialog());
                    builder.create().show();
                } else {
                    activity.getEnterCorrectAmountDialog();
                }
            }
        }
    }

    private void getEnterCorrectAmountDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        EditText text = new EditText(MainActivity.this);
        text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout ll = new LinearLayout(this);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        ll.setPadding(10, 0, 10, 0);
        ll.addView(text);
        builder
                .setMessage("Enter the correct amount:")
                .setView(ll)
                .setPositiveButton("Submit", (dialog, which) -> submitCharge(text.getText().toString()));
        builder.create().show();
    }

    private void getWasCorrectStoreDialog(String storeName){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage("The store detected was " + storeName)
                .setPositiveButton("That's right!", (dialog, which) -> this.getStoreRatingDialog(storeName))
                .setNegativeButton("No, needs correction.", (dialog, which) -> this.getEnterCorrectStoreNameDialog());
        builder.create().show();
    }

    private void getStoreRatingDialog(String storeName){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageButton[] ibs = new ImageButton[5];
        int[] rating = new int[1];
        for(int i = 0; i<ibs.length; i++){
            ibs[i] = new ImageButton(this);
            ibs[i].setImageResource(R.drawable.blackstar_unselected);
            ibs[i].setBackgroundResource(0);
            final int _i = i;
            ibs[i].setOnClickListener((which) -> {
                for(int j = 0; j<=_i; j++){
                    ibs[j].setImageResource(R.drawable.blackstar_highlighted);
                }
                for(int j = _i+1; j<ibs.length; j++){
                    ibs[j].setImageResource(R.drawable.blackstar_unselected);
                }
                rating[0] = _i+1;
            });
            ll.addView(ibs[i]);
        }

        builder
                .setView(ll)
                .setMessage("If you wish, change the rating for " + storeName)
                .setPositiveButton("Done!", (dialog, which) -> this.setStoreRating(storeName, rating[0]))
                .setNegativeButton("No Thanks.", (dialog, which) -> {});
        builder.create().show();
    }

    private void getEnterCorrectStoreNameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        EditText text = new EditText(MainActivity.this);
        text.setPadding(10, 0, 10, 0);
        text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        builder
                .setMessage("Enter the correct store name:")
                .setView(text)
                .setPositiveButton("Submit", (dialog, which) -> getStoreRatingDialog(text.getText().toString()));
        builder.create().show();
    }

    private void setStoreRating(String storeName, int storeRating){
        gr.addRating(storeName, storeRating, ratingFile);
    }

    private void submitCharge(String result){
        Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        double amount = Double.parseDouble(result);
        currentTotal += amount;
        String newValue = String.format(Locale.US,"$%.2f", currentTotal);
        Storage.add(thisMonthsFile, Date.from(Instant.now()), amount);
        bigNumber.setText(newValue);
        if(this.name != null) getWasCorrectStoreDialog(this.name);
        else this.shouldLaunchPopup = true;
    }

    private static class LocationAPITask extends AsyncTask<Object, Void, String> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;
        private URL url;

        LocationAPITask(MainActivity activity, String query) {
            mActivityWeakReference = new WeakReference<>(activity);
            activity.name = null;

            StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/textsearch/json");
            sb.append("?query="+String.join("+", query.split(" ")));
            sb.append("&key=" + API_KEY);

            try {
                Log.d(TAG, sb.toString());
                url = new URL(sb.toString());
            } catch (MalformedURLException e) {
                Log.e(TAG, "Error processing Places API URL", e);
            }
        }

        @Override
        protected String doInBackground(Object... params) {
            HttpURLConnection conn = null;
            StringBuilder jsonResults = new StringBuilder();

            try {
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
                return jsonResults.toString();
            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            } finally{
                if(conn != null){
                    conn.disconnect();
                }
            }
            return "";
        }

        protected void onPostExecute(String result) {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                if(!result.equals("")) {
                    try {
                        JSONObject obj = new JSONObject(result);
                        String name = obj.getJSONArray("results").getJSONObject(0).getString("name");
                        Log.d(TAG,"Name should be: " + name);
                        activity.name = name;
                        if(activity.shouldLaunchPopup){
                            activity.getWasCorrectStoreDialog(name);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    activity.getEnterCorrectAmountDialog();
                }
            }
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {

        String[] text;
        try{
            text = response.getResponses().get(0).getTextAnnotations().get(0).getDescription().split("[\\s][Tt]otal:?[\\s]+\\$?");
            Log.d(TAG, response.getResponses().get(0).getTextAnnotations().get(0).getDescription());
            StringTokenizer st = new StringTokenizer(response.getResponses().get(0).getTextAnnotations().get(0).getDescription());
            String curr, match = "";
            Pattern r = Pattern.compile("[0-9]+");
            while(st.hasMoreTokens()){
                curr = st.nextToken();
                match += curr;
                // Now create matcher object.
                Matcher m = r.matcher(curr);
                if(m.find() && m.start() == 0){
                    break;
                } else{
                    match += " ";
                }
            }

            while(st.hasMoreTokens()){
                curr = st.nextToken();
                match += " " + curr;

                // Now create matcher object.
                Matcher m = r.matcher(curr);
                if(m.find() && m.regionStart() == 0){
                    break;
                }
            }
            Log.d(TAG, "Address should be: " + match);
            AsyncTask<Object, Void, String> locationAPITask = new LocationAPITask(MainActivity.this, match);
            locationAPITask.execute();
        } catch(Exception e){
            return "";
        }

        if(text.length < 2){
            return "";
        }

        return text[1].substring(0, text[1].indexOf("\n"));
    }
}
