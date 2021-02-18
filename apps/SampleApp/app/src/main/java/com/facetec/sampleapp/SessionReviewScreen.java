package com.facetec.sampleapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.facetec.sdk.FaceTecIDScanResult;
import com.facetec.sdk.FaceTecIDScanStatus;
import com.facetec.sdk.FaceTecSDK;
import com.facetec.sdk.FaceTecSessionResult;
import com.facetec.sdk.FaceTecSessionStatus;

import org.json.JSONException;
import org.json.JSONObject;

import Processors.AuthenticateProcessor;
import Processors.EnrollmentProcessor;
import Processors.LivenessCheckProcessor;
import Processors.PhotoIDMatchProcessor;
import Processors.Processor;

public class SessionReviewScreen {

    Processor latestProcessor;
    byte[] latestFaceScan;
    byte[] latestIDScan;
    String latestSessionId;
    FaceTecSessionStatus latestSessionStatus;
    FaceTecIDScanStatus latestIDScanStatus;
    Integer latest3dAgeEstimateGroup;
    Integer latestMatchLevel;
    Integer latestDigitalSpoofStatus;
    Integer latestFullIDStatus;
    String[] latestAuditTrailImages;
    String[] latestLowQualityAuditTrailImages;
    String[] latestIDScanFrontImages;
    JSONObject latestFaceScanSecurityChecks;

    boolean isLoaded = false;

    private SampleAppActivity sampleAppActivity;

    public SessionReviewScreen(SampleAppActivity activity) {
        sampleAppActivity = activity;
    }

