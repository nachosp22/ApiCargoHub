package com.cargohub.mobile.network;

import androidx.annotation.NonNull;

import com.cargohub.mobile.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile ApiService instance;

    private ApiClient() {
    }

    @NonNull
    public static ApiService getInstance() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
                    logger.setLevel(BuildConfig.DEBUG
                            ? HttpLoggingInterceptor.Level.BODY
                            : HttpLoggingInterceptor.Level.BASIC);

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .addInterceptor(new AuthInterceptor())
                            .addInterceptor(logger)
                            .build();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.API_BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(okHttpClient)
                            .build();

                    instance = retrofit.create(ApiService.class);
                }
            }
        }
        return instance;
    }
}
