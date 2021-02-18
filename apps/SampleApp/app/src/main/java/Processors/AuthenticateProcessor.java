//
// Welcome to the annotated FaceTec Device SDK core code for performing secure Authentication!
//
package Processors;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;

import com.facetec.sampleapp.SampleAppActivity;
import com.facetec.sdk.*;

// This is an example self-contained class to perform Authentication with the FaceTec SDK.
// You may choose to further componentize parts of this in your own Apps based on your specific requirements.

// Android Note 1:  Some commented "Parts" below are out of order so that they can match iOS and Browser source for this same file on those platforms.
// Android Note 2:  Android does not have a onFaceTecSDKCompletelyDone function that you must implement like "Part 10" of iOS and Android Samples.  Instead, onActivityResult is used as the place in code you get control back from the FaceTec SDK.
public class AuthenticateProcessor extends Processor implements FaceTecFaceScanProcessor {
    private boolean isSuccess = false;
    private SampleAppActivity sampleAppActivity;

    public AuthenticateProcessor(String sessionToken, Context context) {
        this.sampleAppActivity = (SampleAppActivity) context;

        //
        // Part 1:  Starting the FaceTec Session
        //
        // Required parameters:
        // - Context:  Unique for Android, a Context is passed in, which is required for the final onActivityResult function after the FaceTec SDK is done.
        // - FaceTecFaceScanProcessor:  A class that implements FaceTecFaceScanProcessor, which handles the FaceScan when the User completes a Session.  In this example, "self" implements the class.
        // - sessionToken:  A valid Session Token you just created by calling your API to get a Session Token from the Server SDK.
        //
        FaceTecSessionActivity.createAndLaunchSession(context, AuthenticateProcessor.this, sessionToken);
    }

    //
    // Part 2:  Handling the Result of a FaceScan
    //
    public void processSessionWhileFaceTecSDKWaits(final FaceTecSessionResult sessionResult, final FaceTecFaceScanResultCallback faceScanResultCallback) {
        //
        // DEVELOPER NOTE:  These properties are for demonstration purposes only so the Sample App can get information about what is happening in the processor.
        // In the code in your own App, you can pass around signals, flags, intermediates, and results however you would like.
        //
        sampleAppActivity.setLatestSessionResult(sessionResult);

        //
        // Part 3:  Handles early exit scenarios where there is no FaceScan to handle -- i.e. User Cancellation, Timeouts, etc.
        //
        if(sessionResult.getStatus() != FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY) {
            NetworkingHelpers.cancelPendingRequests();
            faceScanResultCallback.cancel();
            return;
        }

        //
        // Part 4:  Get essential data off the FaceTecSessionResult
        //
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("faceScan", sessionResult.getFaceScanBase64());
            parameters.put("auditTrailImage", sessionResult.getAuditTrailCompressedBase64()[0]);
            parameters.put("lowQualityAuditTrailImage", sessionResult.getLowQualityAuditTrailCompressedBase64()[0]);
            parameters.put("externalDatabaseRefID", sampleAppActivity.getLatestExternalDatabaseRefID());
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to create JSON payload for upload.");
        }

        //
        // Part 5:  Make the Networking Call to Your Servers.  Below is just example code, you are free to customize based on how your own API works.
        //
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(Config.BaseURL + "/match-3d-3d")
                .header("Content-Type", "application/json")
                .header("X-Device-Key", Config.DeviceKeyIdentifier)
                .header("User-Agent", FaceTecSDK.createFaceTecAPIUserAgentString(sessionResult.getSessionId()))

                //
                // Part 7:  Demonstrates updating the Progress Bar based on the progress event.
                //
                .post(new ProgressRequestBody(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), parameters.toString()),
                        new ProgressRequestBody.Listener() {
                            @Override
                            public void onUploadProgressChanged(long bytesWritten, long totalBytes) {
                                final float uploadProgressPercent = ((float)bytesWritten) / ((float)totalBytes);
                                faceScanResultCallback.uploadProgress(uploadProgressPercent);
                            }
                        }))
                .build();

        //
        // Part 8:  Actually send the request.
        //
        NetworkingHelpers.getApiClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                //
                // Part 6:  In our Sample, we evaluate a boolean response and treat true as success, false as "User Needs to Retry",
                // and handle all other non-nominal responses by cancelling out.  You may have different paradigms in your own API and are free to customize based on these.
                //
                String responseString = response.body().string();
                response.body().close();
                try {
                    JSONObject responseJSON = new JSONObject(responseString);

                    //
                    // DEVELOPER NOTE:  These properties are for demonstration purposes only so the Sample App can get information about what is happening in the processor.
                    // In the code in your own App, you can pass around signals, flags, intermediates, and results however you would like.
                    //
                    sampleAppActivity.setLatestServerResult(responseJSON);

                    boolean didSucceed = responseJSON.getBoolean("success");

                    if (didSucceed == true) {
                        // CASE:  Success! Authentication succeeded.

                        //
                        // DEVELOPER NOTE:  These properties are for demonstration purposes only so the Sample App can get information about what is happening in the processor.
                        // In the code in your own App, you can pass around signals, flags, intermediates, and results however you would like.
                        //
                        isSuccess = true;

                        FaceTecCustomization.overrideResultScreenSuccessMessage = "Authenticated";
                        faceScanResultCallback.succeed();
                    }
                    else if (didSucceed == false) {
                        // CASE:  In our Sample code, "success" being present and false means that the User Needs to Retry.
                        // Real Users will likely succeed on subsequent attempts after following on-screen guidance.
                        // Attackers/Fraudsters will continue to get rejected.
                        faceScanResultCallback.retry();
                    }
                    else {
                        // CASE:  UNEXPECTED response from API.  Our Sample Code keys of a success boolean on the root of the JSON object --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                        faceScanResultCallback.cancel();
                    }
                }
                catch(JSONException e) {
                    // CASE:  Parsing the response into JSON failed --> You define your own API contracts with yourself and may choose to do something different here based on the error.  Solid server-side code should ensure you don't get to this case.
                    e.printStackTrace();
                    Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result.");
                    faceScanResultCallback.cancel();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // CASE:  Network Request itself is erroring --> You define your own API contracts with yourself and may choose to do something different here based on the error.
                Log.d("FaceTecSDKSampleApp", "Exception raised while attempting HTTPS call.");
                faceScanResultCallback.cancel();
            }
        });

        //
        // Part 9:  For better UX, update the User if the upload is taking a while.  You are free to customize and enhance this behavior to your liking.
        //
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if(faceScanResultCallback == null) { return; }
                faceScanResultCallback.uploadMessageOverride("Still Uploading...");
            }
        }, 6000);
    }

    public boolean isSuccess() {
        return this.isSuccess;
    }
}
