package com.example.navigationneshan

import com.example.navigationneshan.data.api.ApiInterface
import com.example.navigationneshan.data.repository.ApiRepo
import com.example.navigationneshan.di.AppModule
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.neshan.common.model.LatLng
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class SearchTest {

    private val server: MockWebServer = MockWebServer()
    private val MOCK_WEBSERVER_PORT = 8000
    lateinit var apiInterface: ApiInterface
    lateinit var apiRepo: ApiRepo

    private val BASE_URL = "https://api.neshan.org/"
    private val APIKEY = "API_KEY"

    @Before
    fun init() {
        server.start(MOCK_WEBSERVER_PORT)
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val builder: Request.Builder = chain.request().newBuilder()
                builder.header("Content-Type", "application/json")
                builder.header("Api-Key", APIKEY)
                chain.proceed(builder.build())
            }).addInterceptor(logging)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
        apiInterface = Retrofit.Builder()
            .baseUrl(server.url(BASE_URL))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(ApiInterface::class.java)
        apiRepo = ApiRepo(apiInterface)
    }

    @After
    fun shutdown() {
        server.shutdown()
    }

    @Test
    fun `JsonPlaceholder APIs parse correctly`() {
        server.apply {
            enqueue(MockResponse().setBody(MockResponseFileReader("jsonplaceholder_success.json").content))
        }
        apiRepo.getSearchQuery("آزادی", LatLng(35.6999053, 51.3355413))
            .test()
            .awaitDone(3, TimeUnit.SECONDS)
            .assertComplete()
            .assertValueCount(1)
            .assertNoErrors()
    }

}