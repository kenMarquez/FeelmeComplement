package ken.mx.feelmecomplement.cloudinary;

import android.os.AsyncTask;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ken on 13/09/16.
 */
public class CloudinaryInstance {

    private static Cloudinary cloudinary;


    public static Cloudinary getInstance() {
        if (cloudinary == null) {
            Map config = new HashMap();
            config.put("cloud_name", "mariachi-io");
            config.put("api_key", "583431221132112");
            config.put("api_secret", "fGE6M8Wm1O3V3I-jTfeOnF2s-5I");
            cloudinary = new Cloudinary(config);
            return cloudinary;
        }

        return cloudinary;
    }


    public static void uploadImage(final ByteArrayInputStream inputStream, final CloudinaryCallback callback) {

        if (cloudinary == null) getInstance();

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String link = null;
                try {
                    Map upload = cloudinary.uploader().upload(inputStream, ObjectUtils.emptyMap());

                    link = (String) upload.get("url");
                } catch (IOException e) {

                    e.printStackTrace();
                }
                return link;
            }

            @Override
            protected void onPostExecute(String link) {
                if (link != null) {
                    callback.onCloudinaryUploadSucces(link);
                } else {
                    callback.onCloudinaryUploadError();
                }
            }
        }.execute();

    }

}
