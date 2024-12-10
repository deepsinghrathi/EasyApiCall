package com.deep.easyapicall;

import android.os.Bundle;

import com.deep.apicall.Api;
import com.deep.apicall.BaseActivity;
import com.deep.apicall.Environment;
import com.deep.apicall.ImagePojo;
import com.deep.apicall.RequestMethod;
import com.deep.apicall.Response;

import org.json.JSONObject;

import java.util.List;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Api.with("MainActivity", "https://rapidrasoi.in/")
//                .setEnvironment(Environment.DEBUG)
//                .setRequestMethod(RequestMethod.POST)
//                .setSubFolder("api")
//                .setPerm("city_id", "10")
//                .setPerm("name", "")
//                .execute("get_college.php", new Response() {
//                    @Override
//                    public void onSuccess(JSONObject jsonObject) {
//                        super.onSuccess(jsonObject);
//                    }
//
//                    @Override
//                    public void onFailed(int code, String exception, Environment environment) {
//                        super.onFailed(code, exception, environment);
//                    }
//
//                });

//        setCrop(true);
//        setCapture(true);
        checkCaptureImagePermission(findViewById(R.id.img));
    }

    @Override
    public void onImageSelected(List<ImagePojo> profileImageList) {
        super.onImageSelected(profileImageList);
        updateProfilePic("profilePicUrl3",profileImageList.get(0));
    }

    private void updateProfilePic(String key, ImagePojo imagePojo) {
        Api.with(this, "https://heartangle.com/")
                .setRequestMethod(RequestMethod.POST)
                .canShowProgress(false)
                .setEnvironment(Environment.DEBUG)
                .setSubFolder("dating")
                .setSubFolder("Dating_api")
                .setPerm("profile_key", key)
                .setPerm("mobile", "9319963172")
                .setPerm("profile_src", imagePojo.getImageNameWithExtensions(), imagePojo.getImageUrl())
                .execute("update_profile_pic_2", new Response() {
                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        super.onSuccess(jsonObject);
                        try {
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailed(int code, String exception, Environment evironment) {
                        super.onFailed(code, exception, evironment);
                    }
                });
    }

}