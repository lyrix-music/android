package `in`.srev.lyrix

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BackendService {

    @POST("/login")
    suspend fun backendLogin(@Body requestBody: RequestBody): Response<ResponseBody>


    @POST("/register")
    suspend fun backendRegister(@Body requestBody: RequestBody): Response<ResponseBody>


    @POST("/user/player/local/current_song")
    suspend fun setCurrentPlayingSong(@Body requestBody: RequestBody, @Header("Authorization") token: String): Response<ResponseBody>

    @GET("/user/player/local/current_song/lyrics")
    suspend fun getLyrics(@Header("Authorization") token: String): Response<ResponseBody>

}