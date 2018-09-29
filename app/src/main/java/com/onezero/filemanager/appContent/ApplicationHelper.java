package com.onezero.filemanager.appContent;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;

import com.onezero.filemanager.AppInfo;
import com.onezero.filemanager.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ApplicationHelper {
    public static final String TAG = "ApplicationHelper";
    private static boolean result = false;
    /**
     * 外部应用安装器安装apk（原生接口）
     * @param context
     * @param path apk的路径
     * @return
     */
    public static boolean installApkByPath(Context context, String path) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean isSystemApp(PackageInfo pInfo) {
        //判断是否是系统软件
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static boolean isSystemUpdateApp(PackageInfo pInfo) {
        //判断是否是软件更新..
        return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
    }

    public static boolean isUserApp(PackageInfo pInfo) {
        //是否是系统软件或者是系统软件正在更新
        return (!isSystemApp(pInfo) && !isSystemUpdateApp(pInfo));
    }

    public static boolean clientInstallTask(final String apkPath) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                boolean b = clientInstall(apkPath);
                emitter.onNext(b);
            }
        }).subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Boolean b) {
                        result = b;
                        Log.d(TAG, "Client installed successful!");
                    }

                    @Override
                    public void onError(Throwable e) {
                        result = false;
                        Log.e(TAG, "Client installed error!");
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        return result;
    }

    public static boolean clientUninstallTask(final String pkgName) {
        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                boolean b = pmUninstall(pkgName);
                emitter.onNext(b);
            }
        }).subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Boolean b) {
                        result = b;
                        Log.d(TAG, "Client uninstalled successful!");
                    }

                    @Override
                    public void onError(Throwable e) {
                        result = false;
                        Log.e(TAG, "Client uninstalled error!");
                    }

                    @Override
                    public void onComplete() {
                    }
                });
        return result;
    }


    /**
     * install client
     */
    private static boolean clientInstall(String apkPath) {
        PrintWriter PrintWriter = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            PrintWriter = new PrintWriter(process.getOutputStream());
            PrintWriter.println("chmod 777 " + apkPath);
            PrintWriter.println("export LD_LIBRARY_PATH=/vendor/lib:/system/lib");
            PrintWriter.println("pm install -r " + apkPath);
//          PrintWriter.println("exit");
            PrintWriter.flush();
            PrintWriter.close();
            int value = process.waitFor();
            return returnResult(value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }

    /**
     * uninstall client
     */
    private static boolean clientUninstall(String packageName) {
        PrintWriter PrintWriter = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            PrintWriter = new PrintWriter(process.getOutputStream());
            PrintWriter.println("LD_LIBRARY_PATH=/vendor/lib:/system/lib ");
            PrintWriter.println("pm uninstall " + packageName);
            PrintWriter.flush();
            PrintWriter.close();
            int value = process.waitFor();
            return returnResult(value);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }



    private static boolean pmUninstall(String pkgName) {
        String[] args = { "pm", "uninstall", pkgName };
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        String str = null;
        try {
            process = processBuilder.start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((str = successResult.readLine()) != null ) {
                successMsg.append(str);
            }
            while ((str = errorResult.readLine()) != null) {
                errorMsg.append(str);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return successMsg.toString().equalsIgnoreCase("success");
    }

    private static boolean returnResult(int value) {
        // 代表成功
        if (value == 0) {
            return true;
        } else if (value == 1) { // 失败
            return false;
        } else { // 未知情况
            return false;
        }
    }
}
