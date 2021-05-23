// The Browser API key obtained from the Google API Console.
// Replace with your own Browser API key, or your own key.
var developerKey = 'AIzaSyCQJUxZQC7HGSuk2nSpU0Oa8XeSU5sQNAc';

// The Client ID obtained from the Google API Console. Replace with your own Client ID.
var clientId = "817792942985-udknrvailik97go5afcfgt6cr92f1asf.apps.googleusercontent.com"

// Replace with your own project number from console.developers.google.com.
// See "Project number" under "IAM & Admin" > "Settings"
var appId = "817792942985";

// Scope to use to access user's Drive items.
var scope = ['https://www.googleapis.com/auth/drive.file'];

var pickerApiLoaded = false;
var oauthToken;
var oauthCallback = createFilePicker;

// Starting points
function loadFilePicker() {
    validateOAuthToken(function () {
        oauthCallback = createFilePicker;
        gapi.load('picker', {'callback': onPickerApiLoad});
    })
}
function loadFolderPicker() {
    validateOAuthToken(function () {
        oauthCallback = createFolderPicker;
        gapi.load('picker', {'callback': onPickerApiLoad});
    })
}

function onAuthApiLoad() {
    window.gapi.auth.authorize(
        {
            'client_id': clientId,
            'scope': scope,
            'immediate': false
        },
        handleAuthResult);
}

function onPickerApiLoad() {
    pickerApiLoaded = true;
    oauthCallback();
}

function handleAuthResult(authResult) {
    if (authResult && !authResult.error) {
        oauthToken = authResult.access_token;
        document.cookie = "authToken="+oauthToken;
        oauthCallback();
    }
}

// Create and render a Picker object for searching images.
function createFilePicker() {
    if (pickerApiLoaded && oauthToken) {
        var view = new google.picker.View(google.picker.ViewId.DOCS);
        view.setMimeTypes("application/json");
        var picker = new google.picker.PickerBuilder()
            .enableFeature(google.picker.Feature.NAV_HIDDEN)
            .setAppId(appId)
            .setOAuthToken(oauthToken)
            .addView(view)
            .addView(new google.picker.DocsUploadView())
            .setDeveloperKey(developerKey)
            .setCallback(filePickerCallback)
            .build();
        picker.setVisible(true);
    }
}

// Create and render a Picker object for searching images.
function createFolderPicker() {
    if (pickerApiLoaded && oauthToken) {
        var view = new google.picker.DocsView(google.picker.ViewId.FOLDER);
        view.setIncludeFolders(true);
        view.setSelectFolderEnabled(true);
        view.setParent('root')
        view.setMimeTypes("application/vnd.google-apps.folder");
        var picker = new google.picker.PickerBuilder()
            .enableFeature(google.picker.Feature.NAV_HIDDEN)
            .enableFeature(google.picker.Feature.MINE_ONLY)
            .setAppId(appId)
            .setOAuthToken(oauthToken)
            .setTitle("Select a folder")
            .addView(view)
            .setDeveloperKey(developerKey)
            .setCallback(folderPickerCallback)
            .build();
        picker.setVisible(true);
    }
}

// A simple callback implementation.
function filePickerCallback(data) {
    if (data.action === google.picker.Action.PICKED) {
        window.location.href = '/pokemon/drive/' + data.docs[0].id;
    }
}

function folderPickerCallback(data) {
    if (data.action === google.picker.Action.PICKED) {
        onFolderPicked(data.docs[0].id);
    }
}

// Check if the token is valid
function validateOAuthToken(callback) {
    if (document.cookie.includes("authToken=")) {
        oauthToken = getCookie("authToken")
        $.get("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + oauthToken, function (result) {
            if (!result['expires_in'] || result['expires_in'] <= 0) {
                oauthCallback = callback
                gapi.load('auth', {'callback': onDownloadAuthApiLoad});
            } else {
                callback()
            }
        })
    } else {
        oauthCallback = callback
        gapi.load('auth', {'callback': onDownloadAuthApiLoad});
    }
}