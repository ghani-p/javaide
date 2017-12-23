package com.duy.project.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.duy.ide.R;
import com.duy.ide.autocomplete.Patterns;
import com.duy.ide.code_sample.model.AssetUtil;
import com.duy.ide.file.FileManager;
import com.duy.project.file.android.AndroidProjectFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Duy on 16-Jul-17.
 */

public class DialogNewAndroidProject extends AppCompatDialogFragment implements View.OnClickListener {
    public static final String TAG = "DialogNewAndroidProject";
    private EditText editAppName, editPackage;
    private Button btnCreate, btnCancel;
    @Nullable
    private DialogNewJavaProject.OnCreateProjectListener listener;
    private EditText activityName, layoutName;

    public static DialogNewAndroidProject newInstance() {

        Bundle args = new Bundle();

        DialogNewAndroidProject fragment = new DialogNewAndroidProject();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (DialogNewJavaProject.OnCreateProjectListener) getActivity();
        } catch (Exception e) {
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_new_android_project, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        editPackage = view.findViewById(R.id.edit_package_name);
        editAppName = view.findViewById(R.id.edit_project_name);
        btnCreate = view.findViewById(R.id.btn_create);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnCreate.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        activityName = view.findViewById(R.id.edit_activity_name);
        layoutName = view.findViewById(R.id.edit_layout_name);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                dismiss();
                break;
            case R.id.btn_create:
                doCreateProject();
                break;
        }
    }

    private void doCreateProject() {
        if (isOk()) {

            ///create new android project
            String packageName = editPackage.getText().toString();
            String activityName = this.activityName.getText().toString();
            String activityClass = String.format("%s.%s", packageName, activityName);
            String mainLayoutName = layoutName.getText().toString();
            String appName = editAppName.getText().toString();
            String classpath = FileManager.getClasspathFile(getContext()).getPath();
            String projectName = appName.replaceAll("\\s+", "");

            try {
                AndroidProjectFolder projectFile = new AndroidProjectFolder(
                        new File(FileManager.EXTERNAL_DIR), activityClass, packageName, projectName);
                //create directory
                projectFile.mkdirs();

                AssetManager assets = getContext().getAssets();

                copyAsset(projectFile, assets);
                createStringXml(projectFile, appName);
                copyKeyStore(projectFile, assets);
                createManifest(projectFile, activityClass, packageName, assets);
                createMainActivity(projectFile, activityClass, packageName, activityName, appName, assets);
                createMainXml(projectFile, mainLayoutName, assets);
                copyLibrary(projectFile, assets);
                if (listener != null) listener.onProjectCreated(projectFile);
                this.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Can not create project. Error " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyAsset(AndroidProjectFolder projectFile, AssetManager assets) {
        String resourcePath = projectFile.getDirRes().getPath();
        AssetUtil.copyAssetFolder(assets, "templates/src/main/res", resourcePath);
    }

    private void copyLibrary(AndroidProjectFolder projectFile, AssetManager assets) {
        //copy android support library
        AssetUtil.copyAssetFolder(assets, "templates/libs", projectFile.dirLibs.getPath());
    }

    private void copyKeyStore(AndroidProjectFolder projectFile, AssetManager assets) throws IOException {

        //copy keystore
        File file = projectFile.getKeyStore().getFile();
        FileOutputStream out = new FileOutputStream(file);
        FileManager.copyStream(assets.open("templates/src/main/androiddebug.jks"), out);
        out.close();

    }

    private void createStringXml(AndroidProjectFolder projectFile, String appName) throws Exception {
        File stringxml = new File(projectFile.getDirRes(), "values/strings.xml");
        String strings = FileManager.streamToString(new FileInputStream(
                stringxml)).toString();
        strings = strings.replace("{APP_NAME}", appName);
        strings = strings.replace("{MAIN_ACTIVITY_NAME}", appName);
        Log.d(TAG, "doCreateProject strings = " + strings);
        FileManager.saveFile(stringxml, strings);

    }

    private void createManifest(AndroidProjectFolder projectFile, String activityClass, String packageName,
                                AssetManager assets) throws IOException {
        File manifest = projectFile.getXmlManifest();
        InputStream manifestTemplate = assets.open("templates/src/main/AndroidManifest.xml");
        String contentManifest = FileManager.streamToString(manifestTemplate).toString();
        contentManifest = contentManifest.replace("{PACKAGE}", packageName);
        contentManifest = contentManifest.replace("{MAIN_ACTIVITY}", activityClass);
        Log.d(TAG, "doCreateProject contentManifest = " + contentManifest);
        FileManager.saveFile(manifest, contentManifest);
    }

    private void createMainActivity(AndroidProjectFolder projectFile, String activityClass,
                                    String packageName, String activityName, String appName,
                                    AssetManager assets) throws IOException {
        File activityFile = FileManager.createFileIfNeed(new File(projectFile.dirJava,
                activityClass.replace(".", File.separator) + ".java"));
        InputStream activityTemplate = assets.open("templates/src/main/MainActivity.java");
        String contentClass = FileManager.streamToString(activityTemplate).toString();
        contentClass = contentClass.replace("{PACKAGE}", packageName);
        contentClass = contentClass.replace("{APP_NAME}", appName);
        contentClass = contentClass.replace("{ACTIVITY_NAME}", activityName);
        Log.d(TAG, "doCreateProject contentManifest = " + contentClass);
        FileManager.saveFile(activityFile, contentClass);
    }

    private void createMainXml(AndroidProjectFolder projectFile, String mainLayoutName, AssetManager assets) throws IOException {
        if (!mainLayoutName.contains(".")) mainLayoutName += ".xml";
        File layoutMain = new File(projectFile.getDirLayout(), mainLayoutName);
        layoutMain.createNewFile();
        InputStream layoutTemplate = assets.open("templates/src/main/activity_main.xml");
        String contentLayout = FileManager.streamToString(layoutTemplate).toString();
        FileManager.saveFile(layoutMain, contentLayout);
    }

    /**
     * check input data
     *
     * @return true if all is ok
     */
    private boolean isOk() {
        //check app name
        if (editAppName.getText().toString().isEmpty()) {
            editAppName.setError(getString(R.string.enter_name));
            return false;
        }
        String packageName = editPackage.getText().toString();
        if (packageName.isEmpty()) {
            editPackage.setError(getString(R.string.enter_package));
            return false;
        }
        if (!packageName.contains(".")) {
            editPackage.setError("Invalid package name: The package name must be least one '.' separator");
            return false;
        }
        if (!Patterns.PACKAGE_NAME.matcher(packageName).find()) {
            editPackage.setError("Invalid package name");
            return false;
        }

        //check activity name
        String activityName = this.activityName.getText().toString();
        if (activityName.isEmpty()) {
            this.activityName.setError(getString(R.string.enter_name));
            return false;
        }
        if (!Patterns.RE_IDENTIFIER.matcher(activityName).find()) {
            this.activityName.setText("Invalid name");
            return false;
        }

        //check layout name
        if (layoutName.getText().toString().isEmpty()) {
            layoutName.setError(getString(R.string.enter_name));
            return false;
        }
        if (!Patterns.RE_IDENTIFIER.matcher(layoutName.getText().toString()).find()) {
            layoutName.setText("Invalid name");
            return false;
        }

        return true;
    }

}
