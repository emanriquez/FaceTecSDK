package com.facetec.sampleapp;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;

import Processors.Config;
import Processors.ThemeHelpers;

public class SampleAppUtilities {

    private SampleAppActivity sampleAppActivity;
    private String currentTheme = "FaceTec Theme";
    private Handler themeTransitionTextHandler;

    public SampleAppUtilities(SampleAppActivity activity) {
        sampleAppActivity = activity;
    }

    public void disableAllButtons() {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.enrollButton.setEnabled(false);
                sampleAppActivity.activityMainBinding.authButton.setEnabled(false);
                sampleAppActivity.activityMainBinding.livenessCheckButton.setEnabled(false);
                sampleAppActivity.activityMainBinding.identityCheckButton.setEnabled(false);
                sampleAppActivity.activityMainBinding.sessionReviewButton.setEnabled(false);
                sampleAppActivity.activityMainBinding.settingsButton.setEnabled(false);
            }
        });
    }

    public void enableAllButtons() {
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.enrollButton.setEnabled(true);
                sampleAppActivity.activityMainBinding.authButton.setEnabled(true);
                sampleAppActivity.activityMainBinding.livenessCheckButton.setEnabled(true);
                sampleAppActivity.activityMainBinding.identityCheckButton.setEnabled(true);
                sampleAppActivity.activityMainBinding.sessionReviewButton.setEnabled(true);
                sampleAppActivity.activityMainBinding.settingsButton.setEnabled(true);
            }
        });
    }

    public void showSessionTokenConnectionText() {
        themeTransitionTextHandler = new Handler();
        themeTransitionTextHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.themeTransitionText.animate().alpha(1f).setDuration(600);
            }
        }, 3000);
    }

    public void hideSessionTokenConnectionText() {
        themeTransitionTextHandler.removeCallbacksAndMessages(null);
        themeTransitionTextHandler = null;
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.themeTransitionText.animate().alpha(0f).setDuration(600);
            }
        });
    }

    // Disable buttons to prevent hammering, fade out main interface elements, and shuffle the guidance images.
    public void fadeOutMainUIAndPrepareForFaceTecSDK(final Runnable callback) {
        disableAllButtons();
        sampleAppActivity.sessionReviewScreen.reset();
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.themeTransitionImageView.animate().alpha(1f).setDuration(600).start();
                sampleAppActivity.activityMainBinding.contentLayout.animate().alpha(0f).setDuration(600).withEndAction(callback).start();
            }
        });
    }

    public void fadeInMainUI() {
        enableAllButtons();
        sampleAppActivity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  sampleAppActivity.activityMainBinding.contentLayout.animate().alpha(1f).setDuration(600);
                  sampleAppActivity.activityMainBinding.themeTransitionImageView.animate().alpha(0f).setDuration(600);
              }
            }
        );
    }

    public void displayStatus(final String statusString) {
        Log.d("FaceTecSDKSampleApp", statusString);
        sampleAppActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sampleAppActivity.activityMainBinding.statusLabel.setText(statusString);
            }
        });
    }

    public void handleErrorGettingServerSessionToken() {
        hideSessionTokenConnectionText();
        displayStatus("Session could not be started due to an unexpected issue during the network request.");
        fadeInMainUI();
    }

    public void showThemeSelectionMenu() {

        final String[] themes;
        if(Config.wasSDKConfiguredWithConfigWizard == true) {
            themes = new String[] { "Config Wizard Theme", "FaceTec Theme", "Pseudo-Fullscreen", "Well-Rounded", "Bitcoin Exchange", "eKYC", "Sample Bank"};
        }
        else {
            themes = new String[] {"FaceTec Theme", "Pseudo-Fullscreen", "Well-Rounded", "Bitcoin Exchange", "eKYC", "Sample Bank"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(sampleAppActivity, android.R.style.Theme_Holo_Light));
        builder.setTitle("Select a Theme:");
        builder.setItems(themes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                currentTheme = themes[index];
                ThemeHelpers.setAppTheme(currentTheme);
                updateThemeTransitionView();
            }
        });
        builder.show();
    }

    public void updateThemeTransitionView() {
        int transitionViewImage = 0;
        int transitionViewTextColor = Config.currentCustomization.getGuidanceCustomization().foregroundColor;
        switch (currentTheme) {
            case "FaceTec Theme":
                break;
            case "Config Wizard Theme":
                break;
            case "Pseudo-Fullscreen":
                break;
            case "Well-Rounded":
                transitionViewImage = R.drawable.well_rounded_bg;
                transitionViewTextColor = Config.currentCustomization.getFrameCustomization().backgroundColor;
                break;
            case "Bitcoin Exchange":
                transitionViewImage = R.drawable.bitcoin_exchange_bg;
                transitionViewTextColor = Config.currentCustomization.getFrameCustomization().backgroundColor;
                break;
            case "eKYC":
                transitionViewImage = R.drawable.ekyc_bg;
                break;
            case "Sample Bank":
                transitionViewImage = R.drawable.sample_bank_bg;
                transitionViewTextColor = Config.currentCustomization.getFrameCustomization().backgroundColor;
                break;
            default:
                break;
        }

        sampleAppActivity.activityMainBinding.themeTransitionImageView.setImageResource(transitionViewImage);
        sampleAppActivity.activityMainBinding.themeTransitionText.setTextColor(transitionViewTextColor);
    }

}
