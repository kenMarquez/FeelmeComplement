package ken.mx.feelmecomplement.cloudinary;

/**
 * Created by Ken on 13/09/16.
 */
public interface CloudinaryCallback {

    public void onCloudinaryUploadSucces(String link);

    public void onCloudinaryUploadError();
}
