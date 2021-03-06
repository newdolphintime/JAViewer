package io.github.javiewer;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Logger;
import io.github.javiewer.adapter.item.DataSource;
import io.github.javiewer.fragment.ActressesFragment;
import io.github.javiewer.fragment.HomeFragment;
import io.github.javiewer.fragment.PopularFragment;
import io.github.javiewer.fragment.ReleasedFragment;
import io.github.javiewer.fragment.favourite.FavouriteTabsFragment;
import io.github.javiewer.fragment.genre.GenreTabsFragment;
import io.github.javiewer.network.BasicService;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

/**
 * Project: JAViewer
 */

public class JAViewer extends Application {

    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 5 Build/LMY48B; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/43.0.2357.65 Mobile Safari/537.36";

    public static Configurations CONFIGURATIONS;

    public static final List<DataSource> DATA_SOURCES = new ArrayList<>();

    public static final Map<Integer, Class<? extends Fragment>> FRAGMENTS = new HashMap<Integer, Class<? extends Fragment>>() {{
        put(R.id.nav_home, HomeFragment.class);
        put(R.id.nav_popular, PopularFragment.class);
        put(R.id.nav_released, ReleasedFragment.class);
        put(R.id.nav_actresses, ActressesFragment.class);
        put(R.id.nav_genre, GenreTabsFragment.class);
        put(R.id.nav_favourite, FavouriteTabsFragment.class);
    }};

    public static DataSource getDataSource() {
        return JAViewer.CONFIGURATIONS.getDataSource();
    }

    public static BasicService SERVICE;

    public static void recreateService() {
        SERVICE = new Retrofit.Builder()
                .baseUrl(JAViewer.getDataSource().getLink())
                .client(JAViewer.HTTP_CLIENT)
                .build()
                .create(BasicService.class);
    }

    public static File getStorageDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), "JAViewer/");
        dir.mkdirs();
        return dir;
    }

    public static HttpUrl replaceUrl(HttpUrl url) {
        HttpUrl.Builder builder = url.newBuilder();
        String host = url.url().getHost();
        Log.d("HOST是啥？？",host);
        Log.d("hostReplacements藏了些什么？",hostReplacements.toString());
        if (hostReplacements.containsKey(host)) {
            builder.host(hostReplacements.get(host));
            return builder.build();
        }

        return url;
    }

    public static Map<String, String> hostReplacements = new HashMap<>();

    /*static {
        String host;
        try {
            host = new URI(DataSource.AVMO.getLink()).getHost();
            hostReplacements.put("avmo.club", host);
            hostReplacements.put("avmo.pw", host);
            hostReplacements.put("avio.pw", host);

            host = new URI(DataSource.AVSO.getLink()).getHost();
            hostReplacements.put("avso.pw", host);

            host = new URI(DataSource.AVXO.getLink()).getHost();
            hostReplacements.put("avxo.pw", host);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }*/


    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
        //应用拦截器
//        拦截器是 OkHttp 提供的对 HTTP 请求和响应进行统一处理的强大机制。拦截器在实现和使用上类似于 Servlet 规范中的过滤器。
// 多个拦截器可以链接起来，形成一个链条。拦截器会按照在链条上的顺序依次执行。 拦截器在执行时，可以先对请求的 Request 对象进行修改；
// 再得到响应的 Response 对象之后，可以进行修改之后再返回。
//        Interceptor 接口只包含一个方法 intercept，其参数是 Chain 对象。
// Chain 对象表示的是当前的拦截器链条。通过 Chain 的 request 方法可以获取到当前的 Request 对象。在使用完 Request
// 对象之后，通过 Chain 对象的 proceed 方法来继续拦截器链条的执行。当执行完成之后，可以对得到的 Response 对象进行额外的处理。
//
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request original = chain.request();//通过 Chain 的 request 方法可以获取到当前的 Request 对象
            Log.d("拦截器之前",original.url().toString());
            Request request = original.newBuilder()
                    .url(replaceUrl(original.url()))
                    .header("User-Agent", USER_AGENT)
                    .build();
            Log.d("拦截器之后",replaceUrl(original.url()).toString());
            Log.d("requst内容",request.toString());
            return chain.proceed(request);//通过 Chain 对象的 proceed 方法来继续拦截器链条的执行。
        }
    })
    //OkHttp框架从3.0开始简化了Cookie的使用，它提供了一个叫做cookieJar的API，只需要我们实现该API中的方法即可，一个简单的使用方式如下：
            .cookieJar(new CookieJar() {
                //通过以上几个关键方法，可以很明显的感觉到作者的意图了，为了更加自由定制化的cookie管理。其中loadForRequest()、saveFromResponse()这两个方法最为关键，
                // 分别是在发送时向request header中加入cookie，在接收时，读取response header中的cookie。现在再去看Cookiejar这个类，就很好理解了
                //so!在OkHttpClient创建时，传入这个CookieJar的实现，就能完成对Cookie的自动管理了
                private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();
                //一个是保存Cookie
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url, cookies);
                }
                //一个是读取Cookie
                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url);
                    return cookies != null ? cookies : new ArrayList<Cookie>();
                }
            })
            .build();

    public static <T> T parseJson(Class<T> beanClass, JsonReader reader) throws JsonParseException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(reader, beanClass);
    }

    public static <T> T parseJson(Class<T> beanClass, String json) throws JsonParseException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(json, beanClass);
    }

    public static boolean Objects_equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static void a(Context context) {
        String url = "https://qr.alipay.com/a6x05027ymf6n8kl0qkoa54";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CustomActivityOnCrash.install(this);
        Fabric.with(this, new Crashlytics());
    }
}