    void initLayout() {
        float screenWidth = sampleAppActivity.getResources().getConfiguration().screenWidthDp;
        if(screenWidth < 400f) {
            // Scale down content for smaller screens
            float scaleFactor = (400f - (400f - screenWidth)) / 400f;

            sampleAppActivity.activityMainBinding.srsCancelButton.getLayoutParams().height *= scaleFactor;
            sampleAppActivity.activityMainBinding.srsCancelButton.getLayoutParams().width *= scaleFactor;

            sampleAppActivity.activityMainBinding.srsHeaderTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f * scaleFactor);
        }
    }

    public void setLatestSessionResult(FaceTecSessionResult sessionResult) {
        if(sessionResult.getSessionId() != null) {
            latestSessionId = sessionResult.getSessionId();
        }
        if(sessionResult.getStatus() != null) {
            latestSessionStatus = sessionResult.getStatus();
        }
        if(sessionResult.getFaceScan() != null) {
            latestFaceScan = sessionResult.getFaceScan();
        }
        if(sessionResult.getAuditTrailCompressedBase64().length > 0) {
            latestAuditTrailImages = sessionResult.getAuditTrailCompressedBase64();
        }
        if(sessionResult.getLowQualityAuditTrailCompressedBase64().length > 0) {
            latestLowQualityAuditTrailImages = sessionResult.getLowQualityAuditTrailCompressedBase64();
        }
    }

    public void setLatestIDScanResult(FaceTecIDScanResult idScanResult) {
        if(idScanResult.getSessionId() != null && idScanResult.getSessionId().length() > 0) {
            latestSessionId = idScanResult.getSessionId();
        }
        if(idScanResult.getStatus() != null) {
            latestIDScanStatus = idScanResult.getStatus();
        }
        if(idScanResult.getIDScan() != null) {
            latestIDScan = idScanResult.getIDScan();
        }
        if(!idScanResult.getFrontImagesCompressedBase64().isEmpty()) {
            latestIDScanFrontImages = idScanResult.getFrontImagesCompressedBase64().toArray(new String[0]);
        }
    }

    public void setLatestServerResult(JSONObject responseJSON) {
        try {
            if(responseJSON.has("faceScanSecurityChecks")) {
                latestFaceScanSecurityChecks = responseJSON.getJSONObject("faceScanSecurityChecks");
            }
            if(responseJSON.has("matchLevel")) {
                latestMatchLevel = responseJSON.getInt("matchLevel");
            }
            if(responseJSON.has("ageEstimateGroupEnumInt")) {
                latest3dAgeEstimateGroup = responseJSON.getInt("ageEstimateGroupEnumInt");
            }
            if(responseJSON.has("digitalIDSpoofStatusEnumInt")) {
                latestDigitalSpoofStatus = responseJSON.getInt("digitalIDSpoofStatusEnumInt");
            }
            if(responseJSON.has("fullIDStatusEnumInt")) {
                latestFullIDStatus = responseJSON.getInt("fullIDStatusEnumInt");
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result for Session Review Screen.");
        }
    }

    public void reset() {
        latestProcessor = null;
        latestFaceScan = null;
        latestIDScan = null;
        latestSessionId = null;
        latestSessionStatus = null;
        latestIDScanStatus = null;
        latest3dAgeEstimateGroup = null;
        latestMatchLevel = null;
        latestDigitalSpoofStatus = null;
        latestFullIDStatus = null;
        latestAuditTrailImages = null;
        latestLowQualityAuditTrailImages = null;
        latestIDScanFrontImages = null;
        latestFaceScanSecurityChecks = null;
    }

    public void load(Processor processor, Runnable callback) {
        isLoaded = true;
        latestProcessor = processor;

        configureButtons();

        updateMode();

        updateSessionStatus();

        updateLivenessResult();

        updateMatchLevel();

        updateAgeEstimate();

        updateDigitalSpoofStatus();

        updateFullIDStatus();

        updateAuditTrailImages();

        updateIDScanContainer();

        updateSessionDataSizes();

        updateDebugLog();

        callback.run();
    }

    void setHTMLTextForTextView(TextView textView, String htmlString) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(htmlString, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(htmlString));
        }
    }

    void setOnClickListenerForViewWithLinkedSite(View view, final String linkedSite) {
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(linkedSite));
                sampleAppActivity.startActivity(browserIntent);
            }
        });
    }

    String getDashboardMailingLink() {
        String subject= "FaceTec Dashboard Demo";
        String body="Hello,<br><br>Click to access your <a href='https://dev.facetec.com/#/server-dashboard-demo'>FaceTec Developer Dashboard</a> and view your Session Results. Please use the same email address in your Developer Account as in your Demo Apps in order to view your Sessions.<br><br>Link URL: https://dev.facetec.com/#/server-dashboard-demo<br><br>";
        String mailToLink = "mailto:" +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body);
        return mailToLink;
    }

    void configureButtons() {
        // Setup info links
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsModeInfoLink, "https://dev.facetec.com/#/docs");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsSessionStatusInfoLink, "https://dev.facetec.com/#/docs");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsLivenessResultInfoLink, "https://dev.facetec.com/#/bias-and-success-rates");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsMatchLevelInfoLink, "https://dev.facetec.com/#/matching-guide");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsAgeEstimateInfoLink, "https://dev.facetec.com/#/age-check-guide");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsDigitalSpoofStatusInfoLink, "https://dev.facetec.com/#/photo-id-anti-spoofing-and-anti-tampering");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsFullIDStatusInfoLink, "https://dev.facetec.com/#/photo-id-anti-spoofing-and-anti-tampering");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsDigitalSpoofStatusInfoLink, "https://dev.facetec.com/#/photo-id-anti-spoofing-and-anti-tampering");
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsAuditTrailInfoLink, "https://dev.facetec.com/#/data-types");

        // Setup control buttons
        setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsViewDashboardUsageButton, "https://dev.facetec.com/#/server-dashboard-demo");
        sampleAppActivity.activityMainBinding.srsEmailDashboardLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_VIEW);
                emailIntent.setData(Uri.parse(getDashboardMailingLink()));
                sampleAppActivity.startActivity(Intent.createChooser(emailIntent, "Send Email Using..."));
            }
        });

        // Setup back buttons
        sampleAppActivity.activityMainBinding.srsCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
        sampleAppActivity.activityMainBinding.srsBackToMainMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
    }

    String getModeFromProcessor(Processor processor) {
        String mode = "";
        if(processor instanceof LivenessCheckProcessor) {
            mode = "3D Liveness Check";
        }
        else if(processor instanceof EnrollmentProcessor) {
            mode = "New User Onboarding";
        }
        else if(processor instanceof AuthenticateProcessor) {
            mode = "3D Liveness Check + 3D Matching";
        }
        else if(processor instanceof PhotoIDMatchProcessor) {
            mode = "3D Liveness Check + Photo ID Match";
        }
        return mode;
    }

    String htmlBold(String string) {
        return "<b>" + string + "</b>";
    }

    void updateMode() {
        String modeDescription = getModeFromProcessor(latestProcessor);

        String modeText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_mode_title), modeDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsModeTextView, modeText);
    }

    void updateSessionStatus() {
        String sessionStatusDescription = "";

        if(latestSessionStatus != null) {
            if(latestProcessor != null && latestProcessor.isSuccess() && latestSessionStatus == FaceTecSessionStatus.SESSION_COMPLETED_SUCCESSFULLY && !(latestProcessor instanceof PhotoIDMatchProcessor)) {
                sessionStatusDescription = "Completed";
            }
            else if(latestSessionStatus == FaceTecSessionStatus.USER_CANCELLED || latestSessionStatus == FaceTecSessionStatus.USER_CANCELLED_VIA_HARDWARE_BUTTON) {
                if(latestAuditTrailImages != null && latestAuditTrailImages.length > 0) {
                    // handle cancelling during retry
                    if(latestProcessor instanceof PhotoIDMatchProcessor) {
                        sessionStatusDescription = "ID Scan Not Started";
                    }
                    else {
                        sessionStatusDescription = "Retry Needed to Complete";
                    }
                }
                else {
                    sessionStatusDescription = "Incomplete (Cancelled)";
                }
            }
            else if(latestSessionStatus == FaceTecSessionStatus.TIMEOUT) {
                sessionStatusDescription = "Incomplete (Timed Out)";
            }
            else {
                sessionStatusDescription = "Incomplete";
            }
        }
        else {
            sessionStatusDescription = "Unavailable";
        }
        // handle session status for Photo ID Scan
        if(latestProcessor instanceof PhotoIDMatchProcessor) {
            if(latestIDScanStatus != null) {
                if(latestProcessor.isSuccess() && latestIDScanStatus == FaceTecIDScanStatus.SUCCESS) {
                    sessionStatusDescription = "Completed";
                }
                else if(latestIDScanFrontImages != null && latestIDScanFrontImages.length > 0) {
                    sessionStatusDescription = "Retry Needed to Complete";
                }
                else {
                    sessionStatusDescription = "ID Scan Not Completed";
                }
            }
        }

        String sessionStatusText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_session_status_title), sessionStatusDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsSessionStatusTextView, sessionStatusText);
    }

    boolean didLivenessPass() {
        boolean livenessPassed = false;
        try {
            if(latestFaceScanSecurityChecks != null && latestFaceScanSecurityChecks.has("faceScanLivenessCheckSucceeded")) {
                livenessPassed = latestFaceScanSecurityChecks.getBoolean("faceScanLivenessCheckSucceeded");
            }
        }
        catch(JSONException e) {
            e.printStackTrace();
            Log.d("FaceTecSDKSampleApp", "Exception raised while attempting to parse JSON result for Session Review Screen.");
        }
        return livenessPassed;
    }

    void updateLivenessResult() {
        String livenessResultDescription = "";
        if(didLivenessPass()) {
            livenessResultDescription = "Liveness Passed";
        }
        else {
            livenessResultDescription = "Liveness Not Proven";
        }

        String livenessResultText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_liveness_result_title), livenessResultDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsLivenessResultTextView, livenessResultText);
    }

    String getFriendlyDescriptionForMatchLevel(Integer matchLevel) {
        String matchLevelDescription = "Unavailable";
        if(matchLevel != null) {
            if(matchLevel == 0) {
                if(didLivenessPass()) {
                    if(latestProcessor != null && latestProcessor instanceof PhotoIDMatchProcessor) {
                        if(latestIDScanFrontImages != null && latestIDScanFrontImages.length > 0) {
                            matchLevelDescription = "Unconfirmed";
                        }
                        else {
                            matchLevelDescription = "Not Ran (Must Complete ID Scan)";
                        }
                    }
                    else {
                        matchLevelDescription = "Non-match (Level 0)";
                    }
                }
                else {
                    matchLevelDescription = "Not Ran (Must Prove Liveness)";
                }
            }
            else if(matchLevel == 1) {
                matchLevelDescription = "99.0% (Level 1)"; // 1/100 FAR
            }
            else if(matchLevel == 2) {
                matchLevelDescription = "99.996% (Level 2)"; // 1/250 FAR
            }
            else if(matchLevel == 3) {
                matchLevelDescription = "99.998% (Level 3)"; // 1/500 FAR
            }
            else if(matchLevel == 4) {
                matchLevelDescription = "99.999% (Level 4)"; // 1/1,000 FAR
            }
            else if(matchLevel == 5) {
                matchLevelDescription = "99.9999% (Level 5)"; // 1/10,000 FAR
            }
            else if(matchLevel == 6) {
                matchLevelDescription = "99.99999% (Level 6)"; // 1/100,000 FAR
            }
            else if(matchLevel == 7) {
                matchLevelDescription = "99.999998% (Level 7)"; // 1/500,000 FAR
            }
            else if(matchLevel == 8) {
                matchLevelDescription = "99.999999% (Level 8)"; // 1/1,000,000 FAR
            }
            else if(matchLevel == 9) {
                matchLevelDescription = "99.9999995% (Level 9)"; // 1/2,000,000 FAR
            }
            else if(matchLevel == 10) {
                matchLevelDescription = "99.9999997619048% (Level 10)"; // 1/4,200,000 FAR
            }
        }
        return matchLevelDescription;
    }

    void updateMatchLevel() {
        if(latestProcessor instanceof AuthenticateProcessor || latestProcessor instanceof PhotoIDMatchProcessor) {
            sampleAppActivity.activityMainBinding.srsMatchLevelContainer.setVisibility(View.VISIBLE);
        }
        else {
            sampleAppActivity.activityMainBinding.srsMatchLevelContainer.setVisibility(View.GONE);
        }

        String matchLevelDescription = getFriendlyDescriptionForMatchLevel(latestMatchLevel);

        String matchLevelTitle = "";
        if(latestProcessor instanceof AuthenticateProcessor) {
            matchLevelTitle = sampleAppActivity.getString(R.string.srs_3d_match_level_title);
            // Update info link
            setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsMatchLevelInfoLink, "https://dev.facetec.com/#/matching-guide");
        }
        else if(latestProcessor instanceof PhotoIDMatchProcessor) {
            matchLevelTitle = sampleAppActivity.getString(R.string.srs_2d_match_level_title);
            // Update info link
            setOnClickListenerForViewWithLinkedSite(sampleAppActivity.activityMainBinding.srsMatchLevelInfoLink, "https://dev.facetec.com/#/photo-id-match-guide");
        }

        String matchLevelText = createBoldHTMLTextWithTitle(matchLevelTitle, matchLevelDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsMatchLevelTextView, matchLevelText);
    }

    String getFriendlyDescriptionForAgeEstimateGroup(Integer ageEstimateGroup) {
        String ageEstimateDescription = "Unavailable";
        if(ageEstimateGroup != null) {
            if(ageEstimateGroup == 0) {
                ageEstimateDescription = "Under 13 Years";
            }
            else if(ageEstimateGroup == 1) {
                ageEstimateDescription = "Over 13 Years";
            }
            else if(ageEstimateGroup == 2) {
                ageEstimateDescription = "Over 18 Years";
            }
            else if(ageEstimateGroup == 3) {
                ageEstimateDescription = "Over 25 Years";
            }
            else if(ageEstimateGroup == 4) {
                ageEstimateDescription = "Over 30 Years";
            }
            else if(ageEstimateGroup == 5) {
                ageEstimateDescription = "Over 22 Years";
            }
        }
        return ageEstimateDescription;
    }

    void updateAgeEstimate() {
        String ageEstimate3dDescription = getFriendlyDescriptionForAgeEstimateGroup(latest3dAgeEstimateGroup);

        String ageEstimate3dText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_3d_age_estimate_title), ageEstimate3dDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srs3dAgeEstimateTextView, ageEstimate3dText);
    }

    String getFriendlyDescriptionForDigitalSpoofStatus(Integer digitalSpoofStatus) {
        String spoofStatusDescription = "Unavailable";
        if(latestIDScanFrontImages != null && latestIDScanFrontImages.length > 0) {
            spoofStatusDescription = "Unconfirmed";
            if(latestProcessor != null && latestProcessor.isSuccess() && digitalSpoofStatus != null && digitalSpoofStatus == 0) {
                spoofStatusDescription = "Passed";
            }
        }
        return spoofStatusDescription;
    }

    void updateDigitalSpoofStatus() {
        if(latestProcessor instanceof PhotoIDMatchProcessor) {
            sampleAppActivity.activityMainBinding.srsDigitalSpoofStatusContainer.setVisibility(View.VISIBLE);
        }
        else {
            sampleAppActivity.activityMainBinding.srsDigitalSpoofStatusContainer.setVisibility(View.GONE);
        }

        String spoofStatusDescription = getFriendlyDescriptionForDigitalSpoofStatus(latestDigitalSpoofStatus);

        String spoofStatusText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_digital_spoof_status_title), spoofStatusDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsDigitalSpoofStatusTextView, spoofStatusText);
    }

    String getFriendlyDescriptionForFullIDStatus(Integer fullIDStatus) {
        String fullIDStatusDescription = "Unavailable";
        if(latestIDScanFrontImages != null && latestIDScanFrontImages.length > 0) {
            fullIDStatusDescription = "Unconfirmed";
            if(latestProcessor != null && latestProcessor.isSuccess() && fullIDStatus != null && fullIDStatus == 0) {
                fullIDStatusDescription = "Yes";
            }
        }
        return fullIDStatusDescription;
    }

    void updateFullIDStatus() {
        if(latestProcessor instanceof PhotoIDMatchProcessor) {
            sampleAppActivity.activityMainBinding.srsFullIDStatusContainer.setVisibility(View.VISIBLE);
        }
        else {
            sampleAppActivity.activityMainBinding.srsFullIDStatusContainer.setVisibility(View.GONE);
        }

        String fullIDStatusDescription = getFriendlyDescriptionForFullIDStatus(latestFullIDStatus);

        String fullIDStatusText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_full_id_status_title), fullIDStatusDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsFullIDStatusTextView, fullIDStatusText);
    }

    void updateIDScanContainer() {
        String idScanDescription = "";
        if(latestProcessor instanceof PhotoIDMatchProcessor) {
            if(latestIDScanFrontImages != null && latestIDScanFrontImages.length > 0) {
                // Set id scan image
                byte[] decodedString = Base64.decode(latestIDScanFrontImages[0], Base64.DEFAULT);
                Bitmap frontImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                sampleAppActivity.activityMainBinding.srsIDScanImage.setImageBitmap(frontImage);

                sampleAppActivity.activityMainBinding.srsIDScanImage.setVisibility(View.VISIBLE);
            }
            else {
                idScanDescription = htmlBold("Unavailable");

                sampleAppActivity.activityMainBinding.srsIDScanImage.setVisibility(View.GONE);
            }

            sampleAppActivity.activityMainBinding.srsIDScanContainer.setVisibility(View.VISIBLE);
        }
        else {
            sampleAppActivity.activityMainBinding.srsIDScanContainer.setVisibility(View.GONE);
        }

        String idScanText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_idscan_title), idScanDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsIDScanTextView, idScanText);
    }

    void updateDebugLog() {
        if(latestProcessor instanceof LivenessCheckProcessor) {
            sampleAppActivity.activityMainBinding.srsDebugEnrollmentIDTextView.setVisibility(View.GONE);
        }
        else {
            sampleAppActivity.activityMainBinding.srsDebugEnrollmentIDTextView.setVisibility(View.VISIBLE);
        }

        String enrollmentID = sampleAppActivity.latestExternalDatabaseRefID != null ? sampleAppActivity.latestExternalDatabaseRefID : "";

        String debugEnrollmentIDText = sampleAppActivity.getString(R.string.srs_debug_enrollment_id_title) + "<br>" + enrollmentID;
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsDebugEnrollmentIDTextView, debugEnrollmentIDText);

        String sessionId = latestSessionId != null ? latestSessionId : "";

        String debugSessionIDText = sampleAppActivity.getString(R.string.srs_debug_session_id_title) + "<br>" + sessionId;
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsDebugSessionIDTextView, debugSessionIDText);
    }

    void updateAuditTrailImages() {
        String auditTrailDescription = "Unavailable";
        if(latestAuditTrailImages != null && latestAuditTrailImages.length > 0) {
            auditTrailDescription = "";
            byte[] decodedString = Base64.decode(latestAuditTrailImages[0], Base64.DEFAULT);
            if(!latestProcessor.isSuccess() && latestLowQualityAuditTrailImages != null && latestLowQualityAuditTrailImages.length > 0) {
                decodedString = Base64.decode(latestLowQualityAuditTrailImages[0], Base64.DEFAULT);
            }
            Bitmap auditImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            sampleAppActivity.activityMainBinding.srsAuditTrailImage.setImageBitmap(auditImage);

            sampleAppActivity.activityMainBinding.srsAuditTrailImage.setVisibility(View.VISIBLE);
        }
        else {
            sampleAppActivity.activityMainBinding.srsAuditTrailImage.setVisibility(View.GONE);
        }

        String auditTrailText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_audit_trail_title), auditTrailDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsAuditTrailTextView, auditTrailText);
    }

    String getSizeInKb(byte[] data) {
        return ((int) Math.ceil((double)data.length/1000.0)) + "kb";
    }

    void updateSessionDataSizes() {
        String faceScanSizeDescription = "Unavailable";
        String idScanSizeDescription = "Unavailable";

        if(latestFaceScan != null) {
            faceScanSizeDescription = getSizeInKb(latestFaceScan);
        }

        if(latestProcessor instanceof PhotoIDMatchProcessor) {
            sampleAppActivity.activityMainBinding.srsIDScanSizeTextView.setVisibility(View.VISIBLE);

            if(latestIDScan != null) {
                idScanSizeDescription = getSizeInKb(latestIDScan);
            }
        }
        else {
            sampleAppActivity.activityMainBinding.srsIDScanSizeTextView.setVisibility(View.GONE);
        }

        String faceScanSizeText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_facescan_size_title), faceScanSizeDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsFaceScanSizeTextView, faceScanSizeText);

        String idScanSizeText = createBoldHTMLTextWithTitle(sampleAppActivity.getString(R.string.srs_idscan_size_title), idScanSizeDescription);
        setHTMLTextForTextView(sampleAppActivity.activityMainBinding.srsIDScanSizeTextView, idScanSizeText);
    }

    void fadeIn(final Runnable callback) {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.sessionResultViewLayout.setVisibility(View.VISIBLE);
                sampleAppActivity.activityMainBinding.sessionResultViewLayout.animate().alpha(1).setDuration(300).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if(callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        });
    }

    void fadeOut(final Runnable callback) {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.sessionResultViewLayout.animate().alpha(0).setDuration(300).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        sampleAppActivity.activityMainBinding.sessionResultViewLayout.setVisibility(View.GONE);
                        if(callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        });
    }

    void hide() {
        fadeOut(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.contentLayout.animate().alpha(1f).setDuration(300).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        sampleAppActivity.utils.enableAllButtons();
                    }
                });
            }
        });
    }

    void show() {
        if(!isLoaded) {
            sampleAppActivity.utils.displayStatus("Must complete a session first before viewing its results.");
            return;
        }

        sampleAppActivity.utils.disableAllButtons();
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.contentLayout.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        fadeIn(null);
                    }
                });
            }
        });
    }

    void loadAndShow(final Processor processor, final Runnable callback) {
        if(FaceTecSDK.getCameraPermissionStatus(sampleAppActivity) != FaceTecSDK.CameraPermissionStatus.GRANTED) {
            sampleAppActivity.utils.fadeInMainUI();
            if(callback != null) {
                callback.run();
            }
            return;
        }
        if(FaceTecSDK.isLockedOut(sampleAppActivity) && isLoaded) {
            sampleAppActivity.utils.displayStatus("This device is currently locked out due to too many unsuccessful Session attempts. Please try again in a few minutes, or try on another device.");
            sampleAppActivity.utils.fadeInMainUI();
            if(callback != null) {
                callback.run();
            }
            return;
        }
        
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.themeTransitionImageView.animate().alpha(0f).setDuration(300);
                load(processor, new Runnable() {
                    @Override
                    public void run() {
                        fadeIn(new Runnable() {
                            @Override
                            public void run() {
                                if(callback != null) {
                                    callback.run();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    String createBoldHTMLTextWithTitle(String titleText, String valueText) {
        String htmlText = titleText;
        if(!valueText.isEmpty()) {
            htmlText += " " + htmlBold(valueText);
        }
        return htmlText;
    }
}
