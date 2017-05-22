package rocks.keyless.app.android.bleclient;

import android.content.Context;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by Surajit Sarkar on 22/5/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class EncryptionUtils {

    private static String tranformationRSA = "RSA/ECB/PKCS1Padding";
    //private static String tranformationRSA = "RSA/ECB/NoPadding";

    public static String encryptStringRSAPublic(String text, String strKey) {
        byte[] cipherText = null;
        String strEncryInfoData="";
        try {
            Key key = generateRSAPublicKey(strKey);

            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(tranformationRSA,"BC");
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(text.getBytes("UTF-8"));
            strEncryInfoData = new String(Base64.encode(cipherText, Base64.DEFAULT));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return strEncryInfoData.replaceAll("(\\r|\\n)", "");
        //return strEncryInfoData;
    }
    public static String decryptStringRSAPrivate(String text, String strKey){
        String strEncryInfoData="";
        byte[] cipherText = null;
        try{
            Key key = generateRSAPrivateKey(strKey);
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(tranformationRSA,"BC");
            // encrypt the plain text using the public key
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] bytes = Base64.decode(text, Base64.DEFAULT);
            cipherText = cipher.doFinal(bytes);
            strEncryInfoData = new String(cipherText);
        }catch (Exception e){
            e.printStackTrace();
        }
        return strEncryInfoData;
    }

    private static Key generateRSAPublicKey(String strKey) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        KeyFactory keyFac = KeyFactory.getInstance("RSA");
        KeySpec keySpec = new X509EncodedKeySpec(Base64.decode(strKey.trim().getBytes("UTF-8"), Base64.DEFAULT));
        Key publicKey = keyFac.generatePublic(keySpec);
        return publicKey;
    }

    private static Key generateRSAPrivateKey(String strKey) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException, NoSuchProviderException {
        KeyFactory keyFac = KeyFactory.getInstance("RSA","BC");
        //KeySpec keySpec = new X509EncodedKeySpec(Base64.decode(strKey.trim().getBytes("UTF-8"), Base64.DEFAULT));
        KeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(strKey.trim().getBytes("UTF-8"), Base64.DEFAULT));
        Key publicKey = keyFac.generatePrivate(keySpec);
        return publicKey;
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static String[] excludedList = {"BEGIN PUBLIC KEY","END PUBLIC KEY",
            "BEGIN PRIVATE KEY","END PRIVATE KEY",
            "BEGIN RSA PRIVATE KEY","END RSA PRIVATE KEY"};

    public static boolean isExcluded(String str){
        boolean bVal = false;
        for(String s : excludedList){
            if(str.contains(s)){
                bVal = true;
                break;
            }
        }
        return bVal;
    }

    public static String readKey(Context context,int resId){
        StringBuilder result = new StringBuilder();
        try {
            InputStream inputStream = context.getResources().openRawResource(resId);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        //result.append(line);
                        if (!isExcluded(line.trim()) && !isNullOrEmpty(line.trim())) {
                            result.append(line);
                        }
                    }
                    return result.toString();
                } finally {
                    reader.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
